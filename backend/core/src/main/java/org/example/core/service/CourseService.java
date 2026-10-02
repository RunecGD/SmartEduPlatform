package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.CourseStatus;
import org.example.core.dto.request.CourseRequest;
import org.example.core.dto.response.CourseResponse;
import org.example.core.dto.response.CourseSearchResponse;
import org.example.core.mapper.CourseMapper;
import org.example.core.model.Course;
import org.example.core.model.User;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
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

        return courseMapper.toDto(
                courseRepository.save(course)
        );
    }

    @Transactional
    public CourseResponse publishCourse(Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Курс не найден")
                );

        checkOwner(course);

        course.setStatus(CourseStatus.PUBLISHED);

        return courseMapper.toDto(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> catalog() {

        return courseRepository
                .findByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(courseMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Курс не найден")
                );

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            checkOwner(course);
        }

        return courseMapper.toDto(course);
    }

    @Transactional(readOnly = true)
    public List<CourseSearchResponse> searchCourses(String query) {

        String normalizedQuery =
                query == null ? "" : query.trim();

        if (normalizedQuery.isBlank()) {

            return courseRepository
                    .findByStatus(CourseStatus.PUBLISHED)
                    .stream()
                    .map(this::toSearchResponse)
                    .toList();
        }

        return courseRepository
                .search(
                        normalizedQuery,
                        CourseStatus.PUBLISHED
                )
                .stream()
                .map(this::toSearchResponse)
                .toList();
    }

    private CourseSearchResponse toSearchResponse(
            Course course
    ) {

        return new CourseSearchResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCategory(),
                course.getStatus().name(),
                course.getTeacher().getId(),
                course.getTeacher().getFullName()
        );
    }

    private User currentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Пользователь не найден"
                        )
                );
    }

    private void checkOwner(Course course) {

        if (!course.getTeacher().getId().equals(currentUser().getId())
                && !isAdmin()) {

            throw new AccessDeniedException(
                    "Недостаточно прав"
            );
        }
    }

    private boolean isAdmin() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                );
    }
}
