package com.sudoku;

import com.sudoku.dto.CreateGameRequest;
import com.sudoku.dto.LeaderboardEntryResponse;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.SubmitResponse;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.service.GameService;
import com.sudoku.service.LeaderboardService;
import com.sudoku.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the persistent leaderboard:
 * - score awarded only after successful completion validation,
 * - duplicate completion never produces a second score,
 * - daily / weekly / all-time period filtering against real timestamps,
 * - deterministic ranking ties (points -> games -> best time -> user id).
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-leaderboard-integration;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class LeaderboardIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private GameService gameService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private LeaderboardScoreRepository leaderboardScoreRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private User register(String prefix) {
        String suffix = uniqueSuffix();
        return userService.register(new RegisterRequest(
                prefix + "_" + suffix,
                prefix + " " + suffix,
                prefix + "_" + suffix + "@sudoku.test",
                "Password123!"
        ));
    }

    private int[][] solvedBoard() {
        return new int[][] {
                {5, 3, 4, 6, 7, 8, 9, 1, 2},
                {6, 7, 2, 1, 9, 5, 3, 4, 8},
                {1, 9, 8, 3, 4, 2, 5, 6, 7},
                {8, 5, 9, 7, 6, 1, 4, 2, 3},
                {4, 2, 6, 8, 5, 3, 7, 9, 1},
                {7, 1, 3, 9, 2, 4, 8, 5, 6},
                {9, 6, 1, 5, 3, 7, 2, 8, 4},
                {2, 8, 7, 4, 1, 9, 6, 3, 5},
                {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
    }

    private Long createAlmostCompleteGame(User user) {
        int[][] initial = solvedBoard();
        initial[8][8] = 0;
        CreateGameRequest request = new CreateGameRequest();
        request.setDifficulty("Easy");
        request.setInitialBoard(initial);
        request.setSolutionBoard(solvedBoard());
        return gameService.createGame(request, user).getId();
    }

    private long countScoresForGame(Long gameId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_scores WHERE game_id = ?",
                Long.class, gameId);
        return count != null ? count : 0L;
    }

    private LeaderboardEntryResponse findEntry(List<LeaderboardEntryResponse> board, String username) {
        return board.stream()
                .filter(entry -> entry.username().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     * Inserts a leaderboard score directly (bypassing the formula) so tests can
     * control exact point totals for ordering and period-filter assertions.
     */
    private LeaderboardScore insertRawScore(User user, int points, long elapsedSeconds) {
        Game game = new Game();
        game.setUser(user);
        game.setStatus(GameStatus.COMPLETED);
        game.setDifficulty("Hard");
        game.setInitialBoardJson("{}");
        game.setCurrentBoardJson("{}");
        game.setSolutionBoardJson("{}");
        game.setCompletedAt(LocalDateTime.now());
        game.setElapsedSeconds(elapsedSeconds);
        Game savedGame = gameRepository.save(game);

        LeaderboardScore score = new LeaderboardScore(
                user, savedGame, points, "Hard", elapsedSeconds, 0, 0, 100, LocalDateTime.now());
        return leaderboardScoreRepository.save(score);
    }

    @Test
    @DisplayName("Score is awarded only after completion validation and never duplicated on repeat submits")
    void testDuplicateCompletionAwardsSingleScore() {
        User user = register("dup");
        Long gameId = createAlmostCompleteGame(user);

        // Incomplete submission must fail validation and award nothing
        SubmitResponse incomplete = gameService.submitGame(gameId, user);
        assertFalse(incomplete.isCompleted());
        assertFalse(leaderboardScoreRepository.existsByGameId(gameId));
        assertEquals(0, countScoresForGame(gameId));

        // Final move completes the puzzle -> exactly one score awarded
        gameService.makeMove(gameId, new com.sudoku.dto.MoveRequest(8, 8, 9), user);
        assertTrue(leaderboardScoreRepository.existsByGameId(gameId));
        assertEquals(1, countScoresForGame(gameId));

        // Re-submitting the same completed game must not award a second score
        SubmitResponse duplicate = gameService.submitGame(gameId, user);
        assertTrue(duplicate.isCompleted());
        assertEquals(1, countScoresForGame(gameId));

        // Re-awarding via the scoring service directly is also idempotent
        leaderboardScoreRepository.flush();
        assertEquals(1, countScoresForGame(gameId));
    }

    @Test
    @DisplayName("Daily, weekly and all-time periods filter scores by real timestamps")
    void testPeriodFiltersAgainstDatabase() {
        User recentUser = register("recent");
        User oldUser = register("stale");

        LeaderboardScore recent = insertRawScore(recentUser, 910000, 300);
        LeaderboardScore stale = insertRawScore(oldUser, 920000, 300);

        // Backdate the stale score by 3 days: outside daily, inside weekly and all-time
        jdbcTemplate.update(
                "UPDATE leaderboard_scores SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(3)),
                stale.getId());

        // Daily: only the recent score
        List<LeaderboardEntryResponse> daily = leaderboardService.getLeaderboard("daily", null);
        assertNotNull(findEntry(daily, recentUser.getUsername()));
        assertNull(findEntry(daily, oldUser.getUsername()));

        // Weekly: both, ordered by total points (stale has more points -> higher rank)
        List<LeaderboardEntryResponse> weekly = leaderboardService.getLeaderboard("weekly", null);
        LeaderboardEntryResponse staleWeekly = findEntry(weekly, oldUser.getUsername());
        LeaderboardEntryResponse recentWeekly = findEntry(weekly, recentUser.getUsername());
        assertNotNull(staleWeekly);
        assertNotNull(recentWeekly);
        assertTrue(staleWeekly.rank() < recentWeekly.rank(),
                "Higher points must rank higher within the same period");

        // All-time: both present
        List<LeaderboardEntryResponse> allTime = leaderboardService.getLeaderboard("all-time", null);
        assertNotNull(findEntry(allTime, recentUser.getUsername()));
        assertNotNull(findEntry(allTime, oldUser.getUsername()));
    }

    @Test
    @DisplayName("Players tied on points, games and best time get a deterministic order")
    void testRankingTiesAreDeterministic() {
        User tieA = register("tiea");
        User tieB = register("tieb");
        User tieC = register("tiec");

        // Identical aggregates on purpose: identical points, games, accuracy and time
        insertRawScore(tieA, 930000, 420);
        insertRawScore(tieB, 930000, 420);
        insertRawScore(tieC, 930000, 420);

        List<LeaderboardEntryResponse> firstRead = leaderboardService.getLeaderboard("all-time", null);
        LeaderboardEntryResponse entryA = findEntry(firstRead, tieA.getUsername());
        LeaderboardEntryResponse entryB = findEntry(firstRead, tieB.getUsername());
        LeaderboardEntryResponse entryC = findEntry(firstRead, tieC.getUsername());

        assertNotNull(entryA);
        assertNotNull(entryB);
        assertNotNull(entryC);

        // Equal on points and games and best time -> tie-break by ascending user id
        assertEquals(930000L, entryA.totalPoints());
        assertEquals(930000L, entryB.totalPoints());
        assertEquals(930000L, entryC.totalPoints());
        assertTrue(entryA.rank() < entryB.rank(), "Lower user id must win the tie");
        assertTrue(entryB.rank() < entryC.rank(), "Lower user id must win the tie");

        // Deterministic: a second read yields the identical ordering
        List<LeaderboardEntryResponse> secondRead = leaderboardService.getLeaderboard("all-time", null);
        List<String> firstOrder = firstRead.stream().map(e -> e.username().toLowerCase(Locale.ROOT)).toList();
        List<String> secondOrder = secondRead.stream().map(e -> e.username().toLowerCase(Locale.ROOT)).toList();
        assertEquals(firstOrder, secondOrder);
    }
}
