package moodlev2.web.admin.dto;

public record AdminStudentDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String className,
        Long classId,
        boolean twoFaEnabled) {}
