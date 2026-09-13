package org.example.core.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.core.config.AiServiceClient;
import org.example.core.dto.enums.AttemptStatus;
import org.example.core.dto.request.AiGradeAttemptRequest;
import org.example.core.dto.request.AiGradeQuestion;
import org.example.core.dto.request.ExamAnswerRequest;
import org.example.core.dto.request.ExamRequest;
import org.example.core.dto.response.AiGradeResponse;
import org.example.core.dto.response.AiQuestionResponse;
import org.example.core.dto.response.ExamAnswerResponse;
import org.example.core.dto.response.ExamAttemptResponse;
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

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Создание экзамена преподавателем.
     *
     * Экзамен создаётся для конкретного урока.
     * Преподаватель должен владеть курсом, которому принадлежит урок.
     */
    @Transactional
    public Exam createExam(Long lessonId, ExamRequest request) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Урок не найден"));

        checkOwner(lesson.getModule().getCourse());

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException(
                    "Название экзамена не может быть пустым");
        }

        if (request.timeLimitMinutes() == null
                || request.timeLimitMinutes() <= 0) {
            throw new IllegalArgumentException(
                    "Время экзамена должно быть больше 0 минут");
        }

        Exam exam = Exam.builder()
                .lesson(lesson)
                .title(request.title())
                .timeLimitMinutes(request.timeLimitMinutes())
                .build();

        return examRepository.save(exam);
    }

    /**
     * Генерация вопросов экзамена через AI-сервис.
     *
     * Старые вопросы экзамена удаляются и заменяются
     * новыми вопросами, полученными от Python-сервиса.
     */
    @Transactional
    public List<ExamQuestion> generateQuestions(Long examId) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Экзамен не найден"));

        checkOwner(exam.getLesson().getModule().getCourse());

        List<AiQuestionResponse> generated =
                aiServiceClient.generateExam(
                        exam.getLesson().getId(),
                        5
                );

        if (generated == null || generated.isEmpty()) {
            throw new IllegalStateException(
                    "AI-сервис не сгенерировал вопросы");
        }

        examQuestionRepository.deleteAllByExamId(examId);

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
                    "AI-сервис вернул некорректные вопросы");
        }

        return examQuestionRepository.saveAll(questions);
    }

    /**
     * Старт экзамена студентом.
     */
    @Transactional
    public ExamAttempt startAttempt(Long examId) {

        User user = currentUser();

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Экзамен не найден"));

        List<ExamQuestion> questions =
                examQuestionRepository.findByExamIdOrderByOrderIndexAsc(
                        examId
                );

        if (questions.isEmpty()) {
            throw new IllegalStateException(
                    "Нельзя начать экзамен без вопросов");
        }

        boolean hasActiveAttempt =
                examAttemptRepository
                        .existsByExamIdAndUserIdAndStatus(
                                examId,
                                user.getId(),
                                AttemptStatus.IN_PROGRESS
                        );

        if (hasActiveAttempt) {
            throw new IllegalStateException(
                    "У вас уже есть незавершённая попытка этого экзамена"
            );
        }

        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .user(user)
                .status(AttemptStatus.IN_PROGRESS)
                .build();

        return examAttemptRepository.save(attempt);
    }

    /**
     * Отправка ответов студента.
     *
     * Перед отправкой проверяется серверный таймер.
     * Если время закончилось, попытка переводится в EXPIRED.
     */
    @Transactional
    public ExamAttemptResponse submitAttempt(
            Long attemptId,
            List<ExamAnswerRequest> answerRequests
    ) {

        User user = currentUser();

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Попытка не найдена"));

        checkAttemptOwner(attempt, user);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Эта попытка уже завершена");
        }

        /*
         * Проверяем время на сервере.
         */
        if (isExpired(attempt)) {

            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setFinishedAt(Instant.now());

            ExamAttempt saved = examAttemptRepository.save(attempt);

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
                    "В экзамене нет вопросов");
        }

        /*
         * Нормализуем входящие ответы.
         *
         * В результате для каждого вопроса будет ровно один ответ.
         * Если студент вопрос пропустил — отправляем пустую строку.
         */
        Map<Long, String> submittedAnswers =
                normalizeAnswers(answerRequests);

        /*
         * Проверяем, что клиент не прислал вопрос,
         * которого нет в данном экзамене.
         */
        validateQuestionIds(submittedAnswers, questions);

        /*
         * Получаем материал урока для AI-проверки.
         */
        List<String> chunks =
                getLessonChunks(exam.getLesson().getId());

        if (chunks.isEmpty()) {
            throw new IllegalStateException(
                    "Для проверки экзамена нет проиндексированных материалов"
            );
        }

        /*
         * Формируем запрос к Python AI-сервису.
         */
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
                    "AI-сервис не вернул результаты проверки");
        }

        /*
         * Индексируем результаты AI по questionId,
         * чтобы не делать stream-поиск для каждого вопроса.
         */
        Map<Long, AiGradeResponse> gradesByQuestionId =
                new HashMap<>();

        for (AiGradeResponse grade : grades) {

            if (grade == null || grade.questionId() == null) {
                continue;
            }

            gradesByQuestionId.put(
                    grade.questionId(),
                    grade
            );
        }

        /*
         * Если AI не вернул результат хотя бы для одного вопроса,
         * считаем это ошибкой проверки, а не завершаем экзамен
         * с некорректным totalScore.
         */
        for (ExamQuestion question : questions) {

            if (!gradesByQuestionId.containsKey(question.getId())) {
                throw new IllegalStateException(
                        "AI-сервис не проверил вопрос: "
                                + question.getId()
                );
            }
        }

        /*
         * Удаляем старые ответы, если они каким-то образом есть.
         */
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

        return toResponse(saved);
    }

    /**
     * Получение результата попытки.
     *
     * Студент может посмотреть только свою попытку.
     * Преподаватель может посмотреть попытку студента
     * по своему курсу.
     */
    @Transactional
    public ExamAttemptResponse getAttemptResult(Long attemptId) {

        User user = currentUser();

        ExamAttempt attempt =
                examAttemptRepository.findById(attemptId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Попытка не найдена"
                                ));

        checkResultAccess(attempt, user);

        /*
         * Если клиент запросил результат после истечения времени,
         * но попытка ещё числится IN_PROGRESS,
         * синхронизируем статус.
         */
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS
                && isExpired(attempt)) {

            attempt.setStatus(AttemptStatus.EXPIRED);
            attempt.setFinishedAt(Instant.now());

            attempt =
                    examAttemptRepository.save(attempt);
        }

        return toResponse(attempt);
    }

    /**
     * Проверка серверного таймера.
     */
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

    /**
     * Нормализация ответов клиента.
     *
     * Если один questionId приходит несколько раз,
     * последний ответ заменяет предыдущий.
     */
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

            result.put(
                    answer.questionId(),
                    answerText
            );
        }

        return result;
    }

    /**
     * Проверяем, что все questionId принадлежат экзамену.
     */
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

    /**
     * Защитная проверка оценки от AI.
     *
     * Score не может быть меньше 0 и больше maxScore.
     */
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

    /**
     * Получение chunks материалов урока.
     */
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

    /**
     * Проверка владельца попытки.
     */
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

    /**
     * Проверка доступа к результату.
     *
     * Студент:
     *   только собственная попытка.
     *
     * Преподаватель:
     *   только экзамен своего курса.
     *
     * Администратор:
     *   полный доступ.
     */
    private void checkResultAccess(
            ExamAttempt attempt,
            User user
    ) {

        if (isAdmin()) {
            return;
        }

        if (attempt.getUser().getId().equals(user.getId())) {
            return;
        }

        Course course =
                attempt.getExam()
                        .getLesson()
                        .getModule()
                        .getCourse();

        if (course.getTeacher().getId().equals(user.getId())) {
            return;
        }

        throw new AccessDeniedException(
                "Недостаточно прав для просмотра результата"
        );
    }

    /**
     * Проверка владельца курса для TEACHER-операций.
     */
    private void checkOwner(Course course) {

        User user = currentUser();

        if (!course.getTeacher().getId().equals(user.getId())
                && !isAdmin()) {

            throw new AccessDeniedException(
                    "Недостаточно прав"
            );
        }
    }

    /**
     * Текущий авторизованный пользователь.
     */
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

    /**
     * Проверка администратора.
     */
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

    /**
     * Entity -> API response.
     */
    private ExamAttemptResponse toResponse(
            ExamAttempt attempt
    ) {

        List<ExamAnswerResponse> answers =
                attempt.getAnswers()
                        .stream()
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

    /**
     * Entity -> API answer response.
     */
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