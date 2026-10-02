package org.example.core.repository;

import org.example.core.model.ForumTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumTopicRepository
        extends JpaRepository<ForumTopic, Long> {

    List<ForumTopic> findByCourseIdOrderByCreatedAtDesc(
            Long courseId
    );
}