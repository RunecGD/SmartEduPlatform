package org.example.core.dto.response;

public record CourseSearchResponse(
        Long id,
        String title,
        String description,
        String category,
        String status,
        Long teacherId,
        String teacherName
) {
}
