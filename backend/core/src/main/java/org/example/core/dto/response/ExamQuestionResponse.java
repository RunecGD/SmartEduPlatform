package org.example.core.dto.response;

public record ExamQuestionResponse(
        Long id,
        String question,
        Integer maxScore,
        Integer orderIndex
) {
}
