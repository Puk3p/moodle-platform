package moodlev2.progress.web.calendar.dto;

public record CalendarEventDto(
        Long id, String date, String title, String courseCode, String type) {}
