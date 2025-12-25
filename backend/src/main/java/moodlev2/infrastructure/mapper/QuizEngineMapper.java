package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class QuizEngineMapper {

    public StudentQuizViewDto toStudentView(QuizEntity quiz) {
        return new StudentQuizViewDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                quiz.getQuestions().stream().map(this::mapQuestionForStudent).toList()
        );
    }

    private StudentQuizViewDto.StudentQuestionDto mapQuestionForStudent(QuizQuestionEntity q) {
        return new StudentQuizViewDto.StudentQuestionDto(
                q.getId(),
                q.getText(),
                q.getPoints(),
                q.getOptions().stream().map(o -> new StudentQuizViewDto.StudentOptionDto(o.getId(), o.getText())).toList()
        );
    }

    public QuizEntity toEntity(CreateQuizDto dto, CourseEntity course, CourseModuleEntity module) {
        QuizEntity quiz = new QuizEntity();
        quiz.setTitle(dto.title());
        quiz.setCourse(course);
        quiz.setModule(module);
        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        quiz.setStatus("PUBLISHED");
        quiz.setMaxAttempts(1);

        if (dto.questions() != null) {
            quiz.setQuestionsCount(dto.questions().size());
        }

        return quiz;
    }
}