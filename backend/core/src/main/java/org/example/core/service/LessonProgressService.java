package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.LessonProgressRequest;
import org.example.core.dto.response.LessonProgressResponse;
import org.example.core.model.*;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.LessonProgressRepository;
import org.example.core.repository.LessonRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Service
@RequiredArgsConstructor
public class LessonProgressService {
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public LessonProgressResponse completeLesson(Long lessonId, LessonProgressRequest req) {
        User student = currentUser();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Урок не найден"));
        Long courseId = lesson.getModule().getCourse().getId();

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new AccessDeniedException("Вы не записаны на этот курс"));

        LessonProgress progress = lessonProgressRepository
                .findByEnrollment_IdAndLesson_Id(enrollment.getId(), lessonId)
                .orElse(LessonProgress.builder()
                        .enrollment(enrollment)
                        .lesson(lesson)
                        .build());

        progress.setCompletedAt(Instant.now());
        progress.setScore(req.score());
        lessonProgressRepository.save(progress);

        long total = lessonRepository.countByModule_Course_Id(courseId);
        long done = lessonProgressRepository.countByEnrollment_Id(enrollment.getId());
        enrollment.setProgressPct(total == 0 ? 0 : (int) (done * 100 / total));
        enrollmentRepository.save(enrollment);

        return new LessonProgressResponse(progress.getId(), enrollment.getId(),
                lessonId, progress.getCompletedAt(), progress.getScore());
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    private void checkOwner(Course course) {
        if (!course.getTeacher().getId().equals(currentUser().getId())
                && !isAdmin()) {
            throw new AccessDeniedException("Недостаточно прав");
        }
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
