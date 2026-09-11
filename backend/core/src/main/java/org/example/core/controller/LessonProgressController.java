package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.LessonProgressRequest;
import org.example.core.dto.response.LessonProgressResponse;
import org.example.core.service.LessonProgressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lesson_progress")
@RequiredArgsConstructor
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;
    @PostMapping("/{lessonId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public LessonProgressResponse complete(@PathVariable Long lessonId,
                                           @Valid @RequestBody LessonProgressRequest req) {
        return lessonProgressService.completeLesson(lessonId, req);
    }
}
