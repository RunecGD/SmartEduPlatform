package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.CourseStatus;
import org.example.core.dto.response.EnrollmentResponse;
import org.example.core.mapper.EnrollmentMapper;
import org.example.core.model.Course;
import org.example.core.model.Enrollment;
import org.example.core.model.User;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final UserRepository userRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public EnrollmentResponse enroll(Long courseId) {
        User student = currentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Курс не найден"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException("Курс ещё не опубликован");
        }
        if (enrollmentRepository.existsByUserIdAndCourseId(student.getId(), courseId)) {
            throw new IllegalArgumentException("Вы уже записаны на этот курс");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(student)
                .course(course)
                .progressPct(0)
                .build();
        return enrollmentMapper.toDto(enrollmentRepository.save(enrollment));
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
