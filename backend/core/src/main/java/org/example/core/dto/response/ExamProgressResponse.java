package org.example.core.dto.response;

public record ExamProgressResponse(
        Long examId,
        String examTitle,
        Integer score,
        String status
) {
}
