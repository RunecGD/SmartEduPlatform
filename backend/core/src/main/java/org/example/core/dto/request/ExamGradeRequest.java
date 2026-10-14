package org.example.core.dto.request;

public record ExamGradeRequest(Long questionId, Integer score, String feedback) {}

