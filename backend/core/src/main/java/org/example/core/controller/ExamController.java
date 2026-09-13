package org.example.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.ExamAnswerRequest;
import org.example.core.dto.request.ExamRequest;
import org.example.core.dto.response.ExamAttemptResponse;
import org.example.core.model.Exam;
import org.example.core.model.ExamAttempt;
import org.example.core.model.ExamQuestion;
import org.example.core.service.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/exem")
@RestController
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/lessons/{lessonId}")
    @PreAuthorize("hasRole('TEACHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Exam createExam(
            @PathVariable Long lessonId,
            @Valid @RequestBody ExamRequest request
    ) {
        return examService.createExam(
                lessonId,
                request
        );
    }

    @PostMapping("/{examId}/generate")
    @PreAuthorize("hasRole('TEACHER')")
    public List<ExamQuestion> generateQuestions(
            @PathVariable Long examId
    ) {
        return examService.generateQuestions(examId);
    }

    @PostMapping("/{examId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamAttempt startAttempt(
            @PathVariable Long examId
    ) {
        return examService.startAttempt(examId);
    }


    @PostMapping("/attempts/{attemptId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ExamAttemptResponse submitAttempt(
            @PathVariable Long attemptId,
            @RequestBody(required = false)
            List<ExamAnswerRequest> answers
    ) {
        return examService.submitAttempt(
                attemptId,
                answers
        );
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ExamAttemptResponse getAttemptResult(
            @PathVariable Long attemptId
    ) {
        return examService.getAttemptResult(
                attemptId
        );
    }
}