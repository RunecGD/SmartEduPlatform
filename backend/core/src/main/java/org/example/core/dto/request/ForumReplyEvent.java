package org.example.core.dto.request;

public record ForumReplyEvent(
        Long userId,
        Long topicId,
        Long postId
) {
}
