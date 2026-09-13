package org.example.core.dto.request;

public record AiGenerateExamRequest(
        Long lessonId,
        Integer count
) {
}