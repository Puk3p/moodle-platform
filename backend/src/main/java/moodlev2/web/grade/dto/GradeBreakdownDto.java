package moodlev2.web.grade.dto;

public record GradeBreakdownDto(
        int totalCourses,
        int aCourses,
        int bCourses,
        int cCourses
) {}