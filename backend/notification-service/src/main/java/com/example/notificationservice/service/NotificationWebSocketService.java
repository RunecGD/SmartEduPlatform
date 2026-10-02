package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(
            NotificationResponse notification
    ) {
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.userId(),
                notification
        );
    }
}
