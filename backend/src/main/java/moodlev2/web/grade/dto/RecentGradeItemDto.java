package moodlev2.web.grade.dto;

public record RecentGradeItemDto(
        String title,
        String score,
        int percent,
        String weightLabel,
        String gradedOn,
        String typeLabel,
        String typeIcon) {}
