package org.example.core.repository;

import org.example.core.dto.enums.CourseStatus;
import org.example.core.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStatus(CourseStatus status);
    List<Course> findByTeacherId(Long teacherId);
}
