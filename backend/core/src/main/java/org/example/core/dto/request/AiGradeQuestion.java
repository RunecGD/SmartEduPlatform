package org.example.core.dto.request;

import java.util.List;

public record AiGradeQuestion(
        Long id,
        String text,
        Integer maxScore,
        List<String> chunks
) {
}