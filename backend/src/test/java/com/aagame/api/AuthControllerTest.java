package com.aagame.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aagameauth;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_ledger");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_accounts");
        jdbc.execute("DROP TABLE IF EXISTS user_sessions");
        jdbc.execute("DROP TABLE IF EXISTS app_users");
        jdbc.execute("CREATE TABLE app_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'USER', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', email_verified BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_sessions (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_accounts (user_id BIGINT PRIMARY KEY, balance BIGINT NOT NULL DEFAULT 1000, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_ledger (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, delta BIGINT NOT NULL, balance_after BIGINT NOT NULL, reason VARCHAR(40) NOT NULL, reference_id VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
    }

    @Test
    void registrationLoginProfileCreditsAndLogoutFlow() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Player@Example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("player@example.com"))
            .andExpect(jsonPath("$.startingDemoCredits").value(1000));

        String loginBody = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"player@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn().getResponse().getContentAsString();

        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(loginBody).get("accessToken").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("player@example.com"))
            .andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(get("/api/auth/me/demo-credits").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1000))
            .andExpect(jsonPath("$.redeemable").value(false));

        mvc.perform(get("/api/auth/me/ledger").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.entries[0].reason").value("WELCOME_CREDITS"));

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWeakPasswordAndWrongCredentials() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad@example.com\",\"password\":\"weak\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.requestId").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\",\"password\":\"SomeLongPassword123!\"}"))
            .andExpect(status().isUnauthorized());
    }
}
