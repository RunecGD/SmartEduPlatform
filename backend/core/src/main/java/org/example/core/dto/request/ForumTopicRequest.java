package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumTopicRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {
}
