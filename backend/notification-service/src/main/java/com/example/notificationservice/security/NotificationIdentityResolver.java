package com.example.notificationservice.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class NotificationIdentityResolver {
    private final RestTemplate http;
    private final String meUrl;
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public record Identity(Long id,Boolean isEnabled) {}
    public NotificationIdentityResolver(@Value("${core.service.url}") String coreUrl) {
        meUrl=coreUrl.replaceAll("/+$","")+"/api/v1/users/me";
        var factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); factory.setReadTimeout(5000);
        http=new RestTemplate(factory);
    }
    public Long resolve(String authorization) {
        if (authorization==null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Требуется JWT");
        }
        var headers=new HttpHeaders(); headers.set("Authorization",authorization);
        try {
            Identity user=http.exchange(meUrl,HttpMethod.GET,new HttpEntity<>(headers),Identity.class).getBody();
            if (user==null || user.id()==null || !Boolean.TRUE.equals(user.isEnabled())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Учетная запись недоступна");
            }
            return user.id();
        } catch (HttpClientErrorException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Недействительный JWT");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Сервис авторизации недоступен");
        }
    }
    public void requireOwner(Long userId,String authorization) {
        if (!resolve(authorization).equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Это уведомления другого пользователя");
        }
    }
}
