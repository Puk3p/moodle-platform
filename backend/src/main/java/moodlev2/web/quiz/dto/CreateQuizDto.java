package moodlev2.web.quiz.dto;

import java.util.List;

public record CreateQuizDto(
        String title,
        String description,
        Long courseId,
        Long moduleId, // Optional, daca vrei sa il legi de un modul
        Integer timeLimitMinutes,
        Integer passingScore,
        List<QuestionDto> questions
) {}