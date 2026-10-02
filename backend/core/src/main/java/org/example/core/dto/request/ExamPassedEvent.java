package org.example.core.dto.request;

public record ExamPassedEvent(
        Long userId,
        Long examId,
        Long attemptId,
        Integer score
) {
}
