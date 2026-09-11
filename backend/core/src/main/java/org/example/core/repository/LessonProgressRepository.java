package org.example.core.repository;

import org.example.core.model.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
    boolean existsByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);
    Optional<LessonProgress> findByEnrollment_IdAndLesson_Id(Long enrollmentId, Long lessonId);
    long countByEnrollment_Id(Long enrollmentId);  // пройдено уроков
}
