package org.example.core.dto.request;

public record ExamAnswerRequest(
        Long questionId,
        String answerText
) {
}