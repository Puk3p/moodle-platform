package moodlev2.identity.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import moodlev2.identity.web.user.ProfilePictureController;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses oversized picture uploads before the body is read, by their declared length. Multipart
 * parsing happens before any controller runs, so without this a client could make the server buffer
 * up to the multipart limit just to be told "2 MB max".
 */
@Component
public class ProfilePictureUploadLimitFilter extends OncePerRequestFilter {

    /** The 2 MB image plus room for the multipart envelope. */
    static final long MAX_REQUEST_BYTES = 2L * 1024 * 1024 + 16 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("PUT".equals(request.getMethod())
                && ProfilePictureController.PATH.equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        // Browsers always send a length for a form upload; an unknown length is refused rather
        // than trusted.
        if (length < 0 || length > MAX_REQUEST_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"The image is larger than 2 MB.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
