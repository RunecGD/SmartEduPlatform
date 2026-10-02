package org.example.core.dto.response;

import java.time.Instant;
import java.util.List;

public record ForumTopicResponse(
        Long id,
        Long courseId,
        Long authorId,
        String authorName,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<ForumPostResponse> posts
) {
}
