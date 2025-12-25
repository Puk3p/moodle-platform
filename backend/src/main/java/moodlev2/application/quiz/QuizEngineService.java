package moodlev2.application.quiz;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.QuizEngineMapper;
import moodlev2.infrastructure.persistence.jpa.*;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.QuizResultDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto;
import moodlev2.web.quiz.dto.StudentQuizViewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    public StudentQuizViewDto startAttempt(Long quizId, String userEmail) {
        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setUser(user);
        attempt.setQuiz(quiz);
        attempt.setStartedAt(Instant.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setScore(BigDecimal.ZERO);

        attemptRepository.save(attempt);

        return mapper.toStudentView(quiz);
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

            QuizOptionEntity selectedOption = question.getOptions().stream()
                    .filter(opt -> opt.getId().equals(ans.selectedOptionId()))
                    .findFirst()
                    .orElse(null);

            QuizResponseEntity responseEntity = new QuizResponseEntity();
            responseEntity.setAttempt(attempt);
            responseEntity.setQuestion(question);
            responseEntity.setSelectedOption(selectedOption);
            attempt.getResponses().add(responseEntity);

            if (selectedOption != null && selectedOption.isCorrect()) {
                totalScore = totalScore.add(BigDecimal.valueOf(question.getPoints()));
            }
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
}