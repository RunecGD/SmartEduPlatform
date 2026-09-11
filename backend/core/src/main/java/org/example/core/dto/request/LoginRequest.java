package org.example.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email обязателен")
        String email,
        @NotBlank(message = "Пароль обязателен")
        String password
) {}