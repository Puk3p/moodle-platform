package moodlev2.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * CSRF for a single-page app, following the Spring Security reference: the token lives in a
 * readable {@code XSRF-TOKEN} cookie, the SPA echoes it in the {@code X-XSRF-TOKEN} header, and the
 * token is loaded on every request so the cookie is always present for the next one. An attacker's
 * page can neither read the cookie (same-origin policy) nor, with SameSite=Strict, make the browser
 * send the session cookie at all.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {
        // BREACH protection for any token rendered into a response body.
        xor.handle(request, response, csrfToken);
        // Load the deferred token so the cookie is written on this response.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String header = request.getHeader(csrfToken.getHeaderName());
        // A header value is the raw cookie value echoed by the SPA; anything else is XOR-encoded.
        return (StringUtils.hasText(header) ? plain : xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
