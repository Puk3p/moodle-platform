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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizEngineService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final SpringDataUserRepository userRepository;
    private final QuizEngineMapper mapper;

    @Transactional
    public StudentQuizViewDto startAttempt(Long quizId, String userEmail, String providedPassword) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        String dbPassword = quiz.getPassword();

        if (dbPassword != null && !dbPassword.isBlank()) {
            if (providedPassword == null || providedPassword.isBlank()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password is required");
            }
            if (!dbPassword.trim().equals(providedPassword.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid quiz password");
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

            if (ans.selectedOptionId() != null) {
                QuizOptionEntity selectedOption = question.getOptions().stream()
                        .filter(opt -> opt.getId().equals(ans.selectedOptionId()))
                        .findFirst()
                        .orElse(null);

                responseEntity.setSelectedOption(selectedOption);

                if (selectedOption != null && selectedOption.isCorrect()) {
                    totalScore = totalScore.add(BigDecimal.valueOf(question.getPoints()));
                }
            }
            else if (ans.textAnswer() != null) {
                responseEntity.setTextResponse(ans.textAnswer());
            }
            else if (ans.orderedOptionIds() != null && !ans.orderedOptionIds().isEmpty()) {
                String orderStr = ans.orderedOptionIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                responseEntity.setTextResponse(orderStr);
            }

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
        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        return attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId).stream()
                .map(this::mapAttemptToListDto)
                .toList();
    }

    private QuizAttemptListDto mapAttemptToListDto(QuizAttemptEntity attempt) {
        String studentName = attempt.getUser().getFirstName() + " " + attempt.getUser().getLastName();
        String group = (attempt.getUser().getClazz() != null) ? attempt.getUser().getClazz().getName() : "No Group";

        String submittedAt = attempt.getCompletedAt() != null ? attempt.getCompletedAt().toString() : "In Progress";

        BigDecimal maxScore = attempt.getQuiz().getQuestions().stream()
                .map(q -> BigDecimal.valueOf(q.getPoints()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double percentage = 0.0;
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
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
                attempt.getScore(),
                maxScore,
                passed
        );
    }

    @Transactional(readOnly = true)
    public QuizResultsResponse getQuizResultsWithMetadata(Long quizId) {
        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        List<QuizAttemptListDto> attempts = attemptRepository.findByQuizIdOrderByCompletedAtDesc(quizId).stream()
                .map(this::mapAttemptToListDto)
                .toList();

        String dueDateStr = quiz.getAvailableTo() != null ? quiz.getAvailableTo().toString() : "No Deadline";

        return new QuizResultsResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getCourse() != null ? quiz.getCourse().getName() : "Unknown Course",
                dueDateStr,
                quiz.getDurationMinutes(),
                quiz.isPublished(),
                attempts
        );
    }


    @Transactional(readOnly = true)
    public QuizAttemptReviewDto getAttemptReview(Long attemptId) {
        QuizAttemptEntity attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Attempt not found"));

        List<QuestionReviewDto> questions = attempt.getQuiz().getQuestions().stream()
                .map(q -> mapQuestionToReviewDto(q, attempt))
                .toList();

        long diffSeconds = java.time.Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).getSeconds();
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
                attempt.getCompletedAt().toString(),
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

        boolean isFullCorrect = false;

        if (selectedOptionId != null) {
            isFullCorrect = q.getOptions().stream()
                    .anyMatch(o -> o.getId().equals(selectedOptionId) && o.isCorrect());
        } else if (studentTextResponse != null) {
            isFullCorrect = !studentTextResponse.trim().isEmpty();
        }

        List<OptionReviewDto> options = q.getOptions().stream().map(o -> new OptionReviewDto(
                o.getId(),
                o.getText(),
                o.getId().equals(selectedOptionId),
                o.isCorrect()
        )).toList();

        return new QuestionReviewDto(
                q.getId(),
                q.getText(),
                q.getType(),
                isFullCorrect ? BigDecimal.valueOf(q.getPoints()) : BigDecimal.ZERO,
                BigDecimal.valueOf(q.getPoints()),
                isFullCorrect,
                studentTextResponse,
                options,
                null
        );
    }
}