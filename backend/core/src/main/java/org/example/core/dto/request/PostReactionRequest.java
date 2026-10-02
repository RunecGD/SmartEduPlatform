package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PostReactionRequest(
        @NotBlank
        @jakarta.validation.constraints.Size(max=30) String reaction
) {
}
