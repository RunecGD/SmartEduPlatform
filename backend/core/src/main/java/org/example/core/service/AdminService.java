package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.Role;
import org.example.core.dto.request.*;
import org.example.core.dto.response.*;
import org.example.core.mapper.*;
import org.example.core.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')") @Transactional(readOnly=true)
public class AdminService {
    private final UserRepository users;
    private final CourseRepository courses;
    private final StudyGroupRepository groups;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AccessGuard guard;
    private final AdminMutationLock mutationLock;

    public List<UserResponse> users() {
        return users.findAll(Sort.by("fullName", "id")).stream().map(userMapper::toDto).toList();
    }
    public List<CourseResponse> courses() {
        return courses.findAll(Sort.by("title", "id")).stream().map(courseMapper::toDto).toList();
    }
    @Transactional
    public UserResponse updateUser(Long id, AdminUserRequest request) {
        mutationLock.acquire();
        var current = guard.currentUser();
        var user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
        if (current.getId().equals(id) && (request.role() != Role.ADMIN || !request.isEnabled()))
            throw new IllegalStateException("Нельзя заблокировать себя или снять свою роль администратора");
        if (request.role() != user.getRole()) {
            if (groups.existsByCurator_Id(id) || groups.existsByStudents_Id(id)
                    || courses.existsByTeacher_Id(id))
                throw new IllegalStateException("Сначала измените назначение в группах и курсах");
        }
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setIsEnabled(request.isEnabled());
        return userMapper.toDto(users.saveAndFlush(user));
    }
    @Transactional
    public CourseResponse updateCourse(Long id, AdminCourseRequest request) {
        mutationLock.acquire();
        var course = courses.findById(id).orElseThrow(() -> new EntityNotFoundException("Курс не найден"));
        var teacher = users.findById(request.teacherId()).orElseThrow(() -> new EntityNotFoundException("Преподаватель не найден"));
        if (teacher.getRole() != Role.TEACHER || !Boolean.TRUE.equals(teacher.getIsEnabled()))
            throw new IllegalArgumentException("Нужен активный преподаватель");
        course.setTeacher(teacher);
        course.setStatus(request.status());
        return courseMapper.toDto(courses.saveAndFlush(course));
    }
}
