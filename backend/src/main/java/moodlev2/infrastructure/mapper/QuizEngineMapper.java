package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuizEngineMapper {

    public StudentQuizViewDto toStudentView(QuizEntity quiz, Long attemptId) {
        return new StudentQuizViewDto(
                attemptId,
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                quiz.getQuestions().stream()
                        .map(q -> mapQuestionForStudent(q, quiz.isShuffleOptions()))
                        .toList()
        );
    }

    private StudentQuizViewDto.StudentQuestionDto mapQuestionForStudent(QuizQuestionEntity q, boolean shuffle) {
        List<StudentQuizViewDto.StudentOptionDto> optionDtos = q.getOptions().stream()
                .map(o -> new StudentQuizViewDto.StudentOptionDto(o.getId(), o.getText()))
                .collect(Collectors.toList());

        if (shuffle) {
            Collections.shuffle(optionDtos);
        }

        return new StudentQuizViewDto.StudentQuestionDto(
                q.getId(),
                q.getText(),
                q.getPoints(),
                q.getType(),
                optionDtos
        );
    }

    public QuizEntity toEntity(CreateQuizDto dto, CourseEntity course, CourseModuleEntity module) {
        QuizEntity quiz = new QuizEntity();

        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setCourse(course);
        quiz.setModule(module);
        quiz.setStatus("PUBLISHED");

        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        quiz.setMaxAttempts(dto.maxAttempts());
        quiz.setShuffleOptions(dto.shuffleOptions());
        quiz.setPassword(dto.password());
        quiz.setAvailableFrom(dto.availableFrom());
        quiz.setAvailableTo(dto.availableTo());
        quiz.setGenerationType(dto.generationType());

        int count = 0;
        if ("MANUAL".equalsIgnoreCase(dto.generationType()) && dto.specificQuestionIds() != null) {
            count = dto.specificQuestionIds().size();
        } else if ("RANDOM".equalsIgnoreCase(dto.generationType()) && dto.randomRules() != null) {
            count = dto.randomRules().stream().mapToInt(CreateQuizDto.RandomRuleDto::count).sum();
        }
        quiz.setQuestionsCount(count);

        return quiz;
    }
}