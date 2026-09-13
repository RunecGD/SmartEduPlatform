package org.example.core.dto.response;

import java.time.Instant;
import java.util.List;

public record ExamAttemptResponse(
        Long attemptId,
        Long examId,
        Instant startedAt,
        Instant finishedAt,
        Integer totalScore,
        String status,
        List<ExamAnswerResponse> answers
) {
}