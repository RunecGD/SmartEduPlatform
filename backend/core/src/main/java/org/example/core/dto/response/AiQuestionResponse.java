package org.example.core.dto.response;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AiQuestionResponse(
        String question,
        @JsonAlias("max_score") Integer maxScore
) {
}
