package com.example.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@lombok.RequiredArgsConstructor
public class NotificationWebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final com.example.notificationservice.security.NotificationWebSocketAuthInterceptor interceptor;
    @org.springframework.beans.factory.annotation.Value("${app.cors-origins:http://localhost:4173,http://localhost:5173}")
    private String origins;
    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }
    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {
        registry.addEndpoint("/ws/notifications")
                .setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new));
    }
}
