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

    /**
     * Only the endpoints that check a secret. Session status and logout are called on every page
     * load and must not eat into the budget meant for password guessing.
     */
    private static final java.util.Set<String> GUARDED_PATHS =
            java.util.Set.of(
                    "/api/auth/login",
                    "/api/auth/login/verify-2fa",
                    "/api/auth/register",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/auth/2fa/setup",
                    "/api/auth/2fa/verify");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod())
                && GUARDED_PATHS.contains(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long now = System.currentTimeMillis();
        // One bucket per client per endpoint, so a burst of password-reset requests does not lock
        // the same person (or a whole school behind one NAT address) out of signing in.
        String key = clientKey(request) + " " + request.getServletPath();

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

    /**
     * The client address as resolved by the servlet container. X-Forwarded-For is deliberately not
     * read here: its left-most entry is whatever the client sent, so trusting it let anyone reset
     * their rate limit by inventing a new address per request. With {@code
     * server.forward-headers-strategy=native} Tomcat's RemoteIpValve walks the header from the
     * right, skipping only trusted proxies (nginx on localhost), and exposes the real client here.
     */
    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
