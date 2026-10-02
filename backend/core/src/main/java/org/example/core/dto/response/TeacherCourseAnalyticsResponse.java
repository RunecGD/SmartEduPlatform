package org.example.core.dto.response;
import java.math.BigDecimal; import java.util.List;
public record TeacherCourseAnalyticsResponse(Long courseId, String courseTitle, Integer totalStudents, Integer completedStudents, BigDecimal averageProgress, BigDecimal averageExamScore, List<LessonAnalyticsResponse> lessons) {}
