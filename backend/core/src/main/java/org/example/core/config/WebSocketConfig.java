package org.example.core.config;

import lombok.RequiredArgsConstructor;
import org.example.core.websocket.ExamWebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig
        implements WebSocketMessageBrokerConfigurer {

    private final ExamWebSocketAuthInterceptor
            examWebSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(
            MessageBrokerRegistry registry
    ) {

        /*
         * Сообщения от сервера клиентам:
         *
         * /topic/...
         */
        registry.enableSimpleBroker("/topic");

        /*
         * Сообщения от клиента серверу:
         *
         * /app/...
         */
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(
            StompEndpointRegistry registry
    ) {

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(
            ChannelRegistration registration
    ) {

        registration.interceptors(examWebSocketAuthInterceptor);
    }
}