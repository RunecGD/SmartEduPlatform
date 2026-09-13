package org.example.core.repository;

import org.example.core.model.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(Long examId);
    void deleteAllByExamId(Long examId);
}