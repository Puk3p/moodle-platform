package moodlev2.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight, dependency-free per-client rate limiter for the public authentication endpoints. It
 * protects against brute-force and credential-stuffing attacks on login, registration, password
 * reset and 2FA verification. A fixed-window counter keyed by client IP is sufficient for a
 * single-instance deployment sized for a few hundred users; if the platform is ever scaled out
 * behind several instances this should be backed by a shared store (e.g. Redis / Bucket4j).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 10;

    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    private static final class Window {
        private volatile long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.windowStart = start;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Only throttle the sensitive, unauthenticated auth surface.
        return path == null || !path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long now = System.currentTimeMillis();
        String key = clientKey(request);

        Window window = counters.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.windowStart >= WINDOW_MILLIS) {
                window.windowStart = now;
                window.count.set(0);
            }
        }

        int current = window.count.incrementAndGet();
        if (current > MAX_REQUESTS_PER_WINDOW) {
            log.warn("Rate limit exceeded for {} on {}", key, request.getServletPath());
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        // Opportunistic cleanup to keep the map bounded on a long-running instance.
        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(e -> now - e.getValue().windowStart >= WINDOW_MILLIS * 5);
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
