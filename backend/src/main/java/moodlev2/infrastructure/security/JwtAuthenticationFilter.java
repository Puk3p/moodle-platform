package moodlev2.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenServicePort tokenServicePort;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (tokenServicePort.isValid(token)) {
                TokenServicePort.TokenPayload payload = tokenServicePort.parse(token);

                Set<SimpleGrantedAuthority> authorities =
                        payload.roles().stream()
                                .map(Role::name)
                                .map(roleName -> "ROLE_" + roleName)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toSet());

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(payload.email(), null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
