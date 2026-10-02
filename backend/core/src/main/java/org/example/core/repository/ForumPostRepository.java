package org.example.core.repository;

import org.example.core.model.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumPostRepository
        extends JpaRepository<ForumPost, Long> {
}