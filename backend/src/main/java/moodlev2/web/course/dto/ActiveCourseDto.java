package moodlev2.web.course.dto;

public record ActiveCourseDto(
        String code, String title, String teacher, int progress, String accent) {}
