package moodlev2.web.course.dto.students;

public record StudentDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String group,
        int progress,
        String lastActivity,
        String avatarColor) {}
