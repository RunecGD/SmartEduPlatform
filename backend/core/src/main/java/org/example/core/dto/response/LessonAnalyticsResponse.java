package org.example.core.dto.response;

public record LessonAnalyticsResponse(
        Long lessonId,
        String lessonTitle,
        Integer completedStudents,
        Integer totalStudents,
        Integer completionPercent
) {
}
