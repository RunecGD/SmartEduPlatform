package org.example.core.dto.response;

import org.example.core.dto.enums.LessonType;

import java.time.Instant;

public record LessonResponse(
        Long id,
        Long moduleId,
        LessonType type,
        String title,
        String content,
        Integer orderIndex,
        Instant createdAt
) {}