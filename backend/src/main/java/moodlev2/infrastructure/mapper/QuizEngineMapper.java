package moodlev2.infrastructure.mapper;

import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.*;
import org.springframework.stereotype.Component;

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

        // 1. Mapare câmpuri de bază
        quiz.setTitle(dto.title());
        quiz.setDescription(dto.description());
        quiz.setCourse(course);
        quiz.setModule(module);
        quiz.setStatus("PUBLISHED"); // Sau setezi pe DRAFT implicit

        // 2. Mapare setări quiz (Câmpurile noi)
        quiz.setDurationMinutes(dto.timeLimitMinutes());
        quiz.setPassingScore(dto.passingScore());
        quiz.setMaxAttempts(dto.maxAttempts());
        quiz.setShuffleOptions(dto.shuffleOptions());
        quiz.setPassword(dto.password());
        quiz.setAvailableFrom(dto.availableFrom());
        quiz.setAvailableTo(dto.availableTo());
        quiz.setGenerationType(dto.generationType());

        // 3. Calcul număr întrebări estimat (pentru afișare rapidă)
        // Service-ul va popula lista reală de întrebări, aici doar setăm contorul inițial
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