package com.sudoku.service;

import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.SudokuBoard;
import com.sudoku.model.User;
import com.sudoku.repository.HintHistoryRepository;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.repository.MoveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Scoring Service for Sudoku Leaderboard.
 *
 * Scoring Formula Documentation:
 * ------------------------------
 * 1. Base Points by Difficulty:
 *    - Easy:   500 points (Target time: 300s / 5m)
 *    - Medium: 1000 points (Target time: 600s / 10m)
 *    - Hard:   1800 points (Target time: 900s / 15m)
 *    - Expert: 2500 points (Target time: 1200s / 20m)
 *    - Default/Custom: 800 points (Target time: 600s / 10m)
 *
 * 2. Time Bonus (Speed Incentive):
 *    If elapsedSeconds < targetTime:
 *      timeBonus = (targetTime - elapsedSeconds) * timeMultiplier
 *      where multipliers are:
 *        - Easy: 1.0 pt/s
 *        - Medium: 1.5 pt/s
 *        - Hard: 2.0 pt/s
 *        - Expert: 2.5 pt/s
 *        - Default: 1.0 pt/s
 *      Bonus is capped at 50% of the base points to avoid extreme inflation.
 *
 * 3. Time Penalty (Decay):
 *    If elapsedSeconds > targetTime:
 *      timePenalty = (elapsedSeconds - targetTime) * 0.5 pt/s
 *      Penalty is capped at 50% of base points so slow solvers still gain points.
 *
 * 4. Mistake Penalty:
 *    penalty = mistakes * mistakeMultiplier
 *    where multipliers are:
 *      - Easy: 40 pts/mistake
 *      - Medium: 60 pts/mistake
 *      - Hard: 80 pts/mistake
 *      - Expert: 100 pts/mistake
 *      - Default: 50 pts/mistake
 *
 * 5. Hint Penalty:
 *    penalty = hintsUsed * hintMultiplier
 *    where multipliers are:
 *      - Easy: 30 pts/hint
 *      - Medium: 50 pts/hint
 *      - Hard: 70 pts/hint
 *      - Expert: 90 pts/hint
 *      - Default: 50 pts/hint
 *
 * 6. Non-Negative Floor & Guarantee:
 *    rawPoints = basePoints + timeBonus - timePenalty - mistakePenalty - hintPenalty
 *    finalPoints = Math.max(0, rawPoints)
 *    Ensures points awarded are never negative under any circumstances.
 *
 * 7. Duplicate Prevention:
 *    Enforced at the (game, user) level: the service checks existsByGameIdAndUserId
 *    before awarding and the database enforces a unique constraint on
 *    (game_id, user_id) in leaderboard_scores. In a multiplayer room game this
 *    means each eligible participant is awarded exactly once, while replaying
 *    or re-submitting a solo game can never double-award its owner.
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final LeaderboardScoreRepository leaderboardScoreRepository;
    private final MoveRepository moveRepository;
    private final HintHistoryRepository hintHistoryRepository;
    private final SudokuValidationService validationService;

    /** Runs the score INSERT in its own short-lived transaction (see awardScore). */
    private final TransactionTemplate scoreInsertTemplate;

    public ScoringService(
            LeaderboardScoreRepository leaderboardScoreRepository,
            MoveRepository moveRepository,
            HintHistoryRepository hintHistoryRepository,
            SudokuValidationService validationService,
            PlatformTransactionManager transactionManager
    ) {
        this.leaderboardScoreRepository = leaderboardScoreRepository;
        this.moveRepository = moveRepository;
        this.hintHistoryRepository = hintHistoryRepository;
        this.validationService = validationService;
        this.scoreInsertTemplate = new TransactionTemplate(transactionManager);
        this.scoreInsertTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Calculates points strictly using the documented scoring formula.
     */
    public int calculatePoints(String difficulty, int mistakes, int hintsUsed, long elapsedSeconds) {
        int basePoints = getBasePoints(difficulty);
        long targetTime = getTargetTimeSeconds(difficulty);
        double timeMultiplier = getTimeBonusMultiplier(difficulty);
        int mistakeWeight = getMistakeWeight(difficulty);
        int hintWeight = getHintWeight(difficulty);

        long timeBonus = 0;
        long timePenalty = 0;

        if (elapsedSeconds < targetTime) {
            long secondsSaved = targetTime - elapsedSeconds;
            timeBonus = Math.round(secondsSaved * timeMultiplier);
            // Cap bonus at 50% base points
            timeBonus = Math.min(timeBonus, basePoints / 2);
        } else if (elapsedSeconds > targetTime) {
            long extraSeconds = elapsedSeconds - targetTime;
            timePenalty = Math.round(extraSeconds * 0.5);
            // Cap time penalty at 50% base points
            timePenalty = Math.min(timePenalty, basePoints / 2);
        }

        long mistakePenalty = (long) Math.max(0, mistakes) * mistakeWeight;
        long hintPenalty = (long) Math.max(0, hintsUsed) * hintWeight;

        long rawScore = basePoints + timeBonus - timePenalty - mistakePenalty - hintPenalty;
        return (int) Math.max(0, rawScore);
    }

    /**
     * Calculates accuracy percentage (0 - 100) based on moves made and mistakes.
     */
    public int calculateAccuracy(long movesCount, int mistakes) {
        long attempts = movesCount + mistakes;
        if (attempts <= 0) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, Math.round((movesCount * 100.0) / attempts)));
    }

    /**
     * Awards score to an authenticated user upon successful game completion validation.
     * Prevents duplicate awards and ensures non-negative scores.
     */
    @Transactional
    public LeaderboardScore awardScore(Game game, User user) {
        if (game == null || user == null) {
            return null;
        }

        // Award at most once per (game, participant): multiplayer room games
        // award both players, solo games their single owner.
        if (leaderboardScoreRepository.existsByGameIdAndUserId(game.getId(), user.getId())) {
            log.info("Score already awarded for game ID {} and user ID {}", game.getId(), user.getId());
            return leaderboardScoreRepository.findByGameIdAndUserId(game.getId(), user.getId()).orElse(null);
        }

        // Validate game completion status and board correctness
        if (game.getStatus() != GameStatus.COMPLETED) {
            log.warn("Cannot award score for game ID {} that is not COMPLETED (status={})", game.getId(), game.getStatus());
            return null;
        }

        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
        if (!validationService.isBoardSolved(currentBoard)) {
            log.warn("Cannot award score for game ID {}: board validation did not pass.", game.getId());
            return null;
        }

        int hintsUsed = (int) hintHistoryRepository.countByGameId(game.getId());
        long movesCount = moveRepository.countByGameId(game.getId());
        int accuracy = calculateAccuracy(movesCount, game.getMistakes());
        int points = calculatePoints(game.getDifficulty(), game.getMistakes(), hintsUsed, game.getElapsedSeconds());

        LeaderboardScore score = new LeaderboardScore(
                user,
                game,
                points,
                game.getDifficulty() != null ? game.getDifficulty() : "Custom",
                game.getElapsedSeconds(),
                game.getMistakes(),
                hintsUsed,
                accuracy,
                game.getCompletedAt()
        );

        try {
            // The INSERT runs in its own transaction (REQUIRES_NEW): if it fails
            // (duplicate race or a stale schema constraint), only that inner
            // transaction rolls back and its failed entity never enters THIS
            // session. Without this, the null-id entity left behind made the outer
            // commit flush abort with "null id in com.sudoku.model.LeaderboardScore
            // entry (don't flush the Session after an exception occurs)".
            return scoreInsertTemplate.execute(status -> leaderboardScoreRepository.saveAndFlush(score));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate score award intercepted by DB constraint for game ID {} user ID {}: {}",
                    game.getId(), user.getId(), ex.getMessage());
            return leaderboardScoreRepository.findByGameIdAndUserId(game.getId(), user.getId()).orElse(null);
        }
    }

    private int getBasePoints(String difficulty) {
        if (difficulty == null) return 800;
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 500;
            case "medium" -> 1000;
            case "hard" -> 1800;
            case "expert" -> 2500;
            default -> 800;
        };
    }

    private long getTargetTimeSeconds(String difficulty) {
        if (difficulty == null) return 600;
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 300;     // 5 minutes
            case "medium" -> 600;   // 10 minutes
            case "hard" -> 900;     // 15 minutes
            case "expert" -> 1200;  // 20 minutes
            default -> 600;
        };
    }

    private double getTimeBonusMultiplier(String difficulty) {
        if (difficulty == null) return 1.0;
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 1.0;
            case "medium" -> 1.5;
            case "hard" -> 2.0;
            case "expert" -> 2.5;
            default -> 1.0;
        };
    }

    private int getMistakeWeight(String difficulty) {
        if (difficulty == null) return 50;
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 40;
            case "medium" -> 60;
            case "hard" -> 80;
            case "expert" -> 100;
            default -> 50;
        };
    }

    private int getHintWeight(String difficulty) {
        if (difficulty == null) return 50;
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> 30;
            case "medium" -> 50;
            case "hard" -> 70;
            case "expert" -> 90;
            default -> 50;
        };
    }
}
