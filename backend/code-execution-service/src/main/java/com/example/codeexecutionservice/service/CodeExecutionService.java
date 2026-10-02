package com.example.codeexecutionservice.service;

import com.example.codeexecutionservice.dto.*;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.AccessMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeExecutionService {
    private final DockerClient dockerClient;
    @Value("${code-execution.timeout-seconds:5}") private long timeoutSeconds;
    @Value("${code-execution.compile-timeout-seconds:30}") private long compileTimeoutSeconds;
    @Value("${code-execution.memory-limit-mb:256}") private long memoryLimitMb;
    @Value("${code-execution.cpu-limit:500000000}") private long cpuLimit;
    @Value("${code-execution.pids-limit:64}") private long pidsLimit;
    @Value("${code-execution.max-output-bytes:65536}") private int maxOutputBytes;
    @Value("${code-execution.max-concurrent:2}") private int maxConcurrent;
    @Value("${code-execution.work-dir:${java.io.tmpdir}/smartedu-code-execution}") private String workDirectory;
    @Value("${code-execution.host-work-dir:}") private String hostWorkDirectory;
    private Semaphore slots;
    @jakarta.annotation.PostConstruct
    public void validateConfiguration() throws IOException {
        if (timeoutSeconds<1 || timeoutSeconds>60 || compileTimeoutSeconds<1 || compileTimeoutSeconds>120
                || memoryLimitMb<64 || memoryLimitMb>2048 || pidsLimit<1 || pidsLimit>256
                || cpuLimit<1 || cpuLimit>4_000_000_000L || maxOutputBytes<1024 || maxOutputBytes>1024*1024
                || maxConcurrent<1 || maxConcurrent>16) throw new IllegalStateException("Недопустимые лимиты исполнения");
        Files.createDirectories(Path.of(workDirectory));
        slots=new Semaphore(maxConcurrent);
    }
    public CodeExecutionResponse execute(CodeExecutionRequest request) {
        Language language=language(request.language());
        if (!slots.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Сервис занят, повторите позже");
        long started=System.nanoTime();
        Path directory=null;
        try {
            // Images are pulled by the operator before accepting requests.
            dockerClient.inspectImageCmd(language.image()).exec();
            directory=Files.createTempDirectory(Path.of(workDirectory),"job-");
            try { Files.setPosixFilePermissions(directory,PosixFilePermissions.fromString("rwxrwxrwx")); }
            catch (UnsupportedOperationException ignored) { }
            write(directory.resolve(language.file()),request.code());
            write(directory.resolve("input.txt"),request.stdin()==null ? "" : request.stdin());
            if (language.compile()!=null) {
                Stage compiled=run(language.image(),language.compile(),directory,false,compileTimeoutSeconds);
                if (compiled.timedOut()) return result(ExecutionStatus.TIME_LIMIT_EXCEEDED,compiled,started);
                if (compiled.exitCode()!=0) return result(ExecutionStatus.COMPILATION_ERROR,compiled,started);
            }
            Stage execution=run(language.image(),language.run(),directory,true,timeoutSeconds);
            return result(execution.timedOut() ? ExecutionStatus.TIME_LIMIT_EXCEEDED
                    : execution.exitCode()==0 ? ExecutionStatus.SUCCESS : ExecutionStatus.RUNTIME_ERROR,execution,started);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return internal(started);
        } catch (Exception ex) {
            log.warn("Code execution failed",ex);
            return internal(started);
        } finally {
            deleteDirectory(directory);
            slots.release();
        }
    }
    private void write(Path file,String content) throws IOException {
        Files.writeString(file,content,StandardCharsets.UTF_8);
        try { Files.setPosixFilePermissions(file,PosixFilePermissions.fromString("rw-r--r--")); }
        catch (UnsupportedOperationException ignored) { }
    }
    private Stage run(String image,String[] command,Path directory,boolean readOnly,long seconds) throws Exception {
        String hostPath=hostWorkDirectory.isBlank() ? directory.toAbsolutePath().toString()
                : Path.of(hostWorkDirectory).resolve(directory.getFileName()).toString();
        HostConfig host=HostConfig.newHostConfig().withMemory(memoryLimitMb*1024*1024)
                .withMemorySwap(memoryLimitMb*1024*1024).withNanoCPUs(cpuLimit).withPidsLimit(pidsLimit)
                .withNetworkMode("none").withReadonlyRootfs(true).withCapDrop(Capability.ALL)
                .withSecurityOpts(List.of("no-new-privileges:true"))
                .withBinds(new Bind(hostPath,new Volume("/workspace"),readOnly ? AccessMode.ro : AccessMode.rw))
                .withTmpFs(Map.of("/tmp","rw,noexec,nosuid,nodev,size=64m"))
                .withLogConfig(new LogConfig(LogConfig.LoggingType.JSON_FILE,Map.of("max-size","1m","max-file","1")));
        String id=dockerClient.createContainerCmd(image).withCmd(command).withHostConfig(host)
                .withWorkingDir("/workspace").withUser("65534:65534")
                .withAttachStdout(true).withAttachStderr(true).withTty(false)
                .withLabels(Map.of("smartedu.execution","true")).exec().getId();
        try {
            dockerClient.startContainerCmd(id).exec();
            boolean finished;
            Integer exit;
            try (var wait=new WaitContainerResultCallback()) {
                dockerClient.waitContainerCmd(id).exec(wait);
                finished=wait.awaitCompletion(seconds,TimeUnit.SECONDS);
                if (finished) exit=wait.awaitStatusCode();
                else {
                    dockerClient.killContainerCmd(id).exec();
                    exit=null;
                }
            }
            String stdout=logs(id,false),stderr=logs(id,true);
            return new Stage(exit,stdout,stderr,!finished);
        } finally {
            try { dockerClient.removeContainerCmd(id).withForce(true).withRemoveVolumes(true).exec(); }
            catch (Exception ex) { log.error("Cannot remove execution container {}",id); }
        }
    }
    private String logs(String id,boolean stderr) throws Exception {
        try (var collector=new LogCollector(maxOutputBytes)) {
            dockerClient.logContainerCmd(id).withStdOut(!stderr).withStdErr(stderr)
                    .withFollowStream(false).withTimestamps(false).exec(collector);
            if (!collector.awaitCompletion(5,TimeUnit.SECONDS)) throw new TimeoutException("Log read timed out");
            return collector.output();
        }
    }
    private CodeExecutionResponse result(ExecutionStatus status,Stage stage,long start) {
        return new CodeExecutionResponse(status,stage.stdout(),stage.stderr(),stage.exitCode(),elapsed(start));
    }
    private CodeExecutionResponse internal(long start) {
        return new CodeExecutionResponse(ExecutionStatus.INTERNAL_ERROR,"","Не удалось выполнить программу",null,elapsed(start));
    }
    private long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start); }
    private void deleteDirectory(Path directory) {
        if (directory==null) return;
        try (var paths=Files.walk(directory)) {
            for (Path path:paths.sorted(Comparator.reverseOrder()).toList()) {
                try { Files.deleteIfExists(path); }
                catch (IOException ex) { log.warn("Cannot delete execution file"); }
            }
        } catch (IOException ex) { log.warn("Cannot clean execution directory"); }
    }
    private Language language(String name) {
        if (name==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Не указан язык");
        return switch(name.trim().toLowerCase(Locale.ROOT)) {
            case "python" -> new Language("python:3.13-alpine","main.py",null,
                    new String[]{"sh","-c","exec python3 -B /workspace/main.py < /workspace/input.txt"});
            case "java" -> new Language("eclipse-temurin:25-jdk-alpine","Main.java",
                    new String[]{"javac","-J-Xmx128m","-d","/workspace","/workspace/Main.java"},
                    new String[]{"sh","-c","exec java -Xmx128m -XX:ActiveProcessorCount=1 -cp /workspace Main < /workspace/input.txt"});
            case "cpp","c++" -> new Language("gcc:15","main.cpp",
                    new String[]{"g++","/workspace/main.cpp","-O2","-o","/workspace/program"},
                    new String[]{"sh","-c","exec /workspace/program < /workspace/input.txt"});
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Неподдерживаемый язык");
        };
    }
    private record Language(String image,String file,String[] compile,String[] run) {}
    private record Stage(Integer exitCode,String stdout,String stderr,boolean timedOut) {}
    private static class LogCollector extends ResultCallback.Adapter<Frame> {
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private final int limit;
        LogCollector(int limit) { this.limit=limit; }
        @Override public synchronized void onNext(Frame frame) {
            int length=Math.min(frame.getPayload().length,Math.max(0,limit-bytes.size()));
            bytes.write(frame.getPayload(),0,length);
        }
        synchronized String output() { return bytes.toString(StandardCharsets.UTF_8); }
    }
}
