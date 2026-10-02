package com.example.notificationservice.dto;
public record ExamPassedEvent(Long userId, Long examId, Long attemptId, Integer score) {}
