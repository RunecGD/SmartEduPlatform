package org.example.core.dto.response;

import org.example.core.dto.enums.Role;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        Boolean isEnabled,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}