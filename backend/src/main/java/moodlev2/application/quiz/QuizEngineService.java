package moodlev2.application.quiz;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.common.util.ColorUtil;
import moodlev2.infrastructure.mapper.QuizEngineMapper;
import moodlev2.infrastructure.persistence.jpa.*;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizEngineService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final SpringDataUserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizEngineMapper mapper;

    @Transactional
    public StudentQuizViewDto startAttempt(Long quizId, String userEmail, String providedPassword) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        var existingAttempt = quizAttemptRepository.findByQuizIdAndUserIdAndStatus(quizId, user.getId(), "IN_PROGRESS");
        if (existingAttempt.isPresent()) {
            return mapper.toStudentView(quiz, existingAttempt.get().getId());
        }

        String dbPassword = quiz.getPassword();
        if (dbPassword != null && !dbPassword.isBlank()) {
            if (providedPassword == null || providedPassword.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is required");
            }
            if (!dbPassword.trim().equals(providedPassword.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid quiz password");
            }
        }

        if (quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            int existingAttemptsCount = quizAttemptRepository.countByQuizIdAndUserId(quizId, user.getId());
            if (existingAttemptsCount >= quiz.getMaxAttempts()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You have reached the maximum number of attempts (" + quiz.getMaxAttempts() + ") for this quiz.");
            }
        }

        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setStartedAt(Instant.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setScore(BigDecimal.ZERO);

        attemptRepository.save(attempt);

        return mapper.toStudentView(quiz, attempt.getId());
    }

    @Transactional
    public QuizResultDto submitAttempt(QuizSubmissionDto dto, String userEmail) {
        QuizAttemptEntity attempt = attemptRepository.findById(dto.attemptId())
                .orElseThrow(() -> new NotFoundException("Attempt not found"));

        if (!attempt.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized submission");
        }

        if ("COMPLETED".equals(attempt.getStatus())) {
            throw new RuntimeException("Attempt already submitted");
        }

        QuizEntity quiz = attempt.getQuiz();

        Map<Long, QuizQuestionEntity> questionMap = quiz.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestionEntity::getId, Function.identity()));

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = quiz.getQuestions().stream()
                .map(q -> BigDecimal.valueOf(q.getPoints()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (QuizSubmissionDto.AnswerDto ans : dto.answers()) {
            QuizQuestionEntity question = questionMap.get(ans.questionId());
            if (question == null) continue;

            QuizResponseEntity responseEntity = new QuizResponseEntity();
            responseEntity.setAttempt(attempt);
            responseEntity.setQuestion(question);

            BigDecimal questionScore = BigDecimal.ZERO;

            if (ans.selectedOptionId() != null) {
                QuizOptionEntity selectedOption = question.getOptions().stream()
                        .filter(opt -> opt.getId().equals(ans.selectedOptionId()))
                        .findFirst()
                        .orElse(null);

                responseEntity.setSelectedOption(selectedOption);
                if (selectedOption != null && selectedOption.isCorrect()) {
                    questionScore = BigDecimal.valueOf(question.getPoints());
                }
            }
            else if (ans.orderedOptionIds() != null && !ans.orderedOptionIds().isEmpty()) {
                String orderStr = ans.orderedOptionIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                responseEntity.setTextResponse(orderStr);

                List<Long> correctOrderIds = question.getOptions().stream()
                        .sorted(Comparator.comparingInt(o -> o.getSortOrder() != null ? o.getSortOrder() : 0))
                        .map(QuizOptionEntity::getId)
                        .toList();

                if (ans.orderedOptionIds().equals(correctOrderIds)) {
                    questionScore = BigDecimal.valueOf(question.getPoints());
                }
            }
            else if (ans.textAnswer() != null) {
                responseEntity.setTextResponse(ans.textAnswer());
            }

            responseEntity.setScore(questionScore);
            totalScore = totalScore.add(questionScore);
            attempt.getResponses().add(responseEntity);
        }

        attempt.setCompletedAt(Instant.now());
        attempt.setStatus("COMPLETED");
        attempt.setScore(totalScore);

        attemptRepository.save(attempt);

        boolean passed = totalScore.intValue() >= (quiz.getPassingScore() != null ? quiz.getPassingScore() : 50);

        return new QuizResultDto(
                attempt.getId(),
                quiz.getTitle(),
                totalScore,
                maxScore,
                passed,
                attempt.getCompletedAt().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<QuizAttemptListDto> getAttemptsForQuiz(Long quizId) {
        return attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId).stream()
                .map(this::mapAttemptToListDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuizResultsResponse getQuizResultsWithMetadata(Long quizId) {
        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        String courseCode = quiz.getCourse().getCode();

        Map<Long, UserEntity> uniqueStudentsMap = new HashMap<>();

        enrollmentRepository.findAllByCourseCode(courseCode)
                .forEach(e -> uniqueStudentsMap.put(e.getUser().getId(), e.getUser()));

        if (quiz.getCourse().getAssignedClasses() != null) {
            for (ClassEntity clazz : quiz.getCourse().getAssignedClasses()) {
                userRepository.findAll().stream()
                        .filter(u -> u.getClazz() != null && u.getClazz().getId().equals(clazz.getId()))
                        .forEach(u -> uniqueStudentsMap.put(u.getId(), u));
            }
        }

        if (quiz.getAssignedClasses() != null) {
            for (ClassEntity clazz : quiz.getAssignedClasses()) {
                userRepository.findAll().stream()
                        .filter(u -> u.getClazz() != null && u.getClazz().getId().equals(clazz.getId()))
                        .forEach(u -> uniqueStudentsMap.put(u.getId(), u));
            }
        }

        List<QuizAttemptEntity> allAttempts = attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId);

        Map<Long, QuizAttemptEntity> userAttemptMap = new HashMap<>();

        for (QuizAttemptEntity a : allAttempts) {
            if (!uniqueStudentsMap.containsKey(a.getUser().getId())) {
                uniqueStudentsMap.put(a.getUser().getId(), a.getUser());
            }

            if (!userAttemptMap.containsKey(a.getUser().getId())) {
                userAttemptMap.put(a.getUser().getId(), a);
            }
        }

        List<UserEntity> sortedStudents = new ArrayList<>(uniqueStudentsMap.values());
        sortedStudents.sort(Comparator.comparing(UserEntity::getLastName).thenComparing(UserEntity::getFirstName));

        List<QuizAttemptListDto> resultsList = new ArrayList<>();

        BigDecimal quizMaxScore = quiz.getQuestions().stream()
                .map(q -> BigDecimal.valueOf(q.getPoints()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (UserEntity student : sortedStudents) {
            QuizAttemptEntity attempt = userAttemptMap.get(student.getId());

            if (attempt != null) {
                resultsList.add(mapAttemptToListDto(attempt));
            } else {
                String group = (student.getClazz() != null) ? student.getClazz().getName() : "No Group";
                String studentName = student.getFirstName() + " " + student.getLastName();

                resultsList.add(new QuizAttemptListDto(
                        null,
                        student.getId(),
                        studentName,
                        student.getEmail(),
                        ColorUtil.randomPastelColor(),
                        group,
                        "MISSING",
                        BigDecimal.ZERO,
                        quizMaxScore,
                        false
                ));
            }
        }

        String dueDateStr = quiz.getAvailableTo() != null ? quiz.getAvailableTo().toString() : "No Deadline";

        return new QuizResultsResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCourse().getName(),
                dueDateStr,
                quiz.getDurationMinutes(),
                quiz.isPublished(),
                resultsList
        );
    }

    private QuizAttemptListDto mapAttemptToListDto(QuizAttemptEntity attempt) {
        String studentName = attempt.getUser().getFirstName() + " " + attempt.getUser().getLastName();
        String group = (attempt.getUser().getClazz() != null) ? attempt.getUser().getClazz().getName() : "No Group";

        String submittedAt;
        if ("IN_PROGRESS".equals(attempt.getStatus())) {
            submittedAt = "In Progress";
        } else if (attempt.getCompletedAt() != null) {
            submittedAt = attempt.getCompletedAt().toString();
        } else {
            submittedAt = "Unknown";
        }

        BigDecimal maxScore = attempt.getQuiz().getQuestions().stream()
                .map(q -> BigDecimal.valueOf(q.getPoints()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double percentage = 0.0;
        if (maxScore.compareTo(BigDecimal.ZERO) > 0 && attempt.getScore() != null) {
            percentage = (attempt.getScore().doubleValue() / maxScore.doubleValue()) * 100.0;
        }

        int passingScoreThreshold = attempt.getQuiz().getPassingScore() != null
                ? attempt.getQuiz().getPassingScore()
                : 50;

        boolean passed = percentage >= passingScoreThreshold;

        return new QuizAttemptListDto(
                attempt.getId(),
                attempt.getUser().getId(),
                studentName,
                attempt.getUser().getEmail(),
                ColorUtil.randomPastelColor(),
                group,
                submittedAt,
                attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO,
                maxScore,
                passed
        );
    }

    @Transactional(readOnly = true)
    public QuizAttemptReviewDto getAttemptReview(Long attemptId) {
        QuizAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Attempt not found"));

        List<QuestionReviewDto> questions = attempt.getQuiz().getQuestions().stream()
                .map(q -> mapQuestionToReviewDto(q, attempt))
                .toList();

        long diffSeconds = 0;
        if(attempt.getStartedAt() != null && attempt.getCompletedAt() != null) {
            diffSeconds = java.time.Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).getSeconds();
        }
        String timeTaken = String.format("%d min %d sec", diffSeconds / 60, diffSeconds % 60);

        BigDecimal maxScore = attempt.getQuiz().getQuestions().stream()
                .map(q -> BigDecimal.valueOf(q.getPoints()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new QuizAttemptReviewDto(
                attempt.getId(),
                attempt.getUser().getFirstName() + " " + attempt.getUser().getLastName(),
                attempt.getUser().getClazz() != null ? attempt.getUser().getClazz().getName() : "-",
                attempt.getQuiz().getTitle(),
                attempt.getScore(),
                maxScore,
                timeTaken,
                attempt.getCompletedAt() != null ? attempt.getCompletedAt().toString() : "N/A",
                questions
        );
    }

    private QuestionReviewDto mapQuestionToReviewDto(QuizQuestionEntity q, QuizAttemptEntity attempt) {
        QuizResponseEntity response = attempt.getResponses().stream()
                .filter(r -> r.getQuestion().getId().equals(q.getId()))
                .findFirst()
                .orElse(null);

        Long selectedOptionId = (response != null && response.getSelectedOption() != null)
                ? response.getSelectedOption().getId() : null;

        String studentTextResponse = (response != null) ? response.getTextResponse() : null;

        Set<Long> multiSelectedIds = new HashSet<>();
        if (studentTextResponse != null && (q.getType().equals("MULTI_CHOICE") || q.getType().equals("MCQ_MULTI"))) {
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
        if (response != null && response.getScore() != null && response.getScore().compareTo(BigDecimal.ZERO) > 0) {
            isFullCorrect = response.getScore().compareTo(BigDecimal.valueOf(q.getPoints())) >= 0;
        }

        List<OptionReviewDto> options = q.getOptions().stream().map(o -> {
            boolean isSelected = false;
            if (q.getType().equals("MULTI_CHOICE") || q.getType().equals("MCQ_MULTI")) {
                isSelected = multiSelectedIds.contains(o.getId());
            } else {
                isSelected = o.getId().equals(selectedOptionId);
            }
            return new OptionReviewDto(
                    o.getId(),
                    o.getText(),
                    isSelected,
                    o.isCorrect()
            );
        }).toList();

        return new QuestionReviewDto(
                q.getId(),
                q.getText(),
                q.getType(),
                (response != null && response.getScore() != null) ? response.getScore() : BigDecimal.ZERO,
                BigDecimal.valueOf(q.getPoints()),
                isFullCorrect,
                studentTextResponse,
                options,
                null
        );
    }

    @Transactional
    public void updateQuestionScore(Long attemptId, Long questionId, BigDecimal newScore) {
        QuizAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Attempt not found"));

        QuizResponseEntity response = attempt.getResponses().stream()
                .filter(r -> r.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Response for this question not found in attempt"));

        BigDecimal maxPoints = BigDecimal.valueOf(response.getQuestion().getPoints());
        if (newScore.compareTo(maxPoints) > 0) {
            throw new IllegalArgumentException("Score cannot exceed maximum points for this question (" + maxPoints + ")");
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