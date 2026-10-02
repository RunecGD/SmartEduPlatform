package org.example.core.dto.response;
import java.math.BigDecimal; import java.util.List;
public record StudentProgressResponse(Long courseId, String courseTitle, BigDecimal progressPercent, Integer completedLessons, Integer totalLessons, List<LessonProgressResponse> lessons, List<ExamProgressResponse> exams) {}
