package com.aagame.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aagameauthrecovery;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "app.security.email-verification-enabled=true",
    "app.security.password-reset-enabled=true",
    "app.public-base-url=http://localhost:8080",
    "app.mail.from=no-reply@test.invalid"
})
@AutoConfigureMockMvc
class AuthRecoveryControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockBean JavaMailSender mailSender;

    @BeforeEach
    void schema() {
        jdbc.execute("DROP TABLE IF EXISTS auth_action_tokens");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_ledger");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_accounts");
        jdbc.execute("DROP TABLE IF EXISTS user_sessions");
        jdbc.execute("DROP TABLE IF EXISTS app_users");
        jdbc.execute("CREATE TABLE app_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'USER', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', email_verified BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_sessions (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_accounts (user_id BIGINT PRIMARY KEY, balance BIGINT NOT NULL DEFAULT 1000, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_ledger (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, delta BIGINT NOT NULL, balance_after BIGINT NOT NULL, reason VARCHAR(40) NOT NULL, reference_id VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE auth_action_tokens (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, purpose VARCHAR(24) NOT NULL, expires_at TIMESTAMP NOT NULL, used_at TIMESTAMP NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
    }

    @Test
    void emailVerificationBlocksLoginUntilOneTimeTokenIsConsumed() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"verify@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("VERIFICATION_REQUIRED"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"verify@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isUnauthorized());

        String token = lastMailToken();
        mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EMAIL_VERIFIED"));

        mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"verify@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void passwordResetChangesPasswordAndRevokesExistingSessions() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + lastMailToken() + "\"}"))
            .andExpect(status().isOk());

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String session = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login).get("accessToken").asText();

        mvc.perform(post("/api/auth/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("REQUEST_ACCEPTED"));
        String resetToken = lastMailToken();

        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"AnotherStrongPassword456!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PASSWORD_RESET"));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + session))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + resetToken + "\",\"newPassword\":\"AnotherStrongPassword456!\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"reset@example.com\",\"password\":\"AnotherStrongPassword456!\"}"))
            .andExpect(status().isOk());
    }

    private String lastMailToken() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        String text = captor.getAllValues().get(captor.getAllValues().size() - 1).getText();
        Matcher matcher = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(text);
        if (!matcher.find()) throw new AssertionError("Email did not contain a token-fragment link");
        String token = matcher.group(1);
        assertNotNull(token);
        return token;
    }
}
