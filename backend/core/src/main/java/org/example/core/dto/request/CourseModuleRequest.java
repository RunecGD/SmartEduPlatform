package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseModuleRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull Integer orderIndex
) {}