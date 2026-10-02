package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.StudentResultsResponse;
import org.example.core.dto.response.StudentResultsResponse.*;
import org.example.core.model.*;
import org.example.core.repository.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class ResultsService {
    private final AccessGuard guard;
    private final CourseRepository courses;
    private final StudyGroupRepository groups;
    private final EnrollmentRepository enrollments;
    private final ExamAttemptRepository attempts;
    private final ProgressService progress;

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public List<StudentResultsResponse> course(Long courseId) {
        var course = courses.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Курс не найден"));
        guard.owner(course);
        // Never include this student's enrollments or attempts from another teacher's course.
        return enrollments.findByCourseId(courseId).stream()
                .sorted(Comparator.comparing(e -> e.getUser().getFullName()))
                .map(e -> student(e.getUser(), List.of(e))).toList();
    }

    @PreAuthorize("hasAnyRole('METHODIST','ADMIN')")
    public List<StudentResultsResponse> group(Long groupId) {
        var group = groups.findById(groupId).orElseThrow(() -> new EntityNotFoundException("Группа не найдена"));
        guard.group(group);
        return group.getStudents().stream().sorted(Comparator.comparing(User::getFullName))
                .map(user -> student(user, enrollments.findByUserId(user.getId()))).toList();
    }

    private StudentResultsResponse student(User user, List<Enrollment> scopedEnrollments) {
        var result = scopedEnrollments.stream().sorted(Comparator.comparing(e -> e.getCourse().getTitle()))
                .map(enrollment -> {
                    var scopedAttempts = attempts.findByUser_IdAndExam_Lesson_Module_Course_IdOrderByStartedAtDesc(
                            user.getId(), enrollment.getCourse().getId());
                    return new CourseResults(progress.buildCourseProgress(enrollment, scopedAttempts),
                            scopedAttempts.stream().map(a -> new AttemptSummary(a.getId(), a.getExam().getId(),
                                    a.getExam().getTitle(), a.getStatus().name(), a.getTotalScore(), a.getFinishedAt())).toList());
                }).toList();
        return new StudentResultsResponse(user.getId(), user.getFullName(), user.getEmail(), result);
    }
}
