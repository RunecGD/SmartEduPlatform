package org.example.core.dto.request;

public record ExamSubmittedEvent(Long userId, Long attemptId, String studentName,
                                 String examTitle, String courseTitle) {}
