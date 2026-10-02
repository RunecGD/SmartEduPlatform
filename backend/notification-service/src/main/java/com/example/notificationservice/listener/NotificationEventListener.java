package com.example.notificationservice.listener;

import com.example.notificationservice.dto.ExamPassedEvent;
import com.example.notificationservice.dto.ForumAnsweredEvent;
import com.example.notificationservice.dto.ForumReplyEvent;
import com.example.notificationservice.dto.UserEnrolledEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.exam-passed")
    public void handleExamPassed(ExamPassedEvent event) {


        notificationService.createExamPassedNotification(event);
    }

    @RabbitListener(queues = "notification.user-enrolled")
    public void handleUserEnrolled(UserEnrolledEvent event) {


        notificationService.createUserEnrolledNotification(event);
    }
    @RabbitListener(queues = "notification.forum-reply")
    public void handleForumReply(ForumReplyEvent event) {


        notificationService.createForumReplyNotification(event);
    }

    @RabbitListener(queues="notification.forum-answered")
    public void handleForumAnswered(ForumAnsweredEvent event) {
        notificationService.createForumAnsweredNotification(event);
    }
}
