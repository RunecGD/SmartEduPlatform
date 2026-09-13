package org.example.core.websocket;

import lombok.RequiredArgsConstructor;
import org.example.core.dto.enums.AttemptStatus;
import org.example.core.dto.response.ExamWebSocketResponse;
import org.example.core.model.ExamAttempt;
import org.example.core.repository.ExamAttemptRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamWebSocketService {

    private final ExamAttemptRepository examAttemptRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Обновляет таймеры всех активных попыток.
     */
    @Transactional
    public void updateTimers() {

        List<ExamAttempt> attempts =
                examAttemptRepository
                        .findAll()
                        .stream()
                        .filter(attempt ->
                                attempt.getStatus()
                                        == AttemptStatus.IN_PROGRESS)
                        .toList();

        for (ExamAttempt attempt : attempts) {

            processAttempt(attempt);
        }
    }

    private void processAttempt(
            ExamAttempt attempt
    ) {

        Instant now = Instant.now();

        Instant deadline =
                attempt.getStartedAt()
                        .plusSeconds(
                                attempt.getExam()
                                        .getTimeLimitMinutes()
                                        * 60L
                        );

        long remainingSeconds =
                Duration.between(
                        now,
                        deadline
                ).getSeconds();

        /*
         * Время истекло.
         */
        if (remainingSeconds <= 0) {

            attempt.setStatus(
                    AttemptStatus.EXPIRED
            );

            attempt.setFinishedAt(now);

            examAttemptRepository.save(attempt);

            sendStatus(
                    attempt,
                    0
            );

            return;
        }

        /*
         * Время ещё есть.
         */
        sendStatus(
                attempt,
                remainingSeconds
        );
    }

    private void sendStatus(
            ExamAttempt attempt,
            long remainingSeconds
    ) {

        Instant deadline =
                attempt.getStartedAt()
                        .plusSeconds(
                                attempt.getExam()
                                        .getTimeLimitMinutes()
                                        * 60L
                        );

        ExamWebSocketResponse response =
                new ExamWebSocketResponse(
                        attempt.getId(),
                        attempt.getStatus().name(),
                        remainingSeconds,
                        attempt.getStartedAt(),
                        deadline
                );

        messagingTemplate.convertAndSend(
                "/topic/exams/" + attempt.getId(),
                response
        );
    }
}