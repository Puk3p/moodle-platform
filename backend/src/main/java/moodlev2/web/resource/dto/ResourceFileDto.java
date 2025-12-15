package moodlev2.web.resource.dto;

public record ResourceFileDto(
        String id,
        String title,
        String sizeLabel,
        String type
) {}