package moodlev2.identity.infrastructure.security;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.SessionAuthentication;
import moodlev2.identity.application.auth.SessionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieSessionAuthentication implements SessionAuthentication {

    private final SessionCookies cookies;
    private final SessionService sessionService;

    @Override
    public Optional<AuthenticatedSession> fromCookieHeader(
            String cookieHeader, String userAgent, String remoteAddress) {
        return cookies.readSessionFromHeader(cookieHeader)
                .flatMap(token -> sessionService.resolve(token, userAgent, remoteAddress))
                .map(
                        resolved ->
                                new AuthenticatedSession(
                                        resolved.user().getId(),
                                        resolved.user().getEmail(),
                                        resolved.tokenHash()));
    }
}
