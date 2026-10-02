package org.example.core.dto.request;

public record ForumAnsweredEvent(
        Long userId,
        Long questionId,
        Long answerId
) {
}
