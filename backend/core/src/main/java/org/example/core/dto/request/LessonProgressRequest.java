package org.example.core.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LessonProgressRequest(
        @NotNull Long lessonId,
        @Min(0) @Max(100) Integer score
) {}