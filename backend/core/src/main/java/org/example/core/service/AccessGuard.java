package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.model.Course;
import org.example.core.model.User;
import org.example.core.repository.EnrollmentRepository;
import org.example.core.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessGuard {
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final org.example.core.repository.StudyGroupRepository studyGroups;

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Требуется авторизация");
        }
        return userRepository.findByEmail(auth.getName())
                .filter(user -> Boolean.TRUE.equals(user.getIsEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    }

    public void owner(Course course) {
        User user = currentUser();
        if (!course.getTeacher().getId().equals(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Курс принадлежит другому преподавателю");
        }
    }

    public void courseAccess(Course course) {
        User user = currentUser();
        if (isAdmin(user) || course.getTeacher().getId().equals(user.getId())
                || enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            return;
        }
        throw new AccessDeniedException("Вы не записаны на этот курс");
    }

    public void ownProgress(Long userId) {
        if (!currentUser().getId().equals(userId)) {
            throw new AccessDeniedException("Это прогресс другого пользователя");
        }
    }

    public void group(org.example.core.model.StudyGroup group) {
        User user = currentUser();
        if (isAdmin(user) || (user.getRole() == org.example.core.dto.enums.Role.METHODIST
                && group.getCurator().getId().equals(user.getId()))) return;
        throw new AccessDeniedException("Это группа другого куратора");
    }

    // Shared by HTTP results and STOMP subscriptions. Always use the current DB role.
    public void attemptResult(org.example.core.model.ExamAttempt attempt, User user) {
        if (!Boolean.TRUE.equals(user.getIsEnabled())) throw new AccessDeniedException("Пользователь заблокирован");
        var role = user.getRole();
        if (isAdmin(user)
                || (role == org.example.core.dto.enums.Role.STUDENT && attempt.getUser().getId().equals(user.getId()))
                || (role == org.example.core.dto.enums.Role.TEACHER
                    && attempt.getExam().getLesson().getModule().getCourse().getTeacher().getId().equals(user.getId()))
                || (role == org.example.core.dto.enums.Role.METHODIST
                    && studyGroups.existsByCurator_IdAndStudents_Id(user.getId(), attempt.getUser().getId()))) return;
        throw new AccessDeniedException("Недостаточно прав для просмотра результата");
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole().name());
    }
}
