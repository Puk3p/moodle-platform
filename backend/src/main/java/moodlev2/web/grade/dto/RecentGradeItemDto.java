package moodlev2.web.grade.dto;

/** A graded item; {@code gradedOn} is an ISO-8601 date, or empty when unknown. */
public record RecentGradeItemDto(
        String title,
        String score,
        int percent,
        String weightLabel,
        String gradedOn,
        String typeLabel,
        String typeIcon) {}
