package org.example.core.service;
import io.minio.*;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.core.dto.response.MaterialResponse;
import org.example.core.model.Material;
import org.example.core.repository.MaterialRepository;
import org.example.core.repository.LessonRepository;
import org.springframework.beans.factory.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MaterialService {
    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;
    private final MaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final AccessGuard accessGuard;
    private final JdbcTemplate jdbcTemplate;
    @Value("${minio.bucket}") private String bucket;
    public MaterialService(MinioClient minioClient,@Qualifier("publicMinioClient") MinioClient publicMinioClient,
            MaterialRepository materialRepository,LessonRepository lessonRepository,AccessGuard accessGuard,JdbcTemplate jdbcTemplate) {
        this.minioClient=minioClient; this.publicMinioClient=publicMinioClient;
        this.materialRepository=materialRepository; this.lessonRepository=lessonRepository;
        this.accessGuard=accessGuard; this.jdbcTemplate=jdbcTemplate;
    }
    @Transactional(rollbackFor=Exception.class)
    public MaterialResponse upload(Long lessonId,MultipartFile file) throws Exception {
        var lesson=lessonRepository.findById(lessonId).orElseThrow(()->new EntityNotFoundException("Урок не найден"));
        accessGuard.owner(lesson.getModule().getCourse());
        String name=file.getOriginalFilename();
        if (name==null || name.isBlank() || file.isEmpty() || file.getSize()>25L*1024*1024) {
            throw new IllegalArgumentException("Допустим непустой файл до 25 МБ");
        }
        name=name.replace('\\','/'); name=name.substring(name.lastIndexOf('/')+1);
        if (name.length()>240) throw new IllegalArgumentException("Имя файла слишком длинное");
        if (!name.toLowerCase(java.util.Locale.ROOT).matches(".+\\.(pdf|docx|txt)")) {
            throw new IllegalArgumentException("Допустимы PDF, DOCX и TXT в UTF-8");
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            try { minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build()); }
            catch (io.minio.errors.ErrorResponseException ex) {
                if (!"BucketAlreadyOwnedByYou".equals(ex.errorResponse().code())) throw ex;
            }
        }
        String key="materials/"+lessonId+"/"+UUID.randomUUID()+"_"+name;
        String type=file.getContentType()==null ? "application/octet-stream" : file.getContentType();
        if (type.length()>255) throw new IllegalArgumentException("Content-Type слишком длинный");
        try (var stream=file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(stream,file.getSize(),-1).contentType(type).build());
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status==STATUS_ROLLED_BACK) {
                    try { minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); }
                    catch (Exception ex) { log.error("Cannot remove orphaned upload ({})",ex.getClass().getSimpleName()); }
                }
            }
        });
        var saved=materialRepository.save(Material.builder().lesson(lesson).fileName(name).contentType(type)
                .objectKey(key).sizeBytes(file.getSize()).build());
        // Задание появится для AI только после COMMIT этой транзакции.
        jdbcTemplate.update("INSERT INTO smartedu_jobs(id,kind,routing_key,payload) VALUES (?,'INDEX','material',?)",UUID.randomUUID(),key);
        return new MaterialResponse(saved.getId(),lessonId,saved.getFileName(),saved.getContentType(),saved.getSizeBytes(),saved.getUploadedAt());
    }
    @Transactional(readOnly=true)
    public String downloadUrl(Long materialId) throws Exception {
        var material=materialRepository.findById(materialId).orElseThrow(()->new EntityNotFoundException("Материал не найден"));
        accessGuard.courseAccess(material.getLesson().getModule().getCourse());
        return publicMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                .bucket(bucket).object(material.getObjectKey()).expiry(10,TimeUnit.MINUTES).build());
    }
}
