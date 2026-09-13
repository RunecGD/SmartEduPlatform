package org.example.core.repository;

import org.example.core.dto.enums.AttemptStatus;
import org.example.core.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}