package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.core.dto.enums.LessonType;

public record LessonRequest(
        @NotNull LessonType type,
        @NotBlank @Size(max = 255) String title,
        String content,
        @NotNull Integer orderIndex
) {}