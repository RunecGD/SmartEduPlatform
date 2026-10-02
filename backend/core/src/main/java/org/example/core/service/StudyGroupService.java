package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.Role;
import org.example.core.dto.request.StudyGroupRequest;
import org.example.core.dto.response.StudyGroupResponse;
import org.example.core.mapper.UserMapper;
import org.example.core.model.StudyGroup;
import org.example.core.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class StudyGroupService {
    private final StudyGroupRepository groups;
    private final UserRepository users;
    private final UserMapper mapper;
    private final AccessGuard guard;
    private final AdminMutationLock mutationLock;

    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public List<StudyGroupResponse> list() {
        var user = guard.currentUser();
        return (user.getRole() == Role.ADMIN ? groups.findAllByOrderByNameAsc()
                : groups.findByCurator_IdOrderByNameAsc(user.getId())).stream().map(this::response).toList();
    }

    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public StudyGroupResponse save(Long groupId, StudyGroupRequest request) {
        mutationLock.acquire();
        var group = groupId == null ? new StudyGroup() : groups.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Группа не найдена"));
        var curator = users.findById(request.curatorId())
                .orElseThrow(() -> new EntityNotFoundException("Куратор не найден"));
        if (curator.getRole() != Role.METHODIST || !Boolean.TRUE.equals(curator.getIsEnabled()))
            throw new IllegalArgumentException("Нужен активный куратор");
        var students = users.findAllById(request.studentIds());
        if (students.size() != request.studentIds().size()) throw new IllegalArgumentException("Ученик не найден");
        for (var student : students) {
            // Existing disabled students may remain in their group, preserving access to their history.
            if (student.getRole() != Role.STUDENT) throw new IllegalArgumentException("В группе могут быть только ученики");
            boolean assigned = groupId == null ? groups.existsByStudents_Id(student.getId())
                    : groups.existsByStudents_IdAndIdNot(student.getId(), groupId);
            if (assigned) throw new IllegalStateException("Ученик уже состоит в другой группе");
        }
        group.setName(request.name().trim());
        group.setCurator(curator);
        group.getStudents().clear();
        group.getStudents().addAll(students);
        return response(groups.saveAndFlush(group));
    }

    private StudyGroupResponse response(StudyGroup group) {
        return new StudyGroupResponse(group.getId(), group.getName(), group.getCurator().getId(),
                group.getCurator().getFullName(), group.getStudents().stream()
                .sorted(Comparator.comparing(org.example.core.model.User::getFullName)
                        .thenComparing(org.example.core.model.User::getId)).map(mapper::toDto).toList());
    }
}
