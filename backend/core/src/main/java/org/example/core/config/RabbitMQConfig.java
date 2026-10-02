package org.example.core.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.*;
@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE="smartedu.events";
    public static final String EXAM_PASSED_QUEUE="notification.exam-passed";
    public static final String EXAM_PASSED_ROUTING_KEY="exam.passed";
    public static final String USER_ENROLLED_QUEUE="notification.user-enrolled";
    public static final String USER_ENROLLED_ROUTING_KEY="user.enrolled";
    public static final String FORUM_ANSWERED_QUEUE="notification.forum-answered";
    public static final String FORUM_ANSWERED_ROUTING_KEY="forum.answered";
    @Bean public TopicExchange smartEduExchange() { return new TopicExchange(EXCHANGE,true,false); }
    @Bean public Queue examPassedQueue() { return new Queue("notification.exam-passed",true); }
    @Bean public Binding examPassedBinding(@org.springframework.beans.factory.annotation.Qualifier("examPassedQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(smartEduExchange()).with("exam.passed");
    }
    @Bean public Queue userEnrolledQueue() { return new Queue("notification.user-enrolled",true); }
    @Bean public Binding userEnrolledBinding(@org.springframework.beans.factory.annotation.Qualifier("userEnrolledQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(smartEduExchange()).with("user.enrolled");
    }
    @Bean public Queue forumAnsweredQueue() { return new Queue("notification.forum-answered",true); }
    @Bean public Binding forumAnsweredBinding(@org.springframework.beans.factory.annotation.Qualifier("forumAnsweredQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(smartEduExchange()).with("forum.answered");
    }
    @Bean public Queue forumReplyQueue() { return new Queue("notification.forum-reply",true); }
    @Bean public Binding forumReplyBinding(@org.springframework.beans.factory.annotation.Qualifier("forumReplyQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(smartEduExchange()).with("forum.reply");
    }
    @Bean public JacksonJsonMessageConverter jacksonMessageConverter() { return new JacksonJsonMessageConverter(); }
}
