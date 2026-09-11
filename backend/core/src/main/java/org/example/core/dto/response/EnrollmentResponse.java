package org.example.core.dto.response;

import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        Long userId,
        Long courseId,
        Integer progressPct,
        Instant enrolledAt
) {}