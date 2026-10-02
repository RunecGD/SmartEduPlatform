package org.example.core.dto.response;
import java.time.Instant;
import java.util.List;
public record StudentResultsResponse(Long userId, String fullName, String email,
                                     List<CourseResults> courses) {
    public record CourseResults(StudentProgressResponse progress, List<AttemptSummary> attempts) {}
    public record AttemptSummary(Long attemptId, Long examId, String examTitle, String status,
                                 Integer totalScore, Instant finishedAt) {}
}
