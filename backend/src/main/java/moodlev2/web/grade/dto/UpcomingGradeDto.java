package moodlev2.web.grade.dto;

/** A graded piece of work coming up in an enrolled course; {@code date} is ISO-8601. */
public record UpcomingGradeDto(String courseCode, String title, String date, String type) {}
