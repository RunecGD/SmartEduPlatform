package com.example.codeexecutionservice.controller;

import com.example.codeexecutionservice.dto.CodeExecutionRequest;
import com.example.codeexecutionservice.dto.CodeExecutionResponse;
import com.example.codeexecutionservice.service.CodeExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/code")
@RequiredArgsConstructor
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;
    private final com.example.codeexecutionservice.security.ExecutionIdentityResolver identityResolver;

    @PostMapping("/execute")
    public ResponseEntity<CodeExecutionResponse> execute(
            @RequestHeader(value="Authorization",required=false) String authorization,
            @Valid @RequestBody CodeExecutionRequest request
    ) {
        identityResolver.resolve(authorization);
        return ResponseEntity.ok(
                codeExecutionService.execute(request)
        );
    }
}
