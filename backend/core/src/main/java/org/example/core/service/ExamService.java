package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.core.config.AiServiceClient;
import org.example.core.config.EventPublisher;
import org.example.core.dto.enums.AttemptStatus;
import org.example.core.dto.request.*;
import org.example.core.dto.response.AiGradeResponse;
import org.example.core.dto.response.AiQuestionResponse;
import org.example.core.dto.response.ExamAnswerResponse;
import org.example.core.dto.response.ExamAttemptResponse;
import org.example.core.dto.response.ExamQuestionResponse;
import org.example.core.dto.response.ExamResponse;
import org.example.core.model.Course;
import org.example.core.model.Exam;
import org.example.core.model.ExamAnswer;
import org.example.core.model.ExamAttempt;
import org.example.core.model.ExamQuestion;
import org.example.core.model.Lesson;
import org.example.core.model.User;
import org.example.core.repository.ExamAttemptRepository;
import org.example.core.repository.ExamQuestionRepository;
import org.example.core.repository.ExamRepository;
import org.example.core.repository.LessonRepository;
import org.example.core.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final AccessGuard accessGuard;
    private final jakarta.persistence.EntityManager entityManager;
    private final org.example.core.repository.EnrollmentRepository enrollmentRepository;
    @org.springframework.beans.factory.annotation.Value("${exam.pass-percent:60}")
    private int passPercent;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;
    private final JdbcTemplate jdbcTemplate;
    private final EventPublisher eventPublisher;

    @Transactional
    public ExamResponse createExam(
            Long lessonId,
            ExamRequest request
    ) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Урок не найден"));

        checkOwner(lesson.getModule().getCourse());

        if (request.title() == null || request.title().isBlank() || request.title().length()>255) {
            throw new IllegalArgumentException(
                    "Название экзамена не может быть пустым"
            );
        }

        if (request.timeLimitMinutes() == null
                || request.timeLimitMinutes() <= 0 || request.timeLimitMinutes() > 1440) {
            throw new IllegalArgumentException(
                    "Время экзамена должно быть больше 0 минут"
            );
        }

        Exam exam = Exam.builder()
                .lesson(lesson)
                .title(request.title())
                .timeLimitMinutes(request.timeLimitMinutes())
                .build();

        Exam saved = examRepository.save(exam);

        return new ExamResponse(
                saved.getId(),
                lessonId,
                saved.getTitle(),
                saved.getTimeLimitMinutes()
        );
    }

    @Transactional
    public List<ExamQuestionResponse> generateQuestions(
            Long examId, int count
    ) {
        if (count < 1 || count > 20) throw new IllegalArgumentException("Количество вопросов: от 1 до 20");

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Экзамен не найден"));

        entityManager.lock(exam, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        checkOwner(exam.getLesson().getModule().getCourse());
        if (examAttemptRepository.existsByExam_Id(examId)) {
            throw new IllegalStateException("Нельзя менять вопросы экзамена после начала попыток");
        }

        List<AiQuestionResponse> generated =
                aiServiceClient.generateExam(
                        exam.getLesson().getId(),
                        count
                );

        if (generated == null || generated.isEmpty()) {
            throw new IllegalStateException(
                    "AI-сервис не сгенерировал вопросы"
            );
        }

        examQuestionRepository.deleteAllByExamId(examId);
        examQuestionRepository.flush();

        List<ExamQuestion> questions = new ArrayList<>();

        for (int i = 0; i < generated.size(); i++) {

            AiQuestionResponse aiQuestion = generated.get(i);

            if (aiQuestion.question() == null
                    || aiQuestion.question().isBlank()) {
                continue;
            }

            int maxScore = aiQuestion.maxScore() == null
                    ? 10
                    : aiQuestion.maxScore();

            if (maxScore <= 0) {
                maxScore = 10;
            }

            ExamQuestion question = ExamQuestion.builder()
                    .exam(exam)
                    .questionText(aiQuestion.question())
                    .maxScore(maxScore)
                    .orderIndex(i)
                    .build();

            questions.add(question);
        }

        if (questions.isEmpty()) {
            throw new IllegalStateException(
                    "AI-сервис вернул некорректные вопросы"
            );
        }

        List<ExamQuestion> savedQuestions =
                examQuestionRepository.saveAll(questions);

        return savedQuestions.stream()
                .map(question -> new ExamQuestionResponse(
                        question.getId(),
                        question.getQuestionText(),
                        question.getMaxScore(),
                        question.getOrderIndex()
                ))
                .toList();
    }

    @Transactional
    public ExamAttemptResponse startAttempt(Long examId) {

        User user = currentUser();

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Экзамен не найден"));

        entityManager.lock(exam, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        Course course = exam.getLesson().getModule().getCourse();
        if (course.getStatus() != org.example.core.dto.enums.CourseStatus.PUBLISHED
                || !enrollmentRepository.existsByUserIdAndCourseId(user.getId(), course.getId())) {
            throw new AccessDeniedException("Необходима запись на опубликованный курс");
        }
        for (ExamAttempt active : examAttemptRepository.findByUser_Id(user.getId())) {
            if (active.getExam().getId().equals(examId) && active.getStatus() == AttemptStatus.IN_PROGRESS) {
                entityManager.lock(active, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                entityManager.refresh(active);
                if (active.getStatus() == AttemptStatus.IN_PROGRESS && isExpired(active)) {
                    active.setStatus(AttemptStatus.EXPIRED);
                    active.setFinishedAt(Instant.now());
                    examAttemptRepository.save(active);
                }
            }
        }

        List<ExamQuestion> questions =
                examQuestionRepository.findByExamIdOrderByOrderIndexAsc(
                        examId
                );

        if (questions.isEmpty()) {
            throw new IllegalStateException(
                    "Нельзя начать экзамен без вопросов"
            );
        }

        boolean hasActiveAttempt =
                examAttemptRepository
                        .existsByExamIdAndUserIdAndStatus(
                                examId,
                                user.getId(),
                                AttemptStatus.IN_PROGRESS
                        );

        if (hasActiveAttempt) {
            return examAttemptRepository.findByUser_Id(user.getId()).stream()
                .filter(a -> a.getExam().getId().equals(examId) && a.getStatus()==AttemptStatus.IN_PROGRESS)
                .findFirst().map(this::toResponse).orElseThrow();
        }

        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .user(user)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .answers(new ArrayList<>())
                .build();

        ExamAttempt saved =
                examAttemptRepository.save(attempt);

        return toResponse(saved);
    }

    @Transactional
    public ExamAttemptResponse submitAttempt(
            Long attemptId,
            List<ExamAnswerRequest> answerRequests
    ) {

        User user = currentUser();

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Попытка не найдена"));

        entityManager.lock(attempt, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(attempt);
        checkAttemptOwner(attempt, user);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Эта попытка уже завершена"
            );
        }

        if (isExpired(attempt)) {

            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setFinishedAt(Instant.now());

            ExamAttempt saved =
                    examAttemptRepository.save(attempt);

            return toResponse(saved);
        }

        Exam exam = attempt.getExam();

        List<ExamQuestion> questions =
                examQuestionRepository
                        .findByExamIdOrderByOrderIndexAsc(
                                exam.getId()
                        );

        if (questions.isEmpty()) {
            throw new IllegalStateException(
                    "В экзамене нет вопросов"
            );
        }

        Map<Long, String> submittedAnswers =
                normalizeAnswers(answerRequests);

        validateQuestionIds(
                submittedAnswers,
                questions
        );

        List<String> chunks =
                getLessonChunks(
                        exam.getLesson().getId()
                );

        if (chunks.isEmpty()) {
            throw new IllegalStateException(
                    "Для проверки экзамена нет проиндексированных материалов"
            );
        }

        List<AiGradeQuestion> aiQuestions =
                new ArrayList<>();

        List<ExamAnswerRequest> aiAnswers =
                new ArrayList<>();

        for (ExamQuestion question : questions) {

            aiQuestions.add(
                    new AiGradeQuestion(
                            question.getId(),
                            question.getQuestionText(),
                            question.getMaxScore(),
                            chunks
                    )
            );

            aiAnswers.add(
                    new ExamAnswerRequest(
                            question.getId(),
                            submittedAnswers.getOrDefault(
                                    question.getId(),
                                    ""
                            )
                    )
            );
        }

        AiGradeAttemptRequest aiRequest =
                new AiGradeAttemptRequest(
                        aiQuestions,
                        aiAnswers
                );

        List<AiGradeResponse> grades =
                aiServiceClient.gradeAttempt(aiRequest);

        if (grades == null) {
            throw new IllegalStateException(
                    "AI-сервис не вернул результаты проверки"
            );
        }

        Map<Long, AiGradeResponse> gradesByQuestionId =
                new HashMap<>();

        for (AiGradeResponse grade : grades) {

            if (grade == null || grade.questionId() == null) {
                continue;
            }

            if (gradesByQuestionId.containsKey(grade.questionId())
                    || questions.stream().noneMatch(q -> q.getId().equals(grade.questionId()))) {
                throw new IllegalStateException("AI вернул дублирующийся или посторонний вопрос");
            }
            gradesByQuestionId.put(
                    grade.questionId(),
                    grade
            );
        }

        for (ExamQuestion question : questions) {

            if (!gradesByQuestionId.containsKey(question.getId())) {
                throw new IllegalStateException(
                        "AI-сервис не проверил вопрос: "
                                + question.getId()
                );
            }
        }

        if (attempt.getAnswers() == null) attempt.setAnswers(new ArrayList<>());
        attempt.getAnswers().clear();

        int totalScore = 0;

        for (ExamQuestion question : questions) {

            AiGradeResponse grade =
                    gradesByQuestionId.get(question.getId());

            int score = normalizeScore(
                    grade.score(),
                    question.getMaxScore()
            );

            String answerText =
                    submittedAnswers.getOrDefault(
                            question.getId(),
                            ""
                    );

            ExamAnswer answer = ExamAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .answerText(answerText)
                    .score(score)
                    .aiFeedback(grade.feedback())
                    .build();

            attempt.getAnswers().add(answer);

            totalScore += score;
        }

        attempt.setTotalScore(totalScore);
        attempt.setFinishedAt(Instant.now());
        attempt.setStatus(AttemptStatus.FINISHED);

        ExamAttempt saved =
                examAttemptRepository.save(attempt);

        /*
         * Попытка успешно завершена.
         * Отправляем событие в Notification Service через RabbitMQ.
         */
        int maximumScore = questions.stream().mapToInt(ExamQuestion::getMaxScore).sum();
        if (passPercent < 1 || passPercent > 100) throw new IllegalStateException("exam.pass-percent должен быть 1..100");
        if (maximumScore > 0 && totalScore * 100L >= maximumScore * (long) passPercent) {
        eventPublisher.publishExamPassed(
                new ExamPassedEvent(
                        user.getId(),
                        exam.getId(),
                        saved.getId(),
                        totalScore
                )
        );

        }
        return toResponse(saved);
    }

    @Transactional
    public ExamAttemptResponse getAttemptResult(
            Long attemptId
    ) {

        User user = currentUser();

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Попытка не найдена"
                                ));

        entityManager.lock(attempt, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(attempt);
        checkResultAccess(attempt, user);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS
                && isExpired(attempt)) {

            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setFinishedAt(Instant.now());

            attempt =
                    examAttemptRepository.save(attempt);
        }

        return toResponse(attempt);
    }

    private boolean isExpired(ExamAttempt attempt) {

        Integer timeLimit =
                attempt.getExam().getTimeLimitMinutes();

        if (timeLimit == null || timeLimit <= 0) {
            return false;
        }

        Instant deadline =
                attempt.getStartedAt()
                        .plus(Duration.ofMinutes(timeLimit));

        return !Instant.now().isBefore(deadline);
    }

    private Map<Long, String> normalizeAnswers(
            List<ExamAnswerRequest> answerRequests
    ) {

        Map<Long, String> result = new HashMap<>();

        if (answerRequests == null) {
            return result;
        }

        for (ExamAnswerRequest answer : answerRequests) {

            if (answer == null || answer.questionId() == null) {
                throw new IllegalArgumentException(
                        "questionId ответа не может быть null"
                );
            }

            String answerText = answer.answerText();

            if (answerText == null) {
                answerText = "";
            }

            if (result.containsKey(answer.questionId())) {
                throw new IllegalArgumentException("Повторяющийся questionId");
            }
            if (answerText.length() > 10000) throw new IllegalArgumentException("Ответ слишком длинный");
            result.put(
                    answer.questionId(),
                    answerText
            );
        }

        return result;
    }

    private void validateQuestionIds(
            Map<Long, String> submittedAnswers,
            List<ExamQuestion> questions
    ) {

        Set<Long> validQuestionIds =
                new HashSet<>();

        for (ExamQuestion question : questions) {
            validQuestionIds.add(question.getId());
        }

        for (Long questionId : submittedAnswers.keySet()) {

            if (!validQuestionIds.contains(questionId)) {
                throw new IllegalArgumentException(
                        "Вопрос " + questionId
                                + " не принадлежит этому экзамену"
                );
            }
        }
    }

    private int normalizeScore(
            Integer score,
            Integer maxScore
    ) {

        if (score == null) {
            return 0;
        }

        if (maxScore == null || maxScore <= 0) {
            return Math.max(score, 0);
        }

        return Math.max(
                0,
                Math.min(score, maxScore)
        );
    }

    private List<String> getLessonChunks(Long lessonId) {

        return jdbcTemplate.query(
                """
                SELECT mc.content
                FROM material_chunks mc
                JOIN materials m
                    ON m.id = mc.material_id
                WHERE m.lesson_id = ?
                ORDER BY random()
                LIMIT 10
                """,
                (rs, rowNum) ->
                        rs.getString("content"),
                lessonId
        );
    }

    private void checkAttemptOwner(
            ExamAttempt attempt,
            User user
    ) {

        if (!attempt.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "Это не ваша попытка"
            );
        }
    }

    private void checkResultAccess(
            ExamAttempt attempt,
            User user
    ) {

        accessGuard.attemptResult(attempt, user);
    }

    private void checkOwner(Course course) {

        User user = currentUser();

        if (!course.getTeacher().getId().equals(user.getId())
                && !isAdmin()) {

            throw new AccessDeniedException(
                    "Недостаточно прав"
            );
        }
    }

    private User currentUser() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Пользователь не найден"
                        ));
    }

    private boolean isAdmin() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_ADMIN")
                );
    }

    private ExamAttemptResponse toResponse(
            ExamAttempt attempt
    ) {

        List<ExamAnswerResponse> answers =
                (attempt.getAnswers() == null ? java.util.stream.Stream.<ExamAnswer>empty() : attempt.getAnswers().stream())
                        .map(this::toAnswerResponse)
                        .toList();

        return new ExamAttemptResponse(
                attempt.getId(),
                attempt.getExam().getId(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getTotalScore(),
                attempt.getStatus().name(),
                answers
        );
    }

    private ExamAnswerResponse toAnswerResponse(
            ExamAnswer answer
    ) {

        ExamQuestion question =
                answer.getQuestion();

        return new ExamAnswerResponse(
                question.getId(),
                question.getQuestionText(),
                answer.getAnswerText(),
                answer.getScore(),
                answer.getAiFeedback(),
                question.getMaxScore()
        );
    }
}
