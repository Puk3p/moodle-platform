package moodlev2.identity.web.admin.dto;

public record AdminStudentDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String className,
        Long classId,
        boolean twoFaEnabled) {}
