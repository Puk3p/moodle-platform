package moodlev2.web.questionbank.dto;

public record CreateCategoryRequest(
        String name,
        Long parentId
) {}