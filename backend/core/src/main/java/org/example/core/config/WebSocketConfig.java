package org.example.core.config;
import lombok.RequiredArgsConstructor;
import org.example.core.websocket.ExamWebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.config.annotation.*;
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final ExamWebSocketAuthInterceptor examWebSocketAuthInterceptor;
    @Value("${app.cors-origins:http://localhost:4173,http://localhost:5173,http://127.0.0.1:4173}")
    private String origins;
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new));
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(examWebSocketAuthInterceptor);
    }
}
