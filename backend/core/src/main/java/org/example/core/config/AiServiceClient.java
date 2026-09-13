package org.example.core.config;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.AiGenerateExamRequest;
import org.example.core.dto.request.AiGradeAttemptRequest;
import org.example.core.dto.request.AskRequest;
import org.example.core.dto.response.AiGradeResponse;
import org.example.core.dto.response.AiQuestionResponse;
import org.example.core.dto.response.AskResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://localhost:8000")
            .build();

    public void index(String objectKey) {
        restClient.post()
                .uri("/index")
                .body(Map.of("object_key", objectKey))
                .retrieve()
                .toBodilessEntity();
    }

    public AskResponse ask(String question, Long lessonId) {
        return restClient.post()
                .uri("/ask")
                .body(new AskRequest(question))
                .retrieve()
                .body(AskResponse.class);
    }

    public List<AiQuestionResponse> generateExam(Long lessonId, int count) {
        AiGenerateExamRequest request =
                new AiGenerateExamRequest(lessonId, count);

        AiQuestionResponse[] response = restClient.post()
                .uri("/exams/generate")
                .body(request)
                .retrieve()
                .body(AiQuestionResponse[].class);

        return response != null
                ? Arrays.asList(response)
                : List.of();
    }

    public List<AiGradeResponse> gradeAttempt(AiGradeAttemptRequest request) {
        AiGradeResponse[] response = restClient.post()
                .uri("/exams/grade")
                .body(request)
                .retrieve()
                .body(AiGradeResponse[].class);

        return response != null
                ? Arrays.asList(response)
                : List.of();
    }
}