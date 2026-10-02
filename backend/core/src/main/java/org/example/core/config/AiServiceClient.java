package org.example.core.config;
import org.example.core.dto.request.AiGradeAttemptRequest;
import org.example.core.dto.response.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class AiServiceClient {
    private final RestTemplate http;
    private final String baseUrl;
    private final String token;
    public AiServiceClient(@Value("${ai.service.url}") String url, @Value("${ai.service.token}") String token) {
        if (token.isBlank()) throw new IllegalStateException("ai.service.token отсутствует");
        this.token=token;
        this.baseUrl=url.replaceAll("/+$", "");
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(180000);
        this.http=new RestTemplate(factory);
    }
    private <T> T post(String path, Object body, Class<T> type) {
        var headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Token",token);
        T response=http.postForObject(baseUrl+path,new HttpEntity<>(body,headers),type);
        if (response==null) throw new IllegalStateException("AI-сервис вернул пустой ответ");
        return response;
    }
    public void index(String objectKey) { post("/index",Map.of("object_key",objectKey),Map.class); }
    public AskResponse ask(String question,Long lessonId) {
        return post("/ask",Map.of("question",question,"lesson_id",lessonId),AskResponse.class);
    }
    public List<AiQuestionResponse> generateExam(Long lessonId,int count) {
        return Arrays.asList(post("/exams/generate",Map.of("lesson_id",lessonId,"count",count),AiQuestionResponse[].class));
    }
    public List<AiGradeResponse> gradeAttempt(AiGradeAttemptRequest request) {
        var questions=request.questions().stream().map(q -> Map.of("id",q.id(),"text",q.text(),
                "max_score",q.maxScore(),"chunks",q.chunks())).toList();
        var answers=request.answers().stream().map(a -> Map.of("question_id",a.questionId(),
                "answer_text",a.answerText()==null ? "" : a.answerText())).toList();
        return Arrays.asList(post("/exams/grade",Map.of("questions",questions,"answers",answers),AiGradeResponse[].class));
    }
}
