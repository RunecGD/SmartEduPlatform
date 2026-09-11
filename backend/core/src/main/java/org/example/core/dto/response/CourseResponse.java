package org.example.core.dto.response;

import org.example.core.dto.enums.CourseStatus;

import java.time.Instant;

public record CourseResponse(
        Long id,
        Long teacherId,
        String title,
        String category,
        String description,
        CourseStatus status,
        Instant createdAt,
        Instant updatedAt
) {}