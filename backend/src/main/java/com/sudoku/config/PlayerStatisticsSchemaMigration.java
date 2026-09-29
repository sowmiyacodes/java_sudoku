package com.sudoku.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class PlayerStatisticsSchemaMigration implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatisticsSchemaMigration.class);

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                log.info("Applying pre-Hibernate schema migrations for player_statistics...");
                stmt.execute("CREATE TABLE IF NOT EXISTS player_statistics (id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE);");
                stmt.execute("ALTER TABLE player_statistics ADD COLUMN IF NOT EXISTS current_streak INT DEFAULT 0;");
                stmt.execute("ALTER TABLE player_statistics ADD COLUMN IF NOT EXISTS best_streak INT DEFAULT 0;");
                log.info("player_statistics schema migration completed successfully.");
            } catch (Exception e) {
                log.warn("Notice on schema migration: {}", e.getMessage());
            }
        }
        return bean;
    }
}
