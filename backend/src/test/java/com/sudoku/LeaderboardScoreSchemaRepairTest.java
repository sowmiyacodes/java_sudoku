package com.sudoku;

import com.sudoku.config.LeaderboardScoreSchemaRepair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Startup repair of the Stage-2 leftover schema: the stale UNIQUE constraint
 * on leaderboard_scores(game_id) alone, which blocks the second participant's
 * score when a multiplayer room completes (and used to end the transaction with
 * "null id in com.sudoku.model.LeaderboardScore entry").
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-rooms-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class LeaderboardScoreSchemaRepairTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LeaderboardScoreSchemaRepair schemaRepair;

    private int constraintCount(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE TABLE_NAME = 'LEADERBOARD_SCORES' AND CONSTRAINT_NAME = ?",
                Integer.class, name);
        return count == null ? 0 : count;
    }

    private int uniqueIndexCount(String name) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE TABLE_NAME = 'LEADERBOARD_SCORES' AND INDEX_NAME = ?",
                Integer.class, name);
        return count == null ? 0 : count;
    }

    @Test
    @DisplayName("Repair drops a game_id-only unique constraint but keeps (game_id, user_id)")
    void testStaleConstraintDropped() {
        // The current composite constraint must be present before the repair...
        assertEquals(1, constraintCount("UK_LEADERBOARD_GAME_USER"));

        // ...and a stale Stage-2 style constraint is recreated for the test.
        jdbcTemplate.execute("ALTER TABLE leaderboard_scores ADD CONSTRAINT UK_TEST_STALE_GAME UNIQUE (game_id)");
        try {
            assertEquals(1, constraintCount("UK_TEST_STALE_GAME"));

            schemaRepair.repair();

            assertEquals(0, constraintCount("UK_TEST_STALE_GAME"),
                    "game_id-only unique constraint must be dropped");
            assertEquals(1, constraintCount("UK_LEADERBOARD_GAME_USER"),
                    "the (game_id, user_id) constraint must survive the repair");
        } finally {
            jdbcTemplate.execute("ALTER TABLE leaderboard_scores DROP CONSTRAINT IF EXISTS UK_TEST_STALE_GAME");
        }
    }

    @Test
    @DisplayName("Repair drops a game_id-only UNIQUE INDEX (the shape ddl-auto=update actually leaves)")
    void testStaleUniqueIndexDropped() {
        // Hibernate's old @OneToOne(unique=true) mapping created a plain unique
        // index, not a table constraint — exactly what real H2 databases contain.
        // Such an index never appears in TABLE_CONSTRAINTS, which is why the
        // constraint-only query used to no-op in production while this test suite
        // stayed green. The repair must handle this shape too.
        jdbcTemplate.execute("CREATE UNIQUE INDEX UK_TEST_STALE_GAME_IDX ON leaderboard_scores (game_id)");
        try {
            assertEquals(1, uniqueIndexCount("UK_TEST_STALE_GAME_IDX"));
            assertEquals(0, constraintCount("UK_TEST_STALE_GAME_IDX"),
                    "a plain unique index is invisible to TABLE_CONSTRAINTS (the old blind spot)");

            schemaRepair.repair();

            assertEquals(0, uniqueIndexCount("UK_TEST_STALE_GAME_IDX"),
                    "game_id-only unique index must be dropped");
            assertEquals(1, constraintCount("UK_LEADERBOARD_GAME_USER"),
                    "the (game_id, user_id) constraint must survive the repair");
        } finally {
            jdbcTemplate.execute("DROP INDEX IF EXISTS UK_TEST_STALE_GAME_IDX");
        }
    }

    @Test
    @DisplayName("Repair rebuilds the owning FK when the legacy unique index was adopted by it (production shape)")
    void testStaleIndexAdoptedByForeignKey() {
        System.out.println("DIAG-ENTER adopted-test");
        // Production shape: the game_id foreign key adopted the stale game_id-only
        // unique index as its backing index, so H2 refuses a plain DROP INDEX with
        // "Index ... belongs to constraint ...". Recreate it: drop the FK, plant the
        // unique index, re-add the FK so it adopts the index.
        String fkName = jdbcTemplate.queryForObject(
                "SELECT r.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS r "
                        + "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k ON k.CONSTRAINT_NAME = r.CONSTRAINT_NAME "
                        + "WHERE k.TABLE_NAME = 'LEADERBOARD_SCORES' AND k.COLUMN_NAME = 'GAME_ID'",
                String.class);
        String updateRule = jdbcTemplate.queryForObject(
                "SELECT UPDATE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                String.class, fkName);
        String deleteRule = jdbcTemplate.queryForObject(
                "SELECT DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                String.class, fkName);

        jdbcTemplate.execute("ALTER TABLE leaderboard_scores DROP CONSTRAINT \"" + fkName + "\"");
        jdbcTemplate.execute("CREATE UNIQUE INDEX UK_TEST_OWNED_STALE ON leaderboard_scores (game_id)");
        jdbcTemplate.execute("ALTER TABLE leaderboard_scores ADD CONSTRAINT \"" + fkName
                + "\" FOREIGN KEY (game_id) REFERENCES games(id)"
                + onClause("ON UPDATE", updateRule) + onClause("ON DELETE", deleteRule));
        try {
            // The plain index shape is now locked behind the owning FK.
            Exception refused = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                    () -> jdbcTemplate.execute("DROP INDEX UK_TEST_OWNED_STALE"));
            String fullMessage = refused.getMessage() + (refused.getCause() != null ? " " + refused.getCause().getMessage() : "");
            org.junit.jupiter.api.Assertions.assertTrue(
                    fullMessage.toLowerCase().contains("belongs to constraint"),
                    "H2 must refuse the direct drop so the FK-rebuild path is actually exercised");

            schemaRepair.repair();

            assertEquals(0, gameIdOnlyUniqueIndexCount(),
                    "stale game_id-only unique index must be gone after the FK rebuild");
            assertEquals(1, constraintCount("UK_LEADERBOARD_GAME_USER"),
                    "the (game_id, user_id) constraint must survive the repair");
            Integer fkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                    Integer.class, fkName);
            assertEquals(1, fkCount == null ? 0 : fkCount,
                    "the game_id foreign key must be rebuilt, not left missing");
        } finally {
            try {
                jdbcTemplate.execute("DROP INDEX IF EXISTS UK_TEST_OWNED_STALE");
            } catch (Exception ignored) {}
            // Ensure the FK exists again even if a mid-test failure interrupted the dance.
            Integer fkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = ?",
                    Integer.class, fkName);
            if (fkCount != null && fkCount == 0) {
                jdbcTemplate.execute("ALTER TABLE leaderboard_scores ADD CONSTRAINT \"" + fkName
                        + "\" FOREIGN KEY (game_id) REFERENCES games(id)"
                        + onClause("ON UPDATE", updateRule) + onClause("ON DELETE", deleteRule));
            }
        }
    }

    /** Number of UNIQUE indexes on leaderboard_scores covering exactly (game_id). */
    private int gameIdOnlyUniqueIndexCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS "
                        + "WHERE TABLE_NAME = 'LEADERBOARD_SCORES' AND IS_UNIQUE = TRUE "
                        + "GROUP BY INDEX_NAME HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'GAME_ID')",
                Integer.class);
        return count == null ? 0 : count;
    }

    private String onClause(String keyword, String rule) {
        if (rule == null || "NO_ACTION".equalsIgnoreCase(rule)) return "";
        return " " + keyword + " " + rule.replace('_', ' ');
    }

    @Test
    @DisplayName("Repair is idempotent: a clean schema is left untouched")
    void testRepairIsIdempotent() {
        schemaRepair.repair();
        schemaRepair.repair();

        assertEquals(1, constraintCount("UK_LEADERBOARD_GAME_USER"),
                "legit constraint must still exist after repeated repairs");
        assertEquals(0, constraintCount("UK_LEADERBOARD_GAME"),
                "no game_id-only constraint may exist after repair");
    }
}
