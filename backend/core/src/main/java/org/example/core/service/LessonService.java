package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.LessonRequest;
import org.example.core.dto.response.LessonResponse;
import org.example.core.mapper.LessonMapper;
import org.example.core.model.Course;
import org.example.core.model.CourseModule;
import org.example.core.model.Lesson;
import org.example.core.model.User;
import org.example.core.repository.CourseModuleRepository;
import org.example.core.repository.LessonRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final CourseModuleRepository courseModuleRepository;
    private final LessonMapper lessonMapper;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    @Transactional
    public LessonResponse addLesson(Long moduleId, LessonRequest req) {
        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("Модуль не найден"));
        checkOwner(module.getCourse());

        Lesson lesson = Lesson.builder()
                .module(module)
                .type(req.type())
                .title(req.title())
                .content(req.content())
                .orderIndex(req.orderIndex())
                .build();
        return lessonMapper.toDto(lessonRepository.save(lesson));
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
