package org.example.core.dto.response;

public record AiQuestionResponse(
        String question,
        Integer maxScore
) {
}