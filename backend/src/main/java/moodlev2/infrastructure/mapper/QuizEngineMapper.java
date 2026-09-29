package moodlev2.infrastructure.mapper;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.quiz.dto.*;
import org.springframework.stereotype.Component;

@Component
public class QuizEngineMapper {

    /** Shuffles decide what an examinee sees, so they should not be predictable. */
    private static final SecureRandom RANDOM = new SecureRandom();

    public StudentQuizViewDto toStudentView(QuizEntity quiz, Long attemptId) {
        return new StudentQuizViewDto(
                attemptId,
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                quiz.getQuestions().stream()
                        .map(q -> mapQuestionForStudent(q, quiz.isShuffleOptions()))
                        .toList(),
                quiz.isBlockMessaging());
    }

    private StudentQuizViewDto.StudentQuestionDto mapQuestionForStudent(
            QuizQuestionEntity q, boolean shuffle) {
        List<StudentQuizViewDto.StudentOptionDto> optionDtos =
                q.getOptions().stream()
                        .map(o -> new StudentQuizViewDto.StudentOptionDto(o.getId(), o.getText()))
                        .collect(Collectors.toList());

        // Drag-and-drop options are stored in answer order (sortOrder is the answer), so showing
        // them unshuffled would hand the student the solution. They are always shuffled; the
        // teacher's shuffleOptions switch only governs the other question types.
        if (shuffle || "DRAG_DROP".equalsIgnoreCase(q.getType())) {
            Collections.shuffle(optionDtos, RANDOM);
        }

        return new StudentQuizViewDto.StudentQuestionDto(
                q.getId(), q.getText(), q.getPoints(), q.getType(), optionDtos);
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
