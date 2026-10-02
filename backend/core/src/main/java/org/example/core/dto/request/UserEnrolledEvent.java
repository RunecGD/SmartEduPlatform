package org.example.core.dto.request;

public record UserEnrolledEvent(
        Long userId,
        Long courseId
) {
}
