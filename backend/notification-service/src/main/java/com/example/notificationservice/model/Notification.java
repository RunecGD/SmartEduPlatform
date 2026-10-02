package com.example.notificationservice.model;

import com.example.notificationservice.dto.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    private Long userId;

    private NotificationType type;

    private String message;

    @Builder.Default
    private Boolean read = false;

    private LocalDateTime createdAt;
}
