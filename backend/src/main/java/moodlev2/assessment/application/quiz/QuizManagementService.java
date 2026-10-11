package moodlev2.assessment.application.quiz;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moodlev2.academic.api.CourseCatalogApi;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.ModuleSummary;
import moodlev2.assessment.infrastructure.persistence.QuestionRepository;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.infrastructure.persistence.entity.QuestionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuestionOptionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizOptionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizQuestionEntity;
import moodlev2.assessment.web.quiz.dto.CreateQuizDto;
import moodlev2.identity.api.ClassSummary;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizManagementService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final QuizRepository quizRepository;
    private final CourseCatalogApi catalog;
    private final QuestionRepository questionRepository;
    private final DirectoryApi directory;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void createQuiz(CreateQuizDto dto) {
        log.info("Creating quiz: {}", dto.title());

        CourseSummary course =
                catalog.findCourse(dto.courseId())
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        // An unknown module is ignored rather than refused, as before.
        Long moduleId =
                dto.moduleId() == null
                        ? null
                        : catalog.findModule(dto.moduleId()).map(ModuleSummary::id).orElse(null);

        QuizEntity quiz = new QuizEntity();
        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setCourseId(course.id());
        quiz.setModuleId(moduleId);

        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        quiz.setMaxAttempts(dto.maxAttempts());
        quiz.setShuffleOptions(dto.shuffleOptions());
        if (dto.password() != null && !dto.password().isBlank()) {
            quiz.setPassword(passwordEncoder.encode(dto.password()));
        }
        quiz.setAvailableFrom(dto.availableFrom());
        quiz.setAvailableTo(dto.availableTo());
        quiz.setGenerationType(dto.generationType());
        quiz.setBlockMessaging(Boolean.TRUE.equals(dto.blockMessaging()));
        quiz.setStatus("PUBLISHED");

        if (dto.assignedClassIds() != null && !dto.assignedClassIds().isEmpty()) {
            // Only classes that exist; unknown ids are dropped, as findAllById did before.
            quiz.setClassIds(
                    directory.findClasses(dto.assignedClassIds()).stream()
                            .map(ClassSummary::id)
                            .collect(Collectors.toCollection(java.util.HashSet::new)));
        }

        List<QuizQuestionEntity> questionsToAdd = new ArrayList<>();

        if ("MANUAL".equalsIgnoreCase(dto.generationType())) {
            if (dto.specificQuestionIds() != null) {
                int sortOrder = 1;
                for (Long qId : dto.specificQuestionIds()) {
                    QuestionEntity bankQ = questionRepository.findById(qId).orElse(null);
                    if (bankQ != null) {
                        questionsToAdd.add(
                                convertBankQuestionToQuizQuestion(bankQ, quiz, sortOrder++));
                    }
                }
            }
        } else if ("RANDOM".equalsIgnoreCase(dto.generationType())) {
            if (dto.randomRules() != null) {
                int sortOrder = 1;
                for (CreateQuizDto.RandomRuleDto rule : dto.randomRules()) {
                    log.info(
                            "Processing Random Rule -> Category ID: {}, Difficulty: {}, Count: {}",
                            rule.categoryId(),
                            rule.difficulty(),
                            rule.count());

                    List<QuestionEntity> candidates;

                    if (rule.categoryId() == 0) {
                        candidates = questionRepository.findAll();
                    } else {
                        candidates = questionRepository.findByCategoryId(rule.categoryId());
                    }

                    if (rule.difficulty() != null && !"ANY".equalsIgnoreCase(rule.difficulty())) {
                        candidates =
                                candidates.stream()
                                        .filter(
                                                q ->
                                                        q.getDifficulty()
                                                                .name()
                                                                .equalsIgnoreCase(
                                                                        rule.difficulty()))
                                        .collect(Collectors.toList());
                    }

                    if (candidates.isEmpty()) {
                        log.warn(
                                "No questions found for rule: Category={}, Difficulty={}",
                                rule.categoryId(),
                                rule.difficulty());
                        continue;
                    }

                    Collections.shuffle(candidates);
                    int questionsToTake = Math.min(rule.count(), candidates.size());
                    List<QuestionEntity> selected = candidates.subList(0, questionsToTake);

                    log.info(
                            "Found {} candidates, selected {} questions.",
                            candidates.size(),
                            selected.size());

                    for (QuestionEntity q : selected) {
                        questionsToAdd.add(convertBankQuestionToQuizQuestion(q, quiz, sortOrder++));
                    }
                }
            }
        }

        if (questionsToAdd.isEmpty()) {
            throw new RuntimeException(
                    "Could not create quiz: No questions selected or generated. Please check your Question Bank or Rules.");
        }

        quiz.setQuestions(questionsToAdd);
        quiz.setQuestionsCount(questionsToAdd.size());

        quizRepository.save(quiz);
        log.info("Quiz created successfully with {} questions.", questionsToAdd.size());
    }

    private QuizQuestionEntity convertBankQuestionToQuizQuestion(
            QuestionEntity bankQ, QuizEntity quiz, int order) {
        QuizQuestionEntity qq = new QuizQuestionEntity();
        qq.setQuiz(quiz);
        qq.setText(bankQ.getText());
        qq.setType(bankQ.getType().name());
        qq.setPoints(1);
        qq.setSortOrder(order);

        // Option ids are assigned in insertion order. Inserting in authored order would make a
        // drag-and-drop question's ids spell out its answer, so insert in a random order; each
        // option keeps its sortOrder, which is what grading and display use.
        List<QuestionOptionEntity> bankOptions = new ArrayList<>(bankQ.getOptions());
        Collections.shuffle(bankOptions, RANDOM);

        for (QuestionOptionEntity bankOpt : bankOptions) {
            QuizOptionEntity qo = new QuizOptionEntity();
            qo.setText(bankOpt.getText());
            qo.setCorrect(bankOpt.isCorrect());
            qo.setSortOrder(bankOpt.getSortOrder());
            qq.addOption(qo);
        }
        return qq;
    }

    @Transactional
    public void deleteQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new NotFoundException("Quiz not found");
        }
        quizRepository.deleteById(quizId);
    }

    @Transactional
    public void updateQuiz(Long quizId, CreateQuizDto dto) {
        QuizEntity quiz =
                quizRepository
                        .findById(quizId)
                        .orElseThrow(() -> new NotFoundException("Quiz not found"));

        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        if (dto.password() != null && !dto.password().isBlank()) {
            quiz.setPassword(passwordEncoder.encode(dto.password()));
        }
        if (dto.blockMessaging() != null) {
            quiz.setBlockMessaging(dto.blockMessaging());
        }

        quizRepository.save(quiz);
    }
}
