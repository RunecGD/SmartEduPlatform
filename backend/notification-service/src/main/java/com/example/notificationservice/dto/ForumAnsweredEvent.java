package com.example.notificationservice.dto;
public record ForumAnsweredEvent(Long userId, Long questionId, Long answerId) {}
