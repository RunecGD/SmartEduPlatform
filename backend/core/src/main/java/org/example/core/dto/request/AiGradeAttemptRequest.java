package org.example.core.dto.request;

import java.util.List;

public record AiGradeAttemptRequest(
        List<AiGradeQuestion> questions,
        List<ExamAnswerRequest> answers
) {
}