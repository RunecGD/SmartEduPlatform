package org.example.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @NotBlank String question,
        @JsonProperty("lesson_id") Long lessonId
) {}