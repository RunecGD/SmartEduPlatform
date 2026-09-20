package org.example.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiGenerateExamRequest(
        @JsonProperty("lesson_id")
        Long lessonId,

        @JsonProperty("count")
        Integer count
) {
}