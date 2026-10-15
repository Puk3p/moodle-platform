package moodlev2.assessment.web.quiz.dto;

import java.util.List;

public record QuestionDto(String text, String type, Integer points, List<OptionDto> options) {}
