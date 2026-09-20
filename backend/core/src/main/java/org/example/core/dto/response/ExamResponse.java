package org.example.core.dto.response;

public record ExamResponse(
        Long id,
        Long lessonId,
        String title,
        Integer timeLimitMinutes
) {
}