package moodlev2.application.quiz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.*;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.CreateQuizDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuestionRepository questionRepository;
    private final ClassRepository classRepository;

    @Transactional
    public void createQuiz(CreateQuizDto dto) {
        log.info("Creating quiz: {}", dto.title());

        CourseEntity course = courseRepository.findById(dto.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found"));

        CourseModuleEntity module = null;
        if (dto.moduleId() != null) {
            module = moduleRepository.findById(dto.moduleId()).orElse(null);
        }

        QuizEntity quiz = new QuizEntity();
        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setCourse(course);
        quiz.setModule(module);

        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        quiz.setMaxAttempts(dto.maxAttempts());
        quiz.setShuffleOptions(dto.shuffleOptions());
        quiz.setPassword(dto.password());
        quiz.setAvailableFrom(dto.availableFrom());
        quiz.setAvailableTo(dto.availableTo());
        quiz.setGenerationType(dto.generationType());
        quiz.setStatus("PUBLISHED");

        if (dto.assignedClassIds() != null && !dto.assignedClassIds().isEmpty()) {
            List<ClassEntity> classes = classRepository.findAllById(dto.assignedClassIds());
            quiz.setAssignedClasses(classes);
        }

        List<QuizQuestionEntity> questionsToAdd = new ArrayList<>();

        if ("MANUAL".equalsIgnoreCase(dto.generationType())) {
            if (dto.specificQuestionIds() != null) {
                int sortOrder = 1;
                for (Long qId : dto.specificQuestionIds()) {
                    QuestionEntity bankQ = questionRepository.findById(qId).orElse(null);
                    if (bankQ != null) {
                        questionsToAdd.add(convertBankQuestionToQuizQuestion(bankQ, quiz, sortOrder++));
                    }
                }
            }
        }
        else if ("RANDOM".equalsIgnoreCase(dto.generationType())) {
            if (dto.randomRules() != null) {
                int sortOrder = 1;
                for (CreateQuizDto.RandomRuleDto rule : dto.randomRules()) {
                    log.info("Processing Random Rule -> Category ID: {}, Difficulty: {}, Count: {}",
                            rule.categoryId(), rule.difficulty(), rule.count());

                    List<QuestionEntity> candidates;

                    if (rule.categoryId() == 0) {
                        candidates = questionRepository.findAll();
                    } else {
                        candidates = questionRepository.findByCategoryId(rule.categoryId());
                    }

                    if (rule.difficulty() != null && !"ANY".equalsIgnoreCase(rule.difficulty())) {
                        candidates = candidates.stream()
                                .filter(q -> q.getDifficulty().name().equalsIgnoreCase(rule.difficulty()))
                                .collect(Collectors.toList());
                    }

                    if (candidates.isEmpty()) {
                        log.warn("No questions found for rule: Category={}, Difficulty={}", rule.categoryId(), rule.difficulty());
                        continue;
                    }

                    Collections.shuffle(candidates);
                    int questionsToTake = Math.min(rule.count(), candidates.size());
                    List<QuestionEntity> selected = candidates.subList(0, questionsToTake);

                    log.info("Found {} candidates, selected {} questions.", candidates.size(), selected.size());

                    for (QuestionEntity q : selected) {
                        questionsToAdd.add(convertBankQuestionToQuizQuestion(q, quiz, sortOrder++));
                    }
                }
            }
        }

        if (questionsToAdd.isEmpty()) {
            throw new RuntimeException("Could not create quiz: No questions selected or generated. Please check your Question Bank or Rules.");
        }

        quiz.setQuestions(questionsToAdd);
        quiz.setQuestionsCount(questionsToAdd.size());

        quizRepository.save(quiz);
        log.info("Quiz created successfully with {} questions.", questionsToAdd.size());
    }

    private QuizQuestionEntity convertBankQuestionToQuizQuestion(QuestionEntity bankQ, QuizEntity quiz, int order) {
        QuizQuestionEntity qq = new QuizQuestionEntity();
        qq.setQuiz(quiz);
        qq.setText(bankQ.getText());
        qq.setType(bankQ.getType().name());
        qq.setPoints(1);
        qq.setSortOrder(order);

        for (QuestionOptionEntity bankOpt : bankQ.getOptions()) {
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
        QuizEntity quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Quiz not found"));

        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());

        quizRepository.save(quiz);
    }
}