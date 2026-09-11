package org.example.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(

        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        @Size(max = 255)
        String email,

        @NotBlank(message = "ФИО обязательно")
        @Size(max = 100, message = "ФИО не больше 100 символов")
        String fullName,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 255, message = "Пароль от 8 до 255 символов")
        String password
) {}