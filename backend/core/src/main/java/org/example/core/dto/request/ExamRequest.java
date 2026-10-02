package org.example.core.dto.request;
import jakarta.validation.constraints.*;
public record ExamRequest(@NotBlank @Size(max=255) String title,@NotNull @Min(1) @Max(1440) Integer timeLimitMinutes) {}
