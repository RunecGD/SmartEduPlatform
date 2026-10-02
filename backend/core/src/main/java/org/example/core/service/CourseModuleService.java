package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.CourseModuleRequest;
import org.example.core.dto.response.CourseModuleResponse;
import org.example.core.mapper.CourseModuleMapper;
import org.example.core.model.Course;
import org.example.core.model.CourseModule;
import org.example.core.model.User;
import org.example.core.repository.CourseModuleRepository;
import org.example.core.repository.CourseRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseModuleService {
    private final CourseRepository courseRepository;
    private final CourseModuleMapper courseModuleMapper;
    private final CourseModuleRepository courseModuleRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseModuleResponse addModule(Long courseId, CourseModuleRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Курс не найден"));
        checkOwner(course);

        CourseModule module = CourseModule.builder()
                .course(course)
                .title(req.title())
                .orderIndex(req.orderIndex())
                .build();
        return courseModuleMapper.toDto(courseModuleRepository.save(module));
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
