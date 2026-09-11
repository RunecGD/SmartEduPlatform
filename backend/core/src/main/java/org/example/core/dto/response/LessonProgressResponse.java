package org.example.core.dto.response;

import java.time.Instant;

public record LessonProgressResponse(
        Long id,
        Long enrollmentId,
        Long lessonId,
        Instant completedAt,
        Integer score
) {}