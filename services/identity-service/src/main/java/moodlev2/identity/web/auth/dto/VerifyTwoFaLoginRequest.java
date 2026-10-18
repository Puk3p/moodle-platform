package moodlev2.identity.web.auth.dto;

/** The pre-2FA challenge travels in an HttpOnly cookie, not in this body. */
public record VerifyTwoFaLoginRequest(String code) {}
