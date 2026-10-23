package com.aagame.api;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Best-effort per-instance throttling for public authentication endpoints.
 * Use a shared gateway or distributed limiter for multi-instance deployments.
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_KEYS = 100_000;
    private static final long CLEANUP_EVERY = 256;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final int loginMaxRequests;
    private final Duration loginWindow;
    private final int registerMaxRequests;
    private final Duration registerWindow;

    public AuthenticationRateLimitFilter(
            @Value("${app.security.rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${app.security.rate-limit.login.window:15m}") Duration loginWindow,
            @Value("${app.security.rate-limit.register.max-requests:5}") int registerMaxRequests,
            @Value("${app.security.rate-limit.register.window:1h}") Duration registerWindow) {
        this.loginMaxRequests = Math.max(1, loginMaxRequests);
        this.loginWindow = positive(loginWindow, Duration.ofMinutes(15));
        this.registerMaxRequests = Math.max(1, registerMaxRequests);
        this.registerWindow = positive(registerWindow, Duration.ofHours(1));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return !"/api/auth/login".equals(path) && !"/api/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (requestCounter.incrementAndGet() % CLEANUP_EVERY == 0) cleanupExpired();

        String path = request.getRequestURI();
        int limit = "/api/auth/login".equals(path) ? loginMaxRequests : registerMaxRequests;
        long windowMillis = ("/api/auth/login".equals(path) ? loginWindow : registerWindow).toMillis();
        long now = System.currentTimeMillis();

        // remoteAddr is deliberately used instead of untrusted X-Forwarded-For.
        String key = path + "|" + request.getRemoteAddr();
        if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_KEYS) cleanupExpired();
        if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_KEYS) {
            reject(response, Math.max(1, windowMillis / 1000));
            return;
        }

        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtMillis >= windowMillis) {
                return new Window(now, 1);
            }
            current.count++;
            return current;
        });
        if (window.count > limit) {
            long retryAfter = Math.max(1, (window.startedAtMillis + windowMillis - now + 999) / 1000);
            reject(response, retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> {
            String path = entry.getKey().substring(0, entry.getKey().indexOf('|'));
            long ttl = "/api/auth/login".equals(path) ? loginWindow.toMillis() : registerWindow.toMillis();
            return now - entry.getValue().startedAtMillis >= ttl;
        });
    }

    private static Duration positive(Duration candidate, Duration fallback) {
        return candidate == null || candidate.isZero() || candidate.isNegative() ? fallback : candidate;
    }

    private static void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many authentication attempts. Please try again later.\"}");
    }

    private static final class Window {
        private final long startedAtMillis;
        private volatile int count;

        private Window(long startedAtMillis, int count) {
            this.startedAtMillis = startedAtMillis;
            this.count = count;
        }
    }
}
