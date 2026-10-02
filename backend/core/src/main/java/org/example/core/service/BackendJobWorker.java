package org.example.core.service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.core.config.AiServiceClient;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackendJobWorker {
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;
    private final AiServiceClient aiServiceClient;
    private record Job(UUID id,String kind,String routingKey,String payload,int attempts) {}

    @Scheduled(fixedDelayString="${jobs.poll-ms:1000}")
    public void processEvents() { processKind("EVENT"); }
    @Scheduled(fixedDelayString="${jobs.poll-ms:1000}")
    public void processIndexes() { processKind("INDEX"); }
    private void processKind(String kind) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var jobs=jdbcTemplate.query("SELECT id,kind,routing_key,payload,attempts FROM smartedu_jobs "
                        + "WHERE kind=? AND next_at<=NOW() ORDER BY next_at LIMIT 1 FOR UPDATE SKIP LOCKED",
                        (rs,n)->new Job(rs.getObject("id",UUID.class),rs.getString("kind"),
                                rs.getString("routing_key"),rs.getString("payload"),rs.getInt("attempts")),kind);
                if (jobs.isEmpty()) return;
                Job job=jobs.get(0);
                try {
                    if ("INDEX".equals(job.kind())) aiServiceClient.index(job.payload());
                    else send(job);
                    jdbcTemplate.update("DELETE FROM smartedu_jobs WHERE id=?",job.id());
                } catch (Exception ex) {
                    if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
                    int seconds=(int)Math.min(3600L,1L<<Math.min(job.attempts()+1,12));
                    jdbcTemplate.update("UPDATE smartedu_jobs SET attempts=attempts+1,next_at=NOW()+(? * INTERVAL '1 second') WHERE id=?",
                            seconds,job.id());
                    log.warn("Backend job {} failed; retry in {}s ({})",job.id(),seconds,ex.getClass().getSimpleName());
                }
            });
        } catch (Exception ex) { log.error("Backend jobs unavailable ({})",ex.getClass().getSimpleName()); }
    }
    private void send(Job job) throws Exception {
        var props=new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding("UTF-8");
        props.setMessageId(job.id().toString());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        var correlation=new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.send("smartedu.events",job.routingKey(),
                new Message(job.payload().getBytes(StandardCharsets.UTF_8),props),correlation);
        var confirm=correlation.getFuture().get(10,TimeUnit.SECONDS);
        if (!confirm.isAck() || correlation.getReturned()!=null) {
            throw new IllegalStateException("RabbitMQ did not route/confirm event");
        }
    }
}
