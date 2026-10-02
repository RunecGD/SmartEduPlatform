package org.example.core.dto.request;
import jakarta.validation.constraints.*;
import org.example.core.dto.enums.Role;
public record AdminUserRequest(@NotBlank @Size(max=100) String fullName,
                               @NotNull Role role, @NotNull Boolean isEnabled) {}
