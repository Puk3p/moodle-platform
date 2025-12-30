package moodlev2.web.admin.dto;

public record UpdateStudentRequest(
        String firstName,
        String lastName,
        String email,
        Long classId
) {}