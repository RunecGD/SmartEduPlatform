package org.example.core.dto.response;

import java.time.Instant;

public record ExamWebSocketResponse(
        Long attemptId,
        String status,
        long remainingSeconds,
        Instant startedAt,
        Instant deadline
) {
}