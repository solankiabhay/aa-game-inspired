package com.aagame.api;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationRateLimitFilterTest {
    @Test
    void limitsLoginAttemptsAndReturnsRetryAfter() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(
            2, Duration.ofMinutes(1), 3, Duration.ofMinutes(5));

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = invoke(filter, "/api/auth/login", "192.0.2.10");
            assertEquals(200, response.getStatus());
        }

        MockHttpServletResponse limited = invoke(filter, "/api/auth/login", "192.0.2.10");
        assertEquals(429, limited.getStatus());
        assertEquals("RATE_LIMITED", new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(limited.getContentAsString()).get("code").asText());
        org.junit.jupiter.api.Assertions.assertNotNull(limited.getHeader("Retry-After"));
    }

    @Test
    void separatesClientsAndDoesNotThrottleUnrelatedEndpoints() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(
            1, Duration.ofMinutes(1), 1, Duration.ofMinutes(5));

        assertEquals(200, invoke(filter, "/api/auth/login", "192.0.2.11").getStatus());
        assertEquals(200, invoke(filter, "/api/auth/login", "192.0.2.12").getStatus());
        assertEquals(200, invoke(filter, "/api/auth/me", "192.0.2.11").getStatus());
        assertEquals(429, invoke(filter, "/api/auth/login", "192.0.2.11").getStatus());
    }

    @Test
    void independentlyLimitsRegistrationAttempts() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(
            5, Duration.ofMinutes(1), 1, Duration.ofHours(1));

        assertEquals(200, invoke(filter, "/api/auth/register", "198.51.100.20").getStatus());
        MockHttpServletResponse limited = invoke(filter, "/api/auth/register", "198.51.100.20");
        assertEquals(429, limited.getStatus());
        assertEquals("application/json", limited.getContentType());
    }

    @Test
    void failsClosedWhenDistributedLimiterIsEnabledButUnavailable() throws Exception {
        AuthenticationRateLimitFilter filter = new AuthenticationRateLimitFilter(
            10, Duration.ofMinutes(15), 5, Duration.ofHours(1), true, (org.springframework.data.redis.core.StringRedisTemplate) null);
        MockHttpServletResponse response = invoke(filter, "/api/auth/login", "192.0.2.99");
        assertEquals(503, response.getStatus());
        assertEquals("RATE_LIMITER_UNAVAILABLE", new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsString()).get("code").asText());
    }

    private static MockHttpServletResponse invoke(AuthenticationRateLimitFilter filter, String path, String ip)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
