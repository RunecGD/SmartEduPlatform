package com.example.codeexecutionservice.dto;


public record CodeExecutionResponse(

        ExecutionStatus status,

        String stdout,

        String stderr,

        Integer exitCode,

        Long executionTimeMs
) {
}
