package org.example.core.dto.response;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AiGradeResponse(
        @JsonAlias("question_id") Long questionId,
        Integer score,
        String feedback
) {
}
