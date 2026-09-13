package org.example.core.dto.response;

public record AiGradeResponse(
        Long questionId,
        Integer score,
        String feedback
) {
}