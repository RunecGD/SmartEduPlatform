package org.example.core.dto.response;

import java.time.Instant;

public record CourseModuleResponse(
        Long id,
        Long courseId,
        String title,
        Integer orderIndex,
        Instant createdAt
) {}