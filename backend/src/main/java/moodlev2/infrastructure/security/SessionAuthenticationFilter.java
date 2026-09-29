package moodlev2.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.SessionService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates a request from the HttpOnly session cookie. There is deliberately no
 * Authorization-header path any more: the credential never exists anywhere JavaScript can reach.
 * Roles come from the database on every request, not from anything the client holds.
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute holding the {@link SessionService.Resolved} session, for controllers. */
    public static final String CURRENT_SESSION = "moodlev2.currentSession";

    private final SessionService sessionService;
    private final SessionCookies cookies;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Optional<String> token = cookies.readSession(request);
        if (token.isPresent()) {
            Optional<SessionService.Resolved> session =
                    sessionService.resolve(
                            token.get(),
                            request.getHeader(HttpHeaders.USER_AGENT),
                            request.getRemoteAddr());
            if (session.isPresent()) {
                var user = session.get().user();
                var authorities =
                        user.getRoles().stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                                .collect(Collectors.toSet());
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        user.getEmail(), null, authorities));
                request.setAttribute(CURRENT_SESSION, session.get());
            } else {
                // Dead, revoked or stolen cookie: tell the browser to drop it.
                cookies.clearSession(response);
            }
        }
        chain.doFilter(request, response);
    }
}
