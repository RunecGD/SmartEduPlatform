package org.example.core.controller;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.response.TeacherCourseAnalyticsResponse;
import org.example.core.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public TeacherCourseAnalyticsResponse getCourseAnalytics(
            @PathVariable Long courseId
    ) {
        return analyticsService.getCourseAnalytics(courseId);
    }
}
