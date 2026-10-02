package org.example.core.service;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.ExamProgressResponse;
import org.example.core.dto.response.LessonProgressResponse;
import org.example.core.dto.response.StudentProgressResponse;
import org.example.core.model.Enrollment;
import org.example.core.model.ExamAttempt;
import org.example.core.model.Lesson;
import org.example.core.model.LessonProgress;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.ExamAttemptRepository;
import org.example.core.repository.LessonProgressRepository;
import org.example.core.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final AccessGuard accessGuard;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<StudentProgressResponse> getStudentProgress(
            Long userId
    ) {

        accessGuard.ownProgress(userId);

        List<Enrollment> enrollments =
                enrollmentRepository.findByUserId(userId);

        List<ExamAttempt> attempts =
                examAttemptRepository.findByUser_Id(userId);

        return enrollments.stream()
                .map(enrollment ->
                        buildCourseProgress(
                                enrollment,
                                attempts
                        )
                )
                .toList();
    }

    // Package-private: callers must authorize the enrollment before building its result.
    StudentProgressResponse buildCourseProgress(
            Enrollment enrollment,
            List<ExamAttempt> attempts
    ) {

        Long courseId =
                enrollment.getCourse().getId();

        List<Lesson> lessons =
                lessonRepository.findByModule_Course_Id(courseId);

        List<LessonProgress> progress =
                lessons.stream()
                        .map(lesson ->
                                lessonProgressRepository
                                        .findByEnrollment_IdAndLesson_Id(
                                                enrollment.getId(),
                                                lesson.getId()
                                        )
                                        .orElse(null)
                        )
                        .filter(progressItem -> progressItem != null)
                        .toList();

        List<LessonProgressResponse> lessonResponses =
                lessons.stream()
                        .map(lesson ->
                                mapLessonProgress(
                                        lesson,
                                        enrollment,
                                        progress
                                )
                        )
                        .toList();

        int totalLessons = lessons.size();

        int completedLessons = (int) progress.stream().filter(p -> p.getCompletedAt() != null).count();

        BigDecimal progressPercent = totalLessons == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedLessons * 100L)
                    .divide(BigDecimal.valueOf(totalLessons), 2, java.math.RoundingMode.HALF_UP);

        List<ExamProgressResponse> exams =
                attempts.stream()
                        .filter(attempt ->
                                attempt.getExam()
                                        .getLesson()
                                        .getModule()
                                        .getCourse()
                                        .getId()
                                        .equals(courseId)
                        )
                        .map(this::mapExam)
                        .toList();

        return new StudentProgressResponse(
                courseId,
                enrollment.getCourse().getTitle(),
                progressPercent,
                completedLessons,
                totalLessons,
                lessonResponses,
                exams
        );
    }

    private LessonProgressResponse mapLessonProgress(
            Lesson lesson,
            Enrollment enrollment,
            List<LessonProgress> progress
    ) {

        return progress.stream()
                .filter(item ->
                        item.getLesson()
                                .getId()
                                .equals(lesson.getId())
                )
                .findFirst()
                .map(item ->
                        new LessonProgressResponse(
                                item.getId(),
                                enrollment.getId(),
                                lesson.getId(),
                                item.getCompletedAt(),
                                item.getScore()
                        )
                )
                .orElseGet(() ->
                        new LessonProgressResponse(
                                null,
                                enrollment.getId(),
                                lesson.getId(),
                                null,
                                null
                        )
                );
    }

    private ExamProgressResponse mapExam(
            ExamAttempt attempt
    ) {

        return new ExamProgressResponse(
                attempt.getExam().getId(),
                attempt.getExam().getTitle(),
                attempt.getTotalScore(),
                attempt.getStatus().name()
        );
    }
}
