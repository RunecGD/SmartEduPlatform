package org.example.core.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamWebSocketScheduler {

    private final ExamWebSocketService examWebSocketService;

    /**
     * Обновляем таймер каждую секунду.
     */
    @Scheduled(fixedRate = 1000)
    public void updateTimers() {

        examWebSocketService.updateTimers();
    }
}