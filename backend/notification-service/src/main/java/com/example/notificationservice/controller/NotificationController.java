package com.example.notificationservice.controller;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final com.example.notificationservice.security.NotificationIdentityResolver identityResolver;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @PathVariable Long userId,
            @RequestHeader(value="Authorization",required=false) String authorization
    ) {
        identityResolver.requireOwner(userId,authorization);
        return ResponseEntity.ok(
                notificationService.getUserNotifications(userId)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId,
            @RequestParam Long userId,
            @RequestHeader(value="Authorization",required=false) String authorization
    ) {
        identityResolver.requireOwner(userId,authorization);
        return ResponseEntity.ok(
                notificationService.markAsRead(
                        notificationId,
                        userId
                )
        );
    }
}
