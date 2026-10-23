package com.aagame.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aagamegames;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class GameSessionsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("DROP TABLE IF EXISTS game_sessions");
        jdbc.execute("DROP TABLE IF EXISTS game_configs");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_ledger");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_accounts");
        jdbc.execute("DROP TABLE IF EXISTS user_sessions");
        jdbc.execute("DROP TABLE IF EXISTS app_users");
        jdbc.execute("CREATE TABLE app_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'USER', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', email_verified BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_sessions (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_accounts (user_id BIGINT PRIMARY KEY, balance BIGINT NOT NULL DEFAULT 1000, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_ledger (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, delta BIGINT NOT NULL, balance_after BIGINT NOT NULL, reason VARCHAR(40) NOT NULL, reference_id VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE game_sessions (id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, game_id VARCHAR(64) NOT NULL, status VARCHAR(20) NOT NULL, score BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, finished_at TIMESTAMP NULL, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE game_configs (game_id VARCHAR(64) PRIMARY KEY, title VARCHAR(120) NOT NULL, category VARCHAR(32) NOT NULL, description VARCHAR(500) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, updated_by BIGINT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (updated_by) REFERENCES app_users(id))");
        jdbc.update("INSERT INTO game_configs(game_id,title,category,description,enabled) VALUES ('skyline-crash','Skyline Crash','ARCADE','Demo',TRUE),('neon-reels','Neon Reels','CASUAL','Demo',TRUE),('reef-ranger','Reef Ranger','ARCADE','Demo',TRUE)");
    }

    @Test
    void createsCompletesAndReadsOwnedSession() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"game@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated());

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"game@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login).get("accessToken").asText();

        String started = mvc.perform(post("/api/games/skyline-crash/sessions").header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn().getResponse().getContentAsString();
        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(started).get("sessionId").asText();

        mvc.perform(post("/api/games/sessions/" + id + "/complete").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.score").isNumber());

        mvc.perform(get("/api/games/sessions/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/games/sessions?limit=20").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sessions[0].sessionId").value(id));

        mvc.perform(post("/api/games/sessions/" + id + "/complete").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
    }

    @Test
    void rejectsDisabledGameEvenWhenIdIsKnown() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"disabled@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"disabled@example.com\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(login).get("accessToken").asText();
        jdbc.update("UPDATE game_configs SET enabled=FALSE WHERE game_id='neon-reels'");
        mvc.perform(post("/api/games/neon-reels/sessions").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownGameAndUnauthenticatedStart() throws Exception {
        mvc.perform(post("/api/games/unknown/sessions").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
    }
}
