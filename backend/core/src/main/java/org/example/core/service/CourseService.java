package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.CourseStatus;
import org.example.core.dto.request.CourseRequest;
import org.example.core.dto.request.LessonProgressRequest;
import org.example.core.dto.response.CourseResponse;
import org.example.core.dto.response.EnrollmentResponse;
import org.example.core.dto.response.LessonProgressResponse;
import org.example.core.mapper.CourseMapper;
import org.example.core.model.Course;
import org.example.core.model.Enrollment;
import org.example.core.model.Lesson;
import org.example.core.model.LessonProgress;
import org.example.core.model.User;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.LessonProgressRepository;
import org.example.core.repository.LessonRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;


    @Transactional
    public CourseResponse create(CourseRequest req) {
        User teacher = currentUser();
        Course course = Course.builder()
                .teacher(teacher)
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .status(CourseStatus.DRAFT)
                .build();
        return courseMapper.toDto(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse publishCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Курс не найден"));
        checkOwner(course);
        course.setStatus(CourseStatus.PUBLISHED);
        return courseMapper.toDto(course);
    }


    @Transactional(readOnly = true)
    public List<CourseResponse> catalog() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED).stream()
                .map(courseMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Курс не найден"));
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            checkOwner(course);   // неопубликованное видит только владелец
        }
        return courseMapper.toDto(course);
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