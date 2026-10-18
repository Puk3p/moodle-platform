package moodlev2.identity.web.user.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword, String twoFaCode) {}
