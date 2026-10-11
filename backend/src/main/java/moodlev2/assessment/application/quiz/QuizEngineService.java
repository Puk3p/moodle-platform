package moodlev2.assessment.application.quiz;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.CourseCatalogApi;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.MembershipApi;
import moodlev2.assessment.api.QuizAttemptChangedEvent;
import moodlev2.assessment.infrastructure.mapper.QuizEngineMapper;
import moodlev2.assessment.infrastructure.persistence.QuizAttemptRepository;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.infrastructure.persistence.entity.QuizAttemptEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizOptionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizQuestionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizResponseEntity;
import moodlev2.assessment.web.quiz.dto.OptionReviewDto;
import moodlev2.assessment.web.quiz.dto.QuestionReviewDto;
import moodlev2.assessment.web.quiz.dto.QuizAttemptListDto;
import moodlev2.assessment.web.quiz.dto.QuizAttemptReviewDto;
import moodlev2.assessment.web.quiz.dto.QuizResultDto;
import moodlev2.assessment.web.quiz.dto.QuizResultsResponse;
import moodlev2.assessment.web.quiz.dto.QuizSubmissionDto;
import moodlev2.assessment.web.quiz.dto.StudentQuizViewDto;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.shared.exception.NotFoundException;
import moodlev2.shared.util.ColorUtil;
import moodlev2.shared.util.SlidingWindowCounter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class QuizEngineService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final DirectoryApi directory;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MembershipApi membership;
    private final CourseCatalogApi catalog;
    private final PasswordEncoder passwordEncoder;
    private final QuizEngineMapper mapper;
    private final ApplicationEventPublisher events;

    private static final Set<String> TEACHER_GRADED_TYPES = Set.of("FREE_TEXT", "CODE");

    static final int MAX_PASSWORD_FAILURES = 5;
    static final Duration PASSWORD_WINDOW = Duration.ofMinutes(15);

    private static final DateTimeFormatter WINDOW_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    /** Wrong access passwords per "userId:quizId". In memory: a restart simply clears it. */
    private final SlidingWindowCounter passwordFailures =
            new SlidingWindowCounter(MAX_PASSWORD_FAILURES, PASSWORD_WINDOW, Clock.systemUTC());

    /**
     * Starts (or resumes) the caller's attempt.
     *
     * <p>Students may only start a published quiz of a course they belong to, assigned to their
     * class when the quiz is restricted to classes, and inside its availability window. Staff skip
     * those rules so they can preview any quiz. The access password and attempt limit apply to
     * everyone.
     */
    @Transactional
    public StudentQuizViewDto startAttempt(Long quizId, String userEmail, String providedPassword) {
        UserSummary user =
                directory
                        .findUserByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        QuizEntity quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new NotFoundException("Quiz not found"));

        boolean staff = user.isStaff();
        if (!staff) {
            requireStudentMayOpen(quiz, user);
        }

        // Resuming is allowed after the window closes: the student started in time, and the
        // attempt's own time limit is enforced separately.
        var existingAttempt =
                quizAttemptRepository.findByQuizIdAndUserIdAndStatus(
                        quizId, user.id(), "IN_PROGRESS");
        if (existingAttempt.isPresent()) {
            events.publishEvent(
                    new QuizAttemptChangedEvent(user.id(), userEmail, quiz.isBlockMessaging()));
            return mapper.toStudentView(quiz, existingAttempt.get().getId());
        }

        if (!staff) {
            requireWithinWindow(quiz);
        }

        checkAccessPassword(quiz, user, providedPassword);

        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            int existingAttemptsCount =
                    quizAttemptRepository.countByQuizIdAndUserId(quizId, user.id());
            if (existingAttemptsCount >= quiz.getMaxAttempts()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "You have reached the maximum number of attempts ("
                                + quiz.getMaxAttempts()
                                + ") for this quiz.");
            }
        }

        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setUserId(user.id());
        attempt.setQuiz(quiz);
        attempt.setStartedAt(Instant.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setScore(BigDecimal.ZERO);

        attemptRepository.save(attempt);
        events.publishEvent(
                new QuizAttemptChangedEvent(user.id(), userEmail, quiz.isBlockMessaging()));

        return mapper.toStudentView(quiz, attempt.getId());
    }

    /**
     * Draft and unpublished quizzes, and quizzes of courses the student does not belong to, look
     * exactly like quizzes that do not exist, so ids cannot be probed.
     */
    private void requireStudentMayOpen(QuizEntity quiz, UserSummary student) {
        if (!quiz.isPublished() || !membership.isMember(quiz.getCourseId(), student)) {
            throw new NotFoundException("Quiz not found");
        }
        Set<Long> assigned = quiz.getClassIds();
        if (assigned != null && !assigned.isEmpty()) {
            Long classId = student.classId();
            boolean assignedToMe = classId != null && assigned.contains(classId);
            if (!assignedToMe) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "This quiz is not assigned to your class.");
            }
        }
    }

    /**
     * The quiz form sends its window as UTC ({@code toISOString()}) and Jackson keeps that wall
     * time in the LocalDateTime, so "now" must be UTC as well, whatever the server's zone.
     */
    private static void requireWithinWindow(QuizEntity quiz) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (quiz.getAvailableFrom() != null && now.isBefore(quiz.getAvailableFrom())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This quiz is not open yet. It opens at "
                            + WINDOW_FORMAT.format(quiz.getAvailableFrom())
                            + " UTC.");
        }
        if (quiz.getAvailableTo() != null && now.isAfter(quiz.getAvailableTo())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This quiz closed at " + WINDOW_FORMAT.format(quiz.getAvailableTo()) + " UTC.");
        }
    }

    /**
     * Quiz passwords are short and shared by a whole class, so wrong guesses are limited per
     * student and quiz; after {@link #MAX_PASSWORD_FAILURES} the student waits out the window.
     */
    private void checkAccessPassword(QuizEntity quiz, UserSummary user, String providedPassword) {
        String dbPassword = quiz.getPassword();
        if (dbPassword == null || dbPassword.isBlank()) {
            return;
        }
        if (providedPassword == null || providedPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is required");
        }
        String key = user.id() + ":" + quiz.getId();
        if (passwordFailures.isExhausted(key)) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Try again in 15 minutes.");
        }
        if (!passwordEncoder.matches(providedPassword.trim(), dbPassword)) {
            passwordFailures.record(key);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid quiz password");
        }
        passwordFailures.reset(key);
    }

    /**
     * Grades an attempt. Only the first answer per question counts: a client sending one answer per
     * option used to collect every correct option's points, so extra entries are ignored, the list
     * may not be longer than the quiz, and the total can never exceed the maximum.
     *
     * <p>A quiz with free-text or code questions gets no pass/fail here: those answers score 0
     * until the teacher grades them, so the result only says that grading is still to come.
     */
    @Transactional
    public QuizResultDto submitAttempt(QuizSubmissionDto dto, String userEmail) {
        if (dto == null || dto.attemptId() == null) {
            throw new IllegalArgumentException("attemptId is required.");
        }

        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(dto.attemptId())
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        if (!ownedBy(attempt, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your attempt");
        }

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This attempt has already been submitted.");
        }

        QuizEntity quiz = attempt.getQuiz();
        List<QuizQuestionEntity> questions = quiz.getQuestions();

        if (dto.answers() == null) {
            throw new IllegalArgumentException("Answers are required.");
        }
        if (dto.answers().size() > questions.size()) {
            throw new IllegalArgumentException("There are more answers than questions.");
        }

        Map<Long, QuizQuestionEntity> questionMap = new HashMap<>();
        for (QuizQuestionEntity q : questions) {
            questionMap.put(q.getId(), q);
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore =
                questions.stream()
                        .map(QuizEngineService::pointsOf)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<Long> answered = new HashSet<>();
        for (QuizSubmissionDto.AnswerDto ans : dto.answers()) {
            if (ans == null || ans.questionId() == null) continue;
            QuizQuestionEntity question = questionMap.get(ans.questionId());
            if (question == null || !answered.add(question.getId())) continue;

            QuizResponseEntity responseEntity = new QuizResponseEntity();
            responseEntity.setAttempt(attempt);
            responseEntity.setQuestion(question);

            BigDecimal questionScore = BigDecimal.ZERO;

            if (ans.selectedOptionId() != null) {
                QuizOptionEntity selectedOption =
                        question.getOptions().stream()
                                .filter(opt -> opt.getId().equals(ans.selectedOptionId()))
                                .findFirst()
                                .orElse(null);

                responseEntity.setSelectedOption(selectedOption);
                if (selectedOption != null && selectedOption.isCorrect()) {
                    questionScore = pointsOf(question);
                }
            } else if (ans.orderedOptionIds() != null && !ans.orderedOptionIds().isEmpty()) {
                String orderStr =
                        ans.orderedOptionIds().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","));
                responseEntity.setTextResponse(orderStr);

                List<Long> correctOrderIds =
                        question.getOptions().stream()
                                .sorted(
                                        Comparator.comparingInt(
                                                o ->
                                                        o.getSortOrder() != null
                                                                ? o.getSortOrder()
                                                                : 0))
                                .map(QuizOptionEntity::getId)
                                .toList();

                if (ans.orderedOptionIds().equals(correctOrderIds)) {
                    questionScore = pointsOf(question);
                }
            } else if (ans.textAnswer() != null) {
                responseEntity.setTextResponse(ans.textAnswer());
            }

            responseEntity.setScore(questionScore);
            totalScore = totalScore.add(questionScore);
            attempt.getResponses().add(responseEntity);
        }

        if (totalScore.compareTo(maxScore) > 0) {
            totalScore = maxScore;
        }

        attempt.setCompletedAt(Instant.now());
        attempt.setStatus("COMPLETED");
        attempt.setScore(totalScore);

        attemptRepository.save(attempt);
        events.publishEvent(
                new QuizAttemptChangedEvent(
                        attempt.getUserId(), userEmail, quiz.isBlockMessaging()));

        boolean awaitingGrading = questions.stream().anyMatch(QuizEngineService::isGradedByTeacher);

        return new QuizResultDto(
                attempt.getId(),
                quiz.getTitle(),
                totalScore,
                maxScore,
                awaitingGrading ? null : isPassing(totalScore, maxScore, quiz.getPassingScore()),
                awaitingGrading,
                attempt.getCompletedAt().toString());
    }

    private static BigDecimal pointsOf(QuizQuestionEntity question) {
        return BigDecimal.valueOf(question.getPoints() != null ? question.getPoints() : 0);
    }

    /** Written answers score 0 on submission and wait for the teacher to grade them. */
    private static boolean isGradedByTeacher(QuizQuestionEntity question) {
        return TEACHER_GRADED_TYPES.contains(question.getType());
    }

    /**
     * Whether {@code score} out of {@code maxScore} reaches the pass mark, which is a percentage
     * (0-100, 50 when unset). A quiz worth no points counts as 0%.
     */
    static boolean isPassing(BigDecimal score, BigDecimal maxScore, Integer passingScore) {
        int threshold = passingScore != null ? passingScore : 50;
        if (score == null || maxScore.signum() <= 0) {
            return threshold <= 0;
        }
        // score / maxScore * 100 >= threshold, cross-multiplied so no rounding can tip it.
        return score.multiply(BigDecimal.valueOf(100))
                        .compareTo(maxScore.multiply(BigDecimal.valueOf(threshold)))
                >= 0;
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptListDto> getAttemptsForQuiz(Long quizId) {
        List<QuizAttemptEntity> attempts =
                attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId);
        Map<Long, UserSummary> users = usersOf(attempts);
        return attempts.stream()
                .map(a -> mapAttemptToListDto(a, users.get(a.getUserId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultsResponse getQuizResultsWithMetadata(Long quizId) {
        QuizEntity quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new NotFoundException("Quiz not found"));

        // Everyone expected to take it: the course's students and the quiz's own classes.
        Map<Long, UserSummary> uniqueStudentsMap = new HashMap<>();
        membership.studentsOf(quiz.getCourseId()).forEach(u -> uniqueStudentsMap.put(u.id(), u));
        directory
                .findUsersInClasses(quiz.getClassIds())
                .forEach(u -> uniqueStudentsMap.put(u.id(), u));

        List<QuizAttemptEntity> allAttempts =
                attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId);

        // Plus anyone who took it and has since left the course.
        List<Long> formerMembers =
                allAttempts.stream()
                        .map(QuizAttemptEntity::getUserId)
                        .filter(id -> !uniqueStudentsMap.containsKey(id))
                        .distinct()
                        .toList();
        directory.findUsers(formerMembers).forEach(u -> uniqueStudentsMap.put(u.id(), u));

        Map<Long, QuizAttemptEntity> userAttemptMap = new HashMap<>();
        for (QuizAttemptEntity a : allAttempts) {
            userAttemptMap.putIfAbsent(a.getUserId(), a);
        }

        List<UserSummary> sortedStudents = new ArrayList<>(uniqueStudentsMap.values());
        sortedStudents.sort(
                Comparator.comparing(UserSummary::lastName).thenComparing(UserSummary::firstName));

        List<QuizAttemptListDto> resultsList = new ArrayList<>();

        BigDecimal quizMaxScore =
                quiz.getQuestions().stream()
                        .map(q -> BigDecimal.valueOf(q.getPoints()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (UserSummary student : sortedStudents) {
            QuizAttemptEntity attempt = userAttemptMap.get(student.id());

            if (attempt != null) {
                resultsList.add(mapAttemptToListDto(attempt, student));
            } else {
                String group = (student.className() != null) ? student.className() : "No Group";

                resultsList.add(
                        new QuizAttemptListDto(
                                null,
                                student.id(),
                                student.fullName(),
                                student.email(),
                                ColorUtil.randomPastelColor(),
                                group,
                                "MISSING",
                                BigDecimal.ZERO,
                                quizMaxScore,
                                false));
            }
        }

        String dueDateStr =
                quiz.getAvailableTo() != null ? quiz.getAvailableTo().toString() : "No Deadline";

        return new QuizResultsResponse(
                quiz.getId(),
                quiz.getTitle(),
                catalog.findCourse(quiz.getCourseId()).map(CourseSummary::name).orElse(""),
                dueDateStr,
                quiz.getDurationMinutes(),
                quiz.isPublished(),
                resultsList);
    }

    /** {@code user} is {@code null} for an account that no longer exists. */
    private QuizAttemptListDto mapAttemptToListDto(QuizAttemptEntity attempt, UserSummary user) {
        String studentName = user != null ? user.fullName() : "Deleted user";
        String group = (user != null && user.className() != null) ? user.className() : "No Group";

        String submittedAt;
        if ("IN_PROGRESS".equals(attempt.getStatus())) {
            submittedAt = "In Progress";
        } else if (attempt.getCompletedAt() != null) {
            submittedAt = attempt.getCompletedAt().toString();
        } else {
            submittedAt = "Unknown";
        }

        BigDecimal maxScore =
                attempt.getQuiz().getQuestions().stream()
                        .map(q -> BigDecimal.valueOf(q.getPoints()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean passed =
                isPassing(attempt.getScore(), maxScore, attempt.getQuiz().getPassingScore());

        return new QuizAttemptListDto(
                attempt.getId(),
                attempt.getUserId(),
                studentName,
                user != null ? user.email() : "",
                ColorUtil.randomPastelColor(),
                group,
                submittedAt,
                attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO,
                maxScore,
                passed);
    }

    private Map<Long, UserSummary> usersOf(List<QuizAttemptEntity> attempts) {
        return directory
                .findUsers(attempts.stream().map(QuizAttemptEntity::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    private boolean ownedBy(QuizAttemptEntity attempt, String userEmail) {
        return attempt.getUserId() != null
                && directory
                        .findUserByEmail(userEmail)
                        .map(u -> attempt.getUserId().equals(u.id()))
                        .orElse(false);
    }

    @Transactional(readOnly = true)
    public QuizAttemptReviewDto getAttemptReview(Long attemptId) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        List<QuestionReviewDto> questions =
                attempt.getQuiz().getQuestions().stream()
                        .map(q -> mapQuestionToReviewDto(q, attempt))
                        .toList();

        long diffSeconds = 0;
        if (attempt.getStartedAt() != null && attempt.getCompletedAt() != null) {
            diffSeconds =
                    java.time.Duration.between(attempt.getStartedAt(), attempt.getCompletedAt())
                            .getSeconds();
        }
        String timeTaken = String.format("%d min %d sec", diffSeconds / 60, diffSeconds % 60);

        BigDecimal maxScore =
                attempt.getQuiz().getQuestions().stream()
                        .map(q -> BigDecimal.valueOf(q.getPoints()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        UserSummary user = directory.findUser(attempt.getUserId()).orElse(null);
        return new QuizAttemptReviewDto(
                attempt.getId(),
                user != null ? user.fullName() : "Deleted user",
                user != null && user.className() != null ? user.className() : "-",
                attempt.getQuiz().getTitle(),
                attempt.getScore(),
                maxScore,
                timeTaken,
                attempt.getCompletedAt() != null ? attempt.getCompletedAt().toString() : "N/A",
                questions);
    }

    private QuestionReviewDto mapQuestionToReviewDto(
            QuizQuestionEntity q, QuizAttemptEntity attempt) {
        QuizResponseEntity response =
                attempt.getResponses().stream()
                        .filter(r -> r.getQuestion().getId().equals(q.getId()))
                        .findFirst()
                        .orElse(null);

        Long selectedOptionId =
                (response != null && response.getSelectedOption() != null)
                        ? response.getSelectedOption().getId()
                        : null;

        String studentTextResponse = (response != null) ? response.getTextResponse() : null;

        Set<Long> multiSelectedIds = new HashSet<>();
        if (studentTextResponse != null
                && (q.getType().equals("MULTI_CHOICE") || q.getType().equals("MCQ_MULTI"))) {
            try {
                String clean = studentTextResponse.replace("[", "").replace("]", "");
                if (!clean.isBlank()) {
                    Arrays.stream(clean.split(","))
                            .map(String::trim)
                            .map(Long::parseLong)
                            .forEach(multiSelectedIds::add);
                }
            } catch (Exception e) {
            }
        }

        boolean isFullCorrect = false;
        if (response != null
                && response.getScore() != null
                && response.getScore().compareTo(BigDecimal.ZERO) > 0) {
            isFullCorrect = response.getScore().compareTo(BigDecimal.valueOf(q.getPoints())) >= 0;
        }

        List<OptionReviewDto> options =
                q.getOptions().stream()
                        .map(
                                o -> {
                                    boolean isSelected = false;
                                    if (q.getType().equals("MULTI_CHOICE")
                                            || q.getType().equals("MCQ_MULTI")) {
                                        isSelected = multiSelectedIds.contains(o.getId());
                                    } else {
                                        isSelected = o.getId().equals(selectedOptionId);
                                    }
                                    return new OptionReviewDto(
                                            o.getId(), o.getText(), isSelected, o.isCorrect());
                                })
                        .toList();

        return new QuestionReviewDto(
                q.getId(),
                q.getText(),
                q.getType(),
                (response != null && response.getScore() != null)
                        ? response.getScore()
                        : BigDecimal.ZERO,
                BigDecimal.valueOf(q.getPoints()),
                isFullCorrect,
                studentTextResponse,
                options,
                null);
    }

    @Transactional
    public void updateQuestionScore(Long attemptId, Long questionId, BigDecimal newScore) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        QuizResponseEntity response =
                attempt.getResponses().stream()
                        .filter(r -> r.getQuestion().getId().equals(questionId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                "Response for this question not found in attempt"));

        BigDecimal maxPoints = BigDecimal.valueOf(response.getQuestion().getPoints());
        if (newScore.compareTo(maxPoints) > 0) {
            throw new IllegalArgumentException(
                    "Score cannot exceed maximum points for this question (" + maxPoints + ")");
        }
        if (newScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Score cannot be negative");
        }

        response.setScore(newScore);
        recalculateAttemptScore(attempt);
        attemptRepository.save(attempt);
    }

    private void recalculateAttemptScore(QuizAttemptEntity attempt) {
        BigDecimal total = BigDecimal.ZERO;
        for (QuizResponseEntity r : attempt.getResponses()) {
            if (r.getScore() != null) {
                total = total.add(r.getScore());
            }
        }
        attempt.setScore(total);
    }
}
