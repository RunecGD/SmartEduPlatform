package com.example.notificationservice.security;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketAuthInterceptor implements ChannelInterceptor {
    private final NotificationIdentityResolver identityResolver;
    @Override public Message<?> preSend(Message<?> message,MessageChannel channel) {
        var accessor=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);
        if (accessor==null) throw new AccessDeniedException("Нет STOMP headers");
        var command=accessor.getCommand();
        if (command==null) return message;
        if (command==StompCommand.CONNECT) {
            String bearer=accessor.getFirstNativeHeader("Authorization");
            Long id=identityResolver.resolve(bearer);
            accessor.setUser(new UsernamePasswordAuthenticationToken(id.toString(),null,List.of()));
            if (accessor.getSessionAttributes()==null) throw new AccessDeniedException("Нет сессии");
            accessor.getSessionAttributes().put("notificationBearer",bearer);
            return message;
        }
        if (command==StompCommand.SEND) throw new AccessDeniedException("Клиент не может публиковать уведомления");
        if (command==StompCommand.DISCONNECT) return message;
        if (accessor.getUser()==null) throw new AccessDeniedException("Требуется авторизация");
        if (command==StompCommand.SUBSCRIBE) {
            String allowed="/topic/notifications/"+accessor.getUser().getName();
            if (!allowed.equals(accessor.getDestination())) throw new AccessDeniedException("Чужие уведомления");
            Object token=accessor.getSessionAttributes()==null ? null : accessor.getSessionAttributes().get("notificationBearer");
            if (!(token instanceof String bearer)) throw new AccessDeniedException("Нет JWT сессии");
            identityResolver.requireOwner(Long.valueOf(accessor.getUser().getName()),bearer);
        }
        return message;
    }
}
