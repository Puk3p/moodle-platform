package moodlev2.application.quiz;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.QuizEngineMapper;
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
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuizEngineMapper mapper;
    private final QuestionRepository questionRepository;
    private final ClassRepository classRepository;
    @Transactional
    public void createQuiz(CreateQuizDto dto) {
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
        } else if ("RANDOM".equalsIgnoreCase(dto.generationType())) {
            if (dto.randomRules() != null) {
                int sortOrder = 1;
                for (CreateQuizDto.RandomRuleDto rule : dto.randomRules()) {
                    List<QuestionEntity> candidates;

                    if ("ANY".equalsIgnoreCase(rule.difficulty())) {
                        candidates = questionRepository.findByCategoryId(rule.categoryId());
                    } else {
                        candidates = questionRepository.findByCategoryId(rule.categoryId()).stream()
                                .filter(q -> q.getDifficulty().name().equalsIgnoreCase(rule.difficulty()))
                                .collect(Collectors.toList());
                    }

                    Collections.shuffle(candidates);
                    List<QuestionEntity> selected = candidates.stream().limit(rule.count()).toList();

                    for (QuestionEntity q : selected) {
                        questionsToAdd.add(convertBankQuestionToQuizQuestion(q, quiz, sortOrder++));
                    }
                }
            }
        }

        quiz.setQuestions(questionsToAdd);
        quiz.setQuestionsCount(questionsToAdd.size());

        quizRepository.save(quiz);
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