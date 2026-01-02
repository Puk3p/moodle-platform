package moodlev2.web.auth.dto;

public record VerifyTwoFaLoginRequest(
        String tempToken,
        String code
) {}