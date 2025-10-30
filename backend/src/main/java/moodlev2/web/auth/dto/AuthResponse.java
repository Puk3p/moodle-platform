package moodlev2.web.auth.dto;

public record AuthResponse(String accessToken, String userId, String email, String firstName, String lastName) {}

