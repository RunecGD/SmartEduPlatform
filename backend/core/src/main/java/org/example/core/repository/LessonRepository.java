package org.example.core.repository;

import org.example.core.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    long countByModule_Course_Id(Long courseId);   // всего уроков в курсе
    List<Lesson> findByModule_Course_Id(Long courseId);
}
