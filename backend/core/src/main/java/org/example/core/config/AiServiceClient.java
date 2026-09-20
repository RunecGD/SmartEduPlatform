package org.example.core.config;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.request.AiGenerateExamRequest;
import org.example.core.dto.request.AiGradeAttemptRequest;
import org.example.core.dto.request.AskRequest;
import org.example.core.dto.response.AiGradeResponse;
import org.example.core.dto.response.AiQuestionResponse;
import org.example.core.dto.response.AskResponse;
import org.springframework.http.MediaType;
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

        Map<String, Object> body = Map.of(
                "question", question,
                "lesson_id", lessonId
        );

        return restClient.post()
                .uri("/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AskResponse.class);
    }

    public List<AiQuestionResponse> generateExam(Long lessonId, int count) {

        String json = "{\"lesson_id\":" + lessonId + ",\"count\":" + count + "}";

        System.out.println("JSON TO AI: " + json);

        try {
            java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                            new java.net.URL("http://localhost:8000/exams/generate")
                                    .openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (var output = connection.getOutputStream()) {
                output.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();

            var stream = status >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            String response = new String(
                    stream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            System.out.println("AI STATUS: " + status);
            System.out.println("AI RESPONSE: " + response);

            if (status < 200 || status >= 300) {
                throw new RuntimeException("AI error: " + status + " " + response);
            }

            var mapper = new tools.jackson.databind.ObjectMapper();

            AiQuestionResponse[] result =
                    mapper.readValue(response, AiQuestionResponse[].class);

            return Arrays.asList(result);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка вызова AI", e);
        }
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