package moodlev2.identity.application.auth.interfaces;

import moodlev2.identity.domain.user.User;
import moodlev2.identity.web.auth.dto.LoginRequest;

/**
 * Checks credentials. It never issues the session itself: the web layer turns a successful result
 * into an HttpOnly session cookie, so no credential is ever handed to JavaScript.
 */
public interface ILoginService {

    LoginResult login(LoginRequest request);

    /** Second step for 2FA accounts; the challenge comes from the HttpOnly pre-2FA cookie. */
    User verifyTwoFaLogin(String challengeToken, String code);

    sealed interface LoginResult {
        record Authenticated(User user) implements LoginResult {}

        record TwoFactorRequired(String challengeToken) implements LoginResult {}
    }
}
