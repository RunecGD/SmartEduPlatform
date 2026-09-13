package org.example.core.dto.request;

public record ExamRequest(
        String title,
        Integer timeLimitMinutes
) {
}