package org.example.core.dto.response;

import java.time.Instant;

public record ForumPostResponse(
        Long id,
        Long topicId,
        Long authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}
