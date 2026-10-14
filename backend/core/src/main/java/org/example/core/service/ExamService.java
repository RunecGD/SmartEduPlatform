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
        // The exam lock serializes starts, including simultaneous browser requests.
        var previous = examAttemptRepository.findFirstByExamIdAndUserIdOrderByStartedAtDescIdDesc(examId, user.getId());
        if (previous.isPresent()) {
            ExamAttempt existing = previous.get();
            entityManager.lock(existing, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            entityManager.refresh(existing);
            if (existing.getStatus() == AttemptStatus.IN_PROGRESS && isExpired(existing)) {
                existing.setStatus(AttemptStatus.EXPIRED);
                existing.setFinishedAt(Instant.now());
                examAttemptRepository.save(existing);
            }
            return toResponse(existing);
        }
        if (examQuestionRepository.findByExamIdOrderByOrderIndexAsc(examId).isEmpty()) {
            throw new IllegalStateException("Нельзя начать экзамен без вопросов");
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

        if (attempt.getAnswers() == null) attempt.setAnswers(new ArrayList<>());
        for (ExamQuestion question : questions) {
            attempt.getAnswers().add(ExamAnswer.builder()
                    .attempt(attempt).question(question)
                    .answerText(submittedAnswers.getOrDefault(question.getId(), ""))
                    .build());
        }
        attempt.setTotalScore(null);
        attempt.setFinishedAt(Instant.now()); // Time of submission, never time of grading.
        attempt.setStatus(AttemptStatus.SUBMITTED);
        return toResponse(examAttemptRepository.save(attempt));
    }

    @Transactional
    public ExamAttemptResponse gradeWithAi(Long attemptId) {
        ExamAttempt attempt = reviewableAttempt(attemptId);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new IllegalStateException("Работа уже проверена. Оценку можно изменить вручную");
        }
        List<String> chunks = getLessonChunks(attempt.getExam().getLesson().getId());
        if (chunks.isEmpty()) {
            throw new IllegalStateException("Нет проиндексированных материалов. Проверьте работу вручную или проиндексируйте материалы урока");
        }
        List<AiGradeQuestion> questions = attempt.getAnswers().stream()
                .map(a -> new AiGradeQuestion(a.getQuestion().getId(), a.getQuestion().getQuestionText(),
                        a.getQuestion().getMaxScore(), chunks)).toList();
        List<ExamAnswerRequest> answers = attempt.getAnswers().stream()
                .map(a -> new ExamAnswerRequest(a.getQuestion().getId(), a.getAnswerText())).toList();
        List<AiGradeResponse> grades = aiServiceClient.gradeAttempt(new AiGradeAttemptRequest(questions, answers));
        if (grades == null) throw new IllegalStateException("ИИ не вернул результаты проверки");
        Map<Long, AiGradeResponse> byId = new HashMap<>();
        Set<Long> expected = new HashSet<>();
        attempt.getAnswers().forEach(a -> expected.add(a.getQuestion().getId()));
        for (AiGradeResponse grade : grades) {
            if (grade == null || grade.questionId() == null || !expected.contains(grade.questionId())
                    || byId.putIfAbsent(grade.questionId(), grade) != null) {
                throw new IllegalStateException("ИИ вернул некорректный список оценок");
            }
        }
        if (!byId.keySet().equals(expected)) throw new IllegalStateException("ИИ проверил не все вопросы");
        for (ExamAnswer answer : attempt.getAnswers()) {
            AiGradeResponse grade = byId.get(answer.getQuestion().getId());
            if (grade.score() == null || grade.score() < 0 || grade.score() > answer.getQuestion().getMaxScore()) {
                throw new IllegalStateException("ИИ вернул оценку вне допустимого диапазона");
            }
        }
        for (ExamAnswer answer : attempt.getAnswers()) {
            AiGradeResponse grade = byId.get(answer.getQuestion().getId());
            answer.setScore(grade.score());
            answer.setAiFeedback(grade.feedback());
        }
        return finishReview(attempt);
    }

    @Transactional
    public ExamAttemptResponse gradeManually(Long attemptId, List<ExamGradeRequest> grades) {
        ExamAttempt attempt = reviewableAttempt(attemptId);
        if (grades == null) throw new IllegalArgumentException("Укажите оценки по всем вопросам");
        Map<Long, ExamGradeRequest> byId = new HashMap<>();
        for (ExamGradeRequest grade : grades) {
            if (grade == null || grade.questionId() == null || grade.score() == null
                    || byId.putIfAbsent(grade.questionId(), grade) != null) {
                throw new IllegalArgumentException("Некорректный или повторяющийся вопрос");
            }
        }
        if (byId.size() != attempt.getAnswers().size()) throw new IllegalArgumentException("Оцените все вопросы");
        for (ExamAnswer answer : attempt.getAnswers()) {
            ExamGradeRequest grade = byId.get(answer.getQuestion().getId());
            if (grade == null || grade.score() < 0 || grade.score() > answer.getQuestion().getMaxScore()
                    || (grade.feedback() != null && grade.feedback().length() > 10000)) {
                throw new IllegalArgumentException("Проверьте баллы и комментарии по вопросам");
            }
        }
        for (ExamAnswer answer : attempt.getAnswers()) {
            ExamGradeRequest grade = byId.get(answer.getQuestion().getId());
            answer.setScore(grade.score());
            // Existing column stores feedback from either the teacher or the AI.
            answer.setAiFeedback(grade.feedback());
        }
        return finishReview(attempt);
    }

    private ExamAttempt reviewableAttempt(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new EntityNotFoundException("Попытка не найдена"));
        if (!canReview(attempt, currentUser())) throw new AccessDeniedException("Проверять работу может преподаватель этого курса");
        entityManager.lock(attempt, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(attempt);
        if (attempt.getStatus() != AttemptStatus.SUBMITTED && attempt.getStatus() != AttemptStatus.FINISHED) {
            throw new IllegalStateException("Работа ещё не отправлена на проверку или время попытки истекло");
        }
        if (attempt.getAnswers() == null || attempt.getAnswers().isEmpty()) {
            throw new IllegalStateException("У попытки нет сохранённых ответов");
        }
        return attempt;
    }

    private boolean canReview(ExamAttempt attempt, User user) {
        return Boolean.TRUE.equals(user.getIsEnabled())
                && (user.getRole() == org.example.core.dto.enums.Role.ADMIN
                    || (user.getRole() == org.example.core.dto.enums.Role.TEACHER
                        && attempt.getExam().getLesson().getModule().getCourse().getTeacher().getId().equals(user.getId())));
    }

    private ExamAttemptResponse finishReview(ExamAttempt attempt) {
        boolean firstReview = attempt.getStatus() == AttemptStatus.SUBMITTED;
        int totalScore = attempt.getAnswers().stream().mapToInt(ExamAnswer::getScore).sum();
        int maximum = attempt.getAnswers().stream().mapToInt(a -> a.getQuestion().getMaxScore()).sum();
        attempt.setTotalScore(totalScore);
        attempt.setStatus(AttemptStatus.FINISHED);
        ExamAttempt saved = examAttemptRepository.save(attempt);
        if (passPercent < 1 || passPercent > 100) throw new IllegalStateException("exam.pass-percent должен быть 1..100");
        // Edits update results without sending repeated exam-passed notifications.
        if (firstReview && maximum > 0 && totalScore * 100L >= maximum * (long) passPercent) {
            eventPublisher.publishExamPassed(new ExamPassedEvent(attempt.getUser().getId(),
                    attempt.getExam().getId(), attempt.getId(), totalScore));
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
                answers,
                canReview(attempt, currentUser())
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
