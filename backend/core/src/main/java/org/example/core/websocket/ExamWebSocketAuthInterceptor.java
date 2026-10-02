package org.example.core.websocket;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.core.model.ExamAttempt;
import org.example.core.model.User;
import org.example.core.repository.ExamAttemptRepository;
import org.example.core.repository.UserRepository;
import org.example.core.utils.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamWebSocketAuthInterceptor
        implements ChannelInterceptor {

    private final org.example.core.service.AccessGuard accessGuard;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final ExamAttemptRepository examAttemptRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) throw new AccessDeniedException("STOMP headers отсутствуют");

        StompCommand command =
                accessor.getCommand();

        if (command == null) {
            return message;
        }

        /*
         * Авторизация при STOMP CONNECT.
         */
        if (StompCommand.CONNECT.equals(command)) {

            authenticate(accessor);

            return message;
        }

        /*
         * Проверяем доступ к конкретному экзамену
         * при подписке на /topic/exams/{attemptId}.
         */
        if (StompCommand.SUBSCRIBE.equals(command)) {

            checkSubscriptionAccess(accessor);

            return message;
        }

        if (command == StompCommand.SEND) {
            throw new AccessDeniedException("Отправка сообщений клиентом не поддерживается");
        }
        if (accessor.getUser() == null && command != StompCommand.DISCONNECT) {
            throw new AccessDeniedException("WebSocket не авторизован");
        }
        return message;
    }

    private void authenticate(
            StompHeaderAccessor accessor
    ) {

        String authorization =
                accessor.getFirstNativeHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            throw new AccessDeniedException(
                    "JWT отсутствует"
            );
        }

        String token =
                authorization.substring(7);

        try {

            if (!jwtService.isValid(token)) {
                throw new AccessDeniedException(
                        "JWT недействителен"
                );
            }

            String email =
                    jwtService.extractEmail(token);

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(
                            email
                    );

            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked() || !userDetails.isAccountNonExpired() || !userDetails.isCredentialsNonExpired()) {
                throw new AccessDeniedException(
                        "Пользователь заблокирован"
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            /*
             * Сохраняем authentication непосредственно
             * в STOMP-сессию.
             */
            accessor.setUser(authentication);

        } catch (JwtException | IllegalArgumentException exception) {

            throw new AccessDeniedException(
                    "JWT недействителен"
            );
        }
    }

    private void checkSubscriptionAccess(
            StompHeaderAccessor accessor
    ) {

        if (accessor.getUser() == null) {

            throw new AccessDeniedException(
                    "WebSocket не авторизован"
            );
        }

        String destination =
                accessor.getDestination();

        if (destination == null) {
            throw new AccessDeniedException(
                    "Destination отсутствует"
            );
        }

        /*
         * Ожидаемый destination:
         *
         * /topic/exams/123
         */
        String prefix = "/topic/exams/";

        if (!destination.startsWith(prefix)) {
            throw new AccessDeniedException(
                    "Недопустимый WebSocket destination"
            );
        }

        String attemptIdString =
                destination.substring(prefix.length());

        Long attemptId;

        try {
            attemptId =
                    Long.parseLong(attemptIdString);
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException(
                    "Некорректный attemptId"
            );
        }

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Попытка не найдена"
                                ));

        String email =
                accessor.getUser().getName();

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Пользователь не найден"
                                ));

        accessGuard.attemptResult(attempt, user);
    }
}
