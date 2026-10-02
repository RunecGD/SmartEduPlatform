package org.example.core.repository;

import org.example.core.dto.enums.AttemptStatus;
import org.example.core.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    boolean existsByExamIdAndUserIdAndStatus(
            Long examId,
            Long userId,
            AttemptStatus status
    );

    Optional<ExamAttempt> findByIdAndUserId(
            Long attemptId,
            Long userId
    );
    List<ExamAttempt> findByUser_Id(Long userId);
    boolean existsByExam_Id(Long examId);
    List<ExamAttempt> findByUser_IdAndExam_Lesson_Module_Course_IdOrderByStartedAtDesc(Long userId, Long courseId);
}