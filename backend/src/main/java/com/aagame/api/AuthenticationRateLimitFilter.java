package com.aagame.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Fixed-window authentication throttling; Redis is used for production multi-instance deployments. */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_KEYS = 100_000;
    private static final long CLEANUP_EVERY = 256;
    private static final DefaultRedisScript<List> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]); end; return {n,redis.call('PTTL',KEYS[1])}",
        List.class);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final int loginMaxRequests;
    private final Duration loginWindow;
    private final int registerMaxRequests;
    private final Duration registerWindow;
    private final boolean redisEnabled;
    private final StringRedisTemplate redis;

    @Autowired
    public AuthenticationRateLimitFilter(
            @Value("${app.security.rate-limit.login.max-requests:10}") int loginMaxRequests,
            @Value("${app.security.rate-limit.login.window:15m}") Duration loginWindow,
            @Value("${app.security.rate-limit.register.max-requests:5}") int registerMaxRequests,
            @Value("${app.security.rate-limit.register.window:1h}") Duration registerWindow,
            @Value("${app.security.rate-limit.redis-enabled:false}") boolean redisEnabled,
            StringRedisTemplate redis) {
        this.loginMaxRequests = Math.max(1, loginMaxRequests);
        this.loginWindow = positive(loginWindow, Duration.ofMinutes(15));
        this.registerMaxRequests = Math.max(1, registerMaxRequests);
        this.registerWindow = positive(registerWindow, Duration.ofHours(1));
        this.redisEnabled = redisEnabled;
        this.redis = redis;
    }

    // Convenient constructor for isolated unit tests that intentionally use the in-memory fallback.
    AuthenticationRateLimitFilter(int loginMaxRequests, Duration loginWindow, int registerMaxRequests, Duration registerWindow) {
        this(loginMaxRequests, loginWindow, registerMaxRequests, registerWindow, false, null);
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
        String path = request.getRequestURI();
        boolean login = "/api/auth/login".equals(path);
        int limit = login ? loginMaxRequests : registerMaxRequests;
        long windowMillis = (login ? loginWindow : registerWindow).toMillis();
        long retryAfter;

        if (redisEnabled) {
            try {
                String key = "aa-game:auth-rate:" + digest(path + "|" + request.getRemoteAddr());
                List<?> result = redis.execute(INCREMENT_WITH_TTL, List.of(key), Long.toString(windowMillis));
                if (result == null || result.size() < 2) {
                    serviceUnavailable(response);
                    return;
                }
                long count = numericValue(result.get(0));
                long ttlMillis = Math.max(1, numericValue(result.get(1)));
                if (count > limit) {
                    reject(response, Math.max(1, (ttlMillis + 999) / 1000));
                    return;
                }
            } catch (RuntimeException ex) {
                // Fail closed: never silently bypass the distributed limiter in production.
                serviceUnavailable(response);
                return;
            }
        } else {
            if (requestCounter.incrementAndGet() % CLEANUP_EVERY == 0) cleanupExpired();
            long now = System.currentTimeMillis();
            String key = path + "|" + request.getRemoteAddr();
            if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_KEYS) cleanupExpired();
            if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_KEYS) {
                reject(response, Math.max(1, windowMillis / 1000));
                return;
            }
            Window window = windows.compute(key, (ignored, current) -> {
                if (current == null || now - current.startedAtMillis >= windowMillis) return new Window(now, 1);
                current.count++;
                return current;
            });
            if (window.count > limit) {
                retryAfter = Math.max(1, (window.startedAtMillis + windowMillis - now + 999) / 1000);
                reject(response, retryAfter);
                return;
            }
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

    private static long numericValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String string) return Long.parseLong(string);
        throw new IllegalStateException("Unexpected Redis rate-limit response");
    }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many authentication attempts. Please try again later.\"}");
    }

    private static void serviceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader("Retry-After", "5");
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"RATE_LIMITER_UNAVAILABLE\",\"message\":\"Authentication is temporarily unavailable. Please retry shortly.\"}");
    }

    private static final class Window {
        private final long startedAtMillis;
        private volatile int count;
        private Window(long startedAtMillis, int count) { this.startedAtMillis = startedAtMillis; this.count = count; }
    }
}
