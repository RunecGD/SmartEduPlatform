package org.example.core.controller;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.core.dto.response.ApiErrorResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;

@RestControllerAdvice
public class BackendExceptionHandler {
    private ResponseEntity<ApiErrorResponse> error(HttpStatus status,String message,HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(),status.value(),status.getReasonPhrase(),message,request.getRequestURI()));
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EntityNotFoundException ex,HttpServletRequest request) { return error(HttpStatus.NOT_FOUND,ex.getMessage(),request); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(AccessDeniedException ex,HttpServletRequest request) { return error(HttpStatus.FORBIDDEN,"Недостаточно прав",request); }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> unauthorized(AuthenticationException ex,HttpServletRequest request) { return error(HttpStatus.UNAUTHORIZED,"Неверные учетные данные",request); }
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> invalid(Exception ex,HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST,"Проверьте параметры запроса",request); }
    @ExceptionHandler({IllegalStateException.class,DataIntegrityViolationException.class})
    public ResponseEntity<ApiErrorResponse> conflict(Exception ex,HttpServletRequest request) { return error(HttpStatus.CONFLICT,"Операция недоступна в текущем состоянии",request); }
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiErrorResponse> upstream(RestClientException ex,HttpServletRequest request) { return error(HttpStatus.BAD_GATEWAY,"AI-сервис временно недоступен",request); }
}
