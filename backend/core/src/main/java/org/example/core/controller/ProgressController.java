package org.example.core.controller;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.StudentProgressResponse;
import org.example.core.service.ProgressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @GetMapping("/student/{userId}")
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentProgressResponse> getStudentProgress(
            @PathVariable Long userId
    ) {
        return progressService.getStudentProgress(userId);
    }
}
