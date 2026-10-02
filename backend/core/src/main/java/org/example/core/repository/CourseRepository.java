package org.example.core.repository;

import org.example.core.dto.enums.CourseStatus;
import org.example.core.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByTeacherId(Long teacherId);
    boolean existsByTeacher_Id(Long teacherId);

    @Query("""
            SELECT c
            FROM Course c
            WHERE c.status = :status
              AND (
                    LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(c.category) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY c.createdAt DESC
            """)
    List<Course> search(
            @Param("query") String query,
            @Param("status") CourseStatus status
    );
}