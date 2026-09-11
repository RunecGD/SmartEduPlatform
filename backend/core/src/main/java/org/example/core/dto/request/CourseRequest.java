package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.core.dto.enums.CourseStatus;

public record CourseRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 100) String category,
        String description
) {}