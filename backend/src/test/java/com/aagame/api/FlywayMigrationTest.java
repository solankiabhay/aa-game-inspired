package com.aagame.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aagameflyway;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration"
})
class FlywayMigrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void allMigrationsCreateExpectedTables() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('APP_USERS','USER_SESSIONS','DEMO_CREDIT_ACCOUNTS','DEMO_CREDIT_LEDGER','GAME_SESSIONS','AUDIT_EVENTS','GAME_CONFIGS','AUTH_ACTION_TOKENS')", Integer.class))
            .isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"success\" = TRUE AND \"version\" IS NOT NULL", Integer.class))
            .isEqualTo(5);
    }
}
