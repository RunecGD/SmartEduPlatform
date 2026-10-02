package org.example.core.dto.request;
import jakarta.validation.constraints.*;
import org.example.core.dto.enums.CourseStatus;
public record AdminCourseRequest(@NotNull @Positive Long teacherId, @NotNull CourseStatus status) {}
