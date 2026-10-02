package org.example.core.dto.request;
import jakarta.validation.constraints.*;
import java.util.Set;
public record StudyGroupRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @Positive Long curatorId,
        @NotNull @Size(max = 2000) Set<@NotNull @Positive Long> studentIds
) {}
