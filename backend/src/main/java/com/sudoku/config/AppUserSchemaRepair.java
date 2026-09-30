package com.sudoku.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

@Component
public class AppUserSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppUserSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;

    public AppUserSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        repair();
    }

    public void repair() {
        if (!isH2()) {
            return;
        }

        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'APP_USERS'",
                Integer.class);
        if (tableCount == null || tableCount == 0) {
            log.debug("app_users table not found yet; skipping repair until Hibernate creates it.");
            return;
        }

        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE UPPER(TABLE_NAME) = 'APP_USERS' AND UPPER(COLUMN_NAME) = 'IS_ACTIVE'",
                Integer.class);
        if (columnCount != null && columnCount > 0) {
            log.debug("app_users already contains is_active; no repair needed.");
            return;
        }

        jdbcTemplate.execute("ALTER TABLE app_users ADD COLUMN is_active BOOLEAN DEFAULT TRUE NOT NULL");
        log.warn("Added missing is_active column to app_users to repair a stale H2 schema left behind by earlier ddl-auto=update runs.");
    }

    private boolean isH2() {
        if (jdbcTemplate.getDataSource() == null) {
            return false;
        }

        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException ex) {
            log.debug("Could not detect database product: {}", ex.getMessage());
            return false;
        }
    }
}
