package moodlev2.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The authentication cookies. All are:
 *
 * <ul>
 *   <li>HttpOnly: page JavaScript, including any injected script, cannot read them;
 *   <li>Secure: never sent over plain HTTP;
 *   <li>SameSite=Strict: not attached to requests started by other sites, which blocks CSRF and
 *       cross-site WebSocket hijacking at the browser level;
 *   <li>{@code __Host-} prefixed: the browser only accepts them from this exact host over HTTPS
 *       with Path=/, so a sibling subdomain cannot plant or overwrite them.
 * </ul>
 *
 * <p>{@code app.session.cookie-secure=false} exists only for local HTTP development in browsers
 * that refuse Secure cookies on http://localhost; the prefix is dropped then, since browsers reject
 * {@code __Host-} cookies without Secure.
 */
@Component
public class SessionCookies {

    /** Pre-2FA challenges are short-lived. */
    public static final Duration CHALLENGE_LIFETIME = Duration.ofMinutes(5);

    private final boolean secure;
    private final String sessionName;
    private final String challengeName;

    public SessionCookies(@Value("${app.session.cookie-secure:true}") boolean secure) {
        this.secure = secure;
        String prefix = secure ? "__Host-" : "";
        this.sessionName = prefix + "mdl_session";
        this.challengeName = prefix + "mdl_2fa";
    }

    public boolean isSecure() {
        return secure;
    }

    public String sessionName() {
        return sessionName;
    }

    public void setSession(HttpServletResponse response, String token, Duration maxAge) {
        add(response, build(sessionName, token, maxAge));
    }

    public void clearSession(HttpServletResponse response) {
        add(response, build(sessionName, "", Duration.ZERO));
    }

    public void setChallenge(HttpServletResponse response, String token) {
        add(response, build(challengeName, token, CHALLENGE_LIFETIME));
    }

    public void clearChallenge(HttpServletResponse response) {
        add(response, build(challengeName, "", Duration.ZERO));
    }

    public Optional<String> readSession(HttpServletRequest request) {
        return read(request, sessionName);
    }

    public Optional<String> readChallenge(HttpServletRequest request) {
        return read(request, challengeName);
    }

    /** For the WebSocket handshake, which only exposes raw headers. */
    public Optional<String> readSessionFromHeader(String cookieHeader) {
        if (cookieHeader == null) {
            return Optional.empty();
        }
        for (String part : cookieHeader.split(";")) {
            String p = part.trim();
            int eq = p.indexOf('=');
            if (eq > 0 && p.substring(0, eq).equals(sessionName)) {
                String value = p.substring(eq + 1);
                return value.isEmpty() ? Optional.empty() : Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isEmpty()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }
}
