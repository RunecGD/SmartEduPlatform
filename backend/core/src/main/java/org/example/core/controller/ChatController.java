package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.config.AiServiceClient;
import org.example.core.dto.request.AskRequest;
import org.example.core.dto.response.AskResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ask")
@RequiredArgsConstructor
public class ChatController {

    private final AiServiceClient aiServiceClient;

    @PostMapping("/{lessonId}/lesson")
    @PreAuthorize("hasRole('STUDENT')")
    public AskResponse ask(@PathVariable Long lessonId,
                           @Valid @RequestBody AskRequest req) {
        return aiServiceClient.ask(req.question(), lessonId);
    }
}