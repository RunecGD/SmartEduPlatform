package org.example.core.dto.response;
import java.time.OffsetDateTime; import org.example.core.dto.enums.Role;
public record UserResponse(Long id, String email, String fullName, Role role, Boolean isEnabled, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
