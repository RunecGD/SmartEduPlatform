package org.example.core.service;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.LessonAnalyticsResponse;
import org.example.core.dto.response.TeacherCourseAnalyticsResponse;
import org.example.core.model.Course;
import org.example.core.model.Enrollment;
import org.example.core.model.Lesson;
import org.example.core.model.LessonProgress;
import org.example.core.model.ExamAttempt;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.ExamAttemptRepository;
import org.example.core.repository.LessonProgressRepository;
import org.example.core.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AccessGuard accessGuard;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public TeacherCourseAnalyticsResponse getCourseAnalytics(
            Long courseId
    ) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Курс не найден"
                        )
                );

        accessGuard.owner(course);

        List<Enrollment> enrollments =
                enrollmentRepository.findByCourseId(courseId);

        List<Lesson> lessons =
                lessonRepository.findByModule_Course_Id(courseId);

        int totalStudents = enrollments.size();

        List<BigDecimal> percentages = enrollments.stream().map(e -> {
            long done=lessons.stream().filter(l -> lessonProgressRepository
                    .findByEnrollment_IdAndLesson_Id(e.getId(),l.getId())
                    .map(p->p.getCompletedAt()!=null).orElse(false)).count();
            return lessons.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(done*100L)
                    .divide(BigDecimal.valueOf(lessons.size()),2,RoundingMode.HALF_UP);
        }).toList();
        int completedStudents=(int)percentages.stream().filter(p->p.compareTo(BigDecimal.valueOf(100))>=0).count();
        BigDecimal averageProgress=percentages.isEmpty() ? BigDecimal.ZERO : percentages.stream()
                .reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(percentages.size()),2,RoundingMode.HALF_UP);

        List<ExamAttempt> attempts =
                examAttemptRepository.findAll()
                        .stream()
                        .filter(attempt ->
                                attempt.getExam()
                                        .getLesson()
                                        .getModule()
                                        .getCourse()
                                        .getId()
                                        .equals(courseId)
                        )
                        .toList();

        BigDecimal averageExamScore =
                calculateAverageExamScore(attempts);

        List<LessonAnalyticsResponse> lessonAnalytics =
                lessons.stream()
                        .map(lesson ->
                                buildLessonAnalytics(
                                        lesson,
                                        enrollments
                                )
                        )
                        .toList();

        return new TeacherCourseAnalyticsResponse(
                course.getId(),
                course.getTitle(),
                totalStudents,
                completedStudents,
                averageProgress,
                averageExamScore,
                lessonAnalytics
        );
    }

    private BigDecimal calculateAverageExamScore(
            List<ExamAttempt> attempts
    ) {

        List<Integer> scores = attempts.stream()
                .filter(a -> a.getStatus() == org.example.core.dto.enums.AttemptStatus.FINISHED)
                .map(ExamAttempt::getTotalScore).filter(java.util.Objects::nonNull).toList();
        if (scores.isEmpty()) return BigDecimal.ZERO;
        return scores.stream().map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private LessonAnalyticsResponse buildLessonAnalytics(
            Lesson lesson,
            List<Enrollment> enrollments
    ) {

        int totalStudents = enrollments.size();

        int completedStudents = 0;

        for (Enrollment enrollment : enrollments) {

            boolean completed =
                    lessonProgressRepository
                            .findByEnrollment_IdAndLesson_Id(
                                    enrollment.getId(),
                                    lesson.getId()
                            )
                            .map(progress ->
                                    progress.getCompletedAt() != null
                            )
                            .orElse(false);

            if (completed) {
                completedStudents++;
            }
        }

        int completionPercent =
                totalStudents == 0
                        ? 0
                        : completedStudents * 100 / totalStudents;

        return new LessonAnalyticsResponse(
                lesson.getId(),
                lesson.getTitle(),
                completedStudents,
                totalStudents,
                completionPercent
        );
    }
}
