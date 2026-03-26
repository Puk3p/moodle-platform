package moodlev2.web.user.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword, String twoFaCode) {}
