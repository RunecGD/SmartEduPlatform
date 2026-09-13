package org.example.core.dto.response;

public record ExamAnswerResponse(
        Long questionId,
        String question,
        String answer,
        Integer score,
        String feedback,
        Integer maxScore
) {
}