package com.example.notificationservice.dto;
import com.example.notificationservice.model.Notification;
import java.time.LocalDateTime;
public record NotificationResponse(String id,Long userId,NotificationType type,String message,Boolean read,LocalDateTime createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(),n.getUserId(),n.getType(),n.getMessage(),n.getRead(),n.getCreatedAt());
    }
}
