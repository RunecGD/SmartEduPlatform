package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.CourseRequest;
import org.example.core.dto.response.CourseResponse;
import org.example.core.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest req) {
        return courseService.create(req);
    }

    @PostMapping("/{courseId}/publish")
    @PreAuthorize("hasRole('TEACHER')")
    public CourseResponse publish(@PathVariable Long courseId) {
        return courseService.publishCourse(courseId);
    }


    @GetMapping
    public List<CourseResponse> catalog() {
        return courseService.catalog();
    }

    @GetMapping("/{courseId}")
    public CourseResponse get(@PathVariable Long courseId) {
        return courseService.getCourse(courseId);
    }
}