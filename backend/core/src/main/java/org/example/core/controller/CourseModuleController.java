package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.CourseModuleRequest;
import org.example.core.dto.response.CourseModuleResponse;
import org.example.core.service.CourseModuleService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/course_modules")
@RequiredArgsConstructor
public class CourseModuleController {
    private final CourseModuleService courseModuleService;


    @PostMapping("/{courseId}")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseModuleResponse addModule(@PathVariable Long courseId,
                                          @Valid @RequestBody CourseModuleRequest req) {
        return courseModuleService.addModule(courseId, req);
    }
}
