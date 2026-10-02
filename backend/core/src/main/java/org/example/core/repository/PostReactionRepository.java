package org.example.core.repository;

import org.example.core.model.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostReactionRepository
        extends JpaRepository<PostReaction, Long> {

    Optional<PostReaction> findByPostIdAndUserId(
            Long postId,
            Long userId
    );
}