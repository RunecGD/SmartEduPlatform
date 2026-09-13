package org.example.core.dto.response;

import java.time.Instant;

public record MaterialResponse(
        Long id,
        Long lessonId,
        String fileName,
        String contentType,
        Long sizeBytes,
        Instant uploadedAt
) {}