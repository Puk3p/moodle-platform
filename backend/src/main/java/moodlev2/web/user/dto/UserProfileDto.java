package moodlev2.web.user.dto;

public record UserProfileDto(
        String email,
        String firstName,
        String lastName,
        String className,
        String studentId,
        boolean twoFaEnabled
) {}