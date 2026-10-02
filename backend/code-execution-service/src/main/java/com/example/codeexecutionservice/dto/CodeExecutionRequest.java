package com.example.codeexecutionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeExecutionRequest(

        @NotBlank
        String language,

        @NotBlank
        @Size(max = 100_000)
        String code,

        @Size(max = 10_000)
        String stdin
) {
}
