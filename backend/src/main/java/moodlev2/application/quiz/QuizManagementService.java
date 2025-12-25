package moodlev2.application.quiz;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.QuizEngineMapper;
import moodlev2.infrastructure.persistence.jpa.*;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.CreateQuizDto;
import moodlev2.web.quiz.dto.QuestionDto;
import moodlev2.web.quiz.dto.OptionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizManagementService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuizEngineMapper mapper;

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
        quiz.setCourse(course);
        quiz.setModule(module);
        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setQuestionsCount(dto.questions().size());
        quiz.setStatus("PUBLISHED");

        List<QuizQuestionEntity> questionEntities = new ArrayList<>();
        int qOrder = 1;

        for (QuestionDto qDto : dto.questions()) {
            QuizQuestionEntity qEntity = new QuizQuestionEntity();
            qEntity.setText(qDto.text());
            qEntity.setType("SINGLE_CHOICE");
            qEntity.setPoints(qDto.points());
            qEntity.setSortOrder(qOrder++);
            qEntity.setQuiz(quiz);

            int oOrder = 1;
            for (OptionDto oDto : qDto.options()) {
                QuizOptionEntity oEntity = new QuizOptionEntity();
                oEntity.setText(oDto.text());
                oEntity.setCorrect(oDto.isCorrect());
                oEntity.setSortOrder(oOrder++);
                qEntity.addOption(oEntity);
            }
            questionEntities.add(qEntity);
        }

        quiz.setQuestions(questionEntities);
        quizRepository.save(quiz);
    }
}