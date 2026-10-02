package org.example.core.config;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.*;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final JdbcTemplate jdbcTemplate;
    private final JacksonJsonMessageConverter jacksonMessageConverter;
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishExamPassed(ExamPassedEvent event) { enqueue("exam.passed",event); }
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishUserEnrolled(UserEnrolledEvent event) { enqueue("user.enrolled",event); }
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishForumAnswered(ForumAnsweredEvent event) { enqueue("forum.answered",event); }
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishForumReply(ForumReplyEvent event) { enqueue("forum.reply",event); }
    private void enqueue(String routingKey,Object event) {
        byte[] body=jacksonMessageConverter.toMessage(event,new MessageProperties()).getBody();
        jdbcTemplate.update("INSERT INTO smartedu_jobs(id,kind,routing_key,payload) VALUES (?,'EVENT',?,?)",
                UUID.randomUUID(),routingKey,new String(body,StandardCharsets.UTF_8));
    }
}
