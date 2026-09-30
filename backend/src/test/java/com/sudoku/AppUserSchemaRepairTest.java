package com.sudoku;

import com.sudoku.config.AppUserSchemaRepair;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppUserSchemaRepairTest {

    @Test
    @DisplayName("Startup repair adds missing is_active column to stale app_users tables")
    void testMissingIsActiveColumnIsAdded() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:app-user-repair-test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS app_users");
        jdbcTemplate.execute("CREATE TABLE app_users ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "username VARCHAR(30) NOT NULL,"
                + "display_name VARCHAR(80) NOT NULL,"
                + "email VARCHAR(254) NOT NULL,"
                + "password_hash VARCHAR(100) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL"
                + ")");

        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'APP_USERS' AND COLUMN_NAME = 'IS_ACTIVE'",
                Integer.class);
        assertEquals(0, before == null ? 0 : before);

        AppUserSchemaRepair schemaRepair = new AppUserSchemaRepair(jdbcTemplate);
        schemaRepair.repair();

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'APP_USERS' AND COLUMN_NAME = 'IS_ACTIVE'",
                Integer.class);
        assertEquals(1, after == null ? 0 : after);

        jdbcTemplate.execute("INSERT INTO app_users (is_active, created_at, display_name, email, password_hash, username) VALUES (true, CURRENT_TIMESTAMP, 'Alice', 'alice@example.com', 'hash', 'alice')");
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM app_users WHERE username = 'alice'", Integer.class);
        assertEquals(1, rows == null ? 0 : rows);
    }
}
