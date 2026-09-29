package com.sudoku.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Startup schema repair for databases created before the Stage-3 multiplayer
 * change.
 *
 * <p>{@code LeaderboardScore} originally mapped its game relation as
 * {@code @OneToOne(unique = true)}, which made the ORM add a UNIQUE constraint
 * on {@code leaderboard_scores.game_id} alone. Stage 3 changed the mapping to
 * {@code @ManyToOne} so both room participants can score for the same game, and
 * the composite {@code (game_id, user_id)} constraint replaced it — but
 * {@code spring.jpa.hibernate.ddl-auto=update} only ever ADDS schema objects;
 * it never drops the obsolete one.
 *
 * <p>With the stale constraint present, awarding the second participant of a
 * completed room fails with a {@code DataIntegrityViolationException}. The
 * failed INSERT leaves a null-id entity in the Hibernate session, so the
 * transaction's commit-time flush then aborts with:
 *
 * <pre>
 *   null id in com.sudoku.model.LeaderboardScore entry
 *   (don't flush the Session after an exception occurs)
 * </pre>
 *
 * <p>On startup we therefore drop any UNIQUE constraint on
 * {@code leaderboard_scores} whose column list is exactly {@code (game_id)}.
 * The current {@code uk_leaderboard_game_user} constraint is never touched, and
 * the whole repair is a no-op on fresh schemas and non-H2 databases. Safe to
 * run repeatedly.
 *
 * <p>Hibernate's {@code unique=true} mapping is emitted as a plain
 * {@code CREATE UNIQUE INDEX}, and H2 records such indexes only in
 * {@code INFORMATION_SCHEMA.INDEXES}/{@code INDEX_COLUMNS} — never as a
 * {@code TABLE_CONSTRAINTS} row. The constraint query above therefore finds
 * nothing on real databases while a test that recreates the leftover via
 * {@code ALTER TABLE ... ADD CONSTRAINT} passes. The repair consequently scans
 * unique indexes with the exact column list {@code (game_id)} as well and drops
 * those; the composite {@code (game_id, user_id)} backing index has two columns
 * and primary-key indexes are not {@code UNIQUE INDEX} rows, so neither is
 * touched.
 */
@Component
public class LeaderboardScoreSchemaRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardScoreSchemaRepair.class);

    /** H2 refuses to drop an index another constraint owns and names it in the error. */
    private static final Pattern OWNED_BY_CONSTRAINT =
            Pattern.compile("belongs to constraint \"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    /**
     * Unique constraints on leaderboard_scores whose column set is exactly
     * GAME_ID (the composite (game_id, user_id) constraint has two columns and
     * is therefore never returned).
     */
    private static final String STALE_CONSTRAINTS_SQL =
            "SELECT tc.CONSTRAINT_NAME " +
            "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
            "JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu " +
            "  ON ccu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME AND ccu.TABLE_NAME = tc.TABLE_NAME " +
            "WHERE UPPER(tc.TABLE_NAME) = 'LEADERBOARD_SCORES' " +
            "  AND tc.CONSTRAINT_TYPE = 'UNIQUE' " +
            "GROUP BY tc.CONSTRAINT_NAME " +
            "HAVING COUNT(DISTINCT UPPER(ccu.COLUMN_NAME)) = 1 " +
            "   AND MAX(UPPER(ccu.COLUMN_NAME)) = 'GAME_ID'";

    /**
     * Unique indexes on leaderboard_scores whose column list is exactly
     * (GAME_ID): the shape {@code ddl-auto=update} actually leaves behind for
     * the old {@code @OneToOne(unique=true)} mapping. H2 keeps those out of
     * TABLE_CONSTRAINTS, so they are invisible to {@link #STALE_CONSTRAINTS_SQL}.
     */
    private static final String STALE_UNIQUE_INDEXES_SQL =
            "SELECT ic.INDEX_NAME " +
            "FROM INFORMATION_SCHEMA.INDEXES i " +
            "JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic " +
            "  ON ic.INDEX_CATALOG = i.INDEX_CATALOG AND ic.INDEX_SCHEMA = i.INDEX_SCHEMA " +
            " AND ic.INDEX_NAME = i.INDEX_NAME AND ic.TABLE_NAME = i.TABLE_NAME " +
            "WHERE UPPER(i.TABLE_NAME) = 'LEADERBOARD_SCORES' " +
            "  AND i.INDEX_TYPE_NAME = 'UNIQUE INDEX' " +
            "GROUP BY ic.INDEX_NAME " +
            "HAVING COUNT(DISTINCT UPPER(ic.COLUMN_NAME)) = 1 " +
            "   AND MAX(UPPER(ic.COLUMN_NAME)) = 'GAME_ID'";

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardScoreSchemaRepair(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        repair();
    }

    /**
     * Drops stale game_id-only unique constraints and unique indexes on
     * leaderboard_scores. Idempotent: a second run finds nothing and does nothing.
     */
    public void repair() {
        if (!isH2()) {
            return; // catalog queries below are H2-specific
        }

        // 1. Table constraints (the shape ALTER TABLE ... ADD CONSTRAINT creates).
        //    Run first: dropping the constraint also removes its backing index.
        for (String name : queryStaleNames(STALE_CONSTRAINTS_SQL)) {
            try {
                jdbcTemplate.execute("ALTER TABLE leaderboard_scores DROP CONSTRAINT \"" + name + "\"");
                log.warn("Dropped stale unique constraint {} on leaderboard_scores(game_id): it blocked the " +
                        "second player's score in multiplayer rooms (left behind by ddl-auto=update).", name);
            } catch (Exception ex) {
                log.warn("Could not drop stale constraint {} on leaderboard_scores: {}", name, ex.getMessage());
            }
        }

        // 2. Plain unique indexes (the shape Hibernate emits for unique=true and
        //    the one real H2 databases contain); invisible to TABLE_CONSTRAINTS.
        for (String name : queryStaleNames(STALE_UNIQUE_INDEXES_SQL)) {
            dropStaleUniqueIndex(name);
        }
    }

    /**
     * Drops a game_id-only unique index, working around H2's rule that an index
     * adopted by a constraint (typically the game_id foreign key) may not be
     * dropped directly: the owning constraint is dropped, the index released
     * with it, and the constraint immediately rebuilt from its catalog
     * definition so referential integrity is never left missing.
     */
    private void dropStaleUniqueIndex(String indexName) {
        try {
            jdbcTemplate.execute("DROP INDEX \"" + indexName + "\"");
            log.warn("Dropped stale unique index {} on leaderboard_scores(game_id): it blocked the " +
                    "second player's score in multiplayer rooms (left behind by ddl-auto=update).", indexName);
        } catch (Exception ex) {
            Matcher owned = OWNED_BY_CONSTRAINT.matcher(causalMessage(ex));
            if (owned.find()) {
                rebuildOwnerToReleaseIndex(indexName, owned.group(1));
            } else {
                log.warn("Could not drop stale unique index {} on leaderboard_scores: {}",
                        indexName, causalMessage(ex));
            }
        }
    }

    private void rebuildOwnerToReleaseIndex(String indexName, String fkName) {
        try {
            Map<String, Object> fk = jdbcTemplate.queryForMap(
                    "SELECT UNIQUE_CONSTRAINT_NAME, UPDATE_RULE, DELETE_RULE " +
                            "FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_NAME = ?", fkName);
            List<String> childColumns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                            "WHERE CONSTRAINT_NAME = ? ORDER BY ORDINAL_POSITION", String.class, fkName);
            List<String> parentKey = new ArrayList<>();
            jdbcTemplate.query(
                    "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                            "WHERE CONSTRAINT_NAME = ? ORDER BY ORDINAL_POSITION",
                    rs -> {
                        if (parentKey.isEmpty()) parentKey.add(rs.getString(1));
                        parentKey.add(rs.getString(2));
                    },
                    fk.get("UNIQUE_CONSTRAINT_NAME"));
            if (childColumns.isEmpty() || parentKey.size() < 2) {
                throw new IllegalStateException("cannot reconstruct foreign key " + fkName);
            }

            String reAdd = "ALTER TABLE leaderboard_scores ADD CONSTRAINT \"" + fkName + "\" FOREIGN KEY ("
                    + String.join(", ", childColumns) + ") REFERENCES " + parentKey.get(0)
                    + "(" + String.join(", ", parentKey.subList(1, parentKey.size())) + ")"
                    + onClause("ON UPDATE", String.valueOf(fk.get("UPDATE_RULE")))
                    + onClause("ON DELETE", String.valueOf(fk.get("DELETE_RULE")));

            jdbcTemplate.execute("ALTER TABLE leaderboard_scores DROP CONSTRAINT \"" + fkName + "\"");
            // H2 removes the adopted index with its owner; IF EXISTS covers variants that do not.
            jdbcTemplate.execute("DROP INDEX IF EXISTS \"" + indexName + "\"");
            jdbcTemplate.execute(reAdd);
            log.warn("Dropped stale unique index {} on leaderboard_scores(game_id): it was adopted by " +
                    "foreign key {}, which was rebuilt from its catalog definition to release it.",
                    indexName, fkName);
        } catch (Exception ex) {
            log.warn("Could not drop stale unique index {} (owner foreign key {}): {}",
                    indexName, fkName, causalMessage(ex));
        }
    }

    /** {@code NO_ACTION} is H2's default, so it is omitted; other rules map to SQL keywords. */
    private String onClause(String keyword, String rule) {
        if (rule == null || rule.isBlank() || "NO_ACTION".equalsIgnoreCase(rule)) {
            return "";
        }
        return " " + keyword + " " + rule.replace('_', ' ');
    }

    /** All messages in the cause chain: JdbcTemplate wraps the original H2 error. */
    private String causalMessage(Throwable ex) {
        StringBuilder messages = new StringBuilder();
        Throwable current = ex;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(current.getMessage()).append('\n');
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    private List<String> queryStaleNames(String sql) {
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception ex) {
            log.debug("Leaderboard schema repair query skipped (catalog unavailable): {}", ex.getMessage());
            return List.of();
        }
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
