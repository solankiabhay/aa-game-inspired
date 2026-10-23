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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aagameadmin;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class AdminControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void schema() {
        jdbc.execute("DROP TABLE IF EXISTS audit_events");
        jdbc.execute("DROP TABLE IF EXISTS game_sessions");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_ledger");
        jdbc.execute("DROP TABLE IF EXISTS demo_credit_accounts");
        jdbc.execute("DROP TABLE IF EXISTS user_sessions");
        jdbc.execute("DROP TABLE IF EXISTS app_users");
        jdbc.execute("CREATE TABLE app_users (id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL DEFAULT 'USER', status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE user_sessions (token_hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at TIMESTAMP NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_accounts (user_id BIGINT PRIMARY KEY, balance BIGINT NOT NULL DEFAULT 1000, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE demo_credit_ledger (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, delta BIGINT NOT NULL, balance_after BIGINT NOT NULL, reason VARCHAR(40) NOT NULL, reference_id VARCHAR(64), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE game_sessions (id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, game_id VARCHAR(64) NOT NULL, status VARCHAR(20) NOT NULL, score BIGINT NOT NULL DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, finished_at TIMESTAMP NULL, FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE game_configs (game_id VARCHAR(64) PRIMARY KEY, title VARCHAR(120) NOT NULL, category VARCHAR(32) NOT NULL, description VARCHAR(500) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE, updated_by BIGINT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (updated_by) REFERENCES app_users(id))");
        jdbc.update("INSERT INTO game_configs(game_id,title,category,description,enabled) VALUES (?,?,?,?,?)", "skyline-crash", "Skyline Crash", "ARCADE", "Demo round.", true);
        jdbc.execute("CREATE TABLE audit_events (id BIGINT AUTO_INCREMENT PRIMARY KEY, actor_user_id BIGINT NOT NULL, action VARCHAR(80) NOT NULL, target_type VARCHAR(40) NOT NULL, target_id VARCHAR(80) NOT NULL, reason VARCHAR(500) NOT NULL, before_value VARCHAR(500), after_value VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (actor_user_id) REFERENCES app_users(id))");
    }

    @Test
    void adminCanSuspendUserAndActionIsAudited() throws Exception {
        register("admin@example.com");
        register("player@example.com");
        jdbc.update("UPDATE app_users SET role='ADMIN' WHERE email='admin@example.com'");
        long playerId = jdbc.queryForObject("SELECT id FROM app_users WHERE email='player@example.com'", Long.class);
        String token = login("admin@example.com");

        mvc.perform(get("/api/admin/users?query=player").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.users[0].email").value("player@example.com"));

        mvc.perform(patch("/api/admin/users/" + playerId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\",\"reason\":\"Abuse report reviewed\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUSPENDED"));

        mvc.perform(get("/api/admin/audit-events").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.events[0].action").value("USER_STATUS_CHANGED"));
    }

    @Test
    void adminCanUpdateGameConfigurationAndAuditIt() throws Exception {
        register("admin-game@example.com");
        jdbc.update("UPDATE app_users SET role='ADMIN' WHERE email='admin-game@example.com'");
        String token = login("admin-game@example.com");

        mvc.perform(get("/api/admin/games").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.games[0].id").value("skyline-crash"));

        mvc.perform(patch("/api/admin/games/skyline-crash/config")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Skyline Demo\",\"description\":\"Updated demo description\",\"enabled\":true,\"reason\":\"Refresh demo copy\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Skyline Demo"));

        mvc.perform(get("/api/admin/audit-events").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.events[0].action").value("GAME_CONFIG_CHANGED"));
    }

    @Test
    void regularUserCannotReadAdminEndpoints() throws Exception {
        register("regular@example.com");
        String token = login("regular@example.com");
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassword123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("accessToken").asText();
    }
}
