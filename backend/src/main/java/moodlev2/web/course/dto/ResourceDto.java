package moodlev2.web.course.dto;

public record ResourceDto(
        Long id,
        String name,
        String category,
        String type,
        String size,
        String date,
        boolean isVisible) {}
