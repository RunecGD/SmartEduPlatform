package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ForumPostRequest(
        @NotBlank
        String content
) {
}
