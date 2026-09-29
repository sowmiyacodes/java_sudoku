package com.sudoku.service;

import com.sudoku.dto.DifficultyPerformanceDto;
import com.sudoku.dto.PlayerStatisticsDto;
import com.sudoku.dto.SkillPredictionRequestDto;
import com.sudoku.model.*;
import com.sudoku.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlayerStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatisticsService.class);

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final HintHistoryRepository hintHistoryRepository;
    private final LeaderboardScoreRepository leaderboardScoreRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;

    public PlayerStatisticsService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            HintHistoryRepository hintHistoryRepository,
            LeaderboardScoreRepository leaderboardScoreRepository,
            PlayerStatisticsRepository playerStatisticsRepository
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.hintHistoryRepository = hintHistoryRepository;
        this.leaderboardScoreRepository = leaderboardScoreRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
    }

    /**
     * Calculates fresh player statistics from raw H2 game data and persists them in PlayerStatistics.
     */
    @Transactional
    public PlayerStatistics calculateAndSaveStatistics(Long userId) {
        if (userId == null) {
            return null;
        }

        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(userId);
        PlayerStatistics stats = playerStatisticsRepository.findByUserId(userId)
                .orElse(new PlayerStatistics(userId));

        if (games.isEmpty()) {
            stats.setGamesPlayed(0);
            stats.setGamesCompleted(0);
            stats.setGamesAbandoned(0);
            stats.setCompletionRate(0.0);
            stats.setAverageTime(0);
            stats.setBestTime(0);
            stats.setAverageScore(0);
            stats.setAverageAccuracy(0.0);
            stats.setAverageMistakes(0.0);
            stats.setAverageHints(0.0);
            stats.setAverageUndos(0.0);
            stats.setAverageMoves(0.0);
            stats.setHardCompletionRate(0.0);
            stats.setMediumCompletionRate(0.0);
            stats.setEasyCompletionRate(0.0);
            stats.setRecentAccuracy(0.0);
            stats.setRecentErrorRate(0.0);
            stats.setAverageMovesPerMinute(0.0);
            stats.setCurrentStreak(0);
            stats.setBestStreak(0);
            stats.setUpdatedAt(LocalDateTime.now());
            return playerStatisticsRepository.save(stats);
        }

        int totalGames = games.size();
        List<Game> completedGames = games.stream()
                .filter(g -> g.getStatus() == GameStatus.COMPLETED)
                .toList();
        int totalCompleted = completedGames.size();
        int totalAbandoned = (int) games.stream()
                .filter(g -> g.getStatus() == GameStatus.PAUSED || g.getStatus() == GameStatus.IN_PROGRESS)
                .count();

        double completionRate = totalGames > 0 ? (double) totalCompleted / totalGames : 0.0;

        // Times
        long totalDuration = games.stream().mapToLong(Game::getElapsedSeconds).sum();
        long avgTime = totalGames > 0 ? totalDuration / totalGames : 0;
        long bestTime = completedGames.stream()
                .mapToLong(Game::getElapsedSeconds)
                .min()
                .orElse(0L);

        // Scores from LeaderboardScore
        List<LeaderboardScore> scores = leaderboardScoreRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int avgScore = scores.isEmpty() ? 0 : (int) Math.round(scores.stream().mapToInt(LeaderboardScore::getPoints).average().orElse(0.0));

        // Aggregated mistakes, moves, hints, undos
        long totalMistakes = games.stream().mapToInt(Game::getMistakes).sum();
        double avgMistakes = totalGames > 0 ? (double) totalMistakes / totalGames : 0.0;

        long totalMoves = 0;
        long totalUndos = 0;
        long totalHints = 0;
        List<Double> accuracies = new ArrayList<>();

        for (Game g : games) {
            long movesCount = moveRepository.countByGameId(g.getId());
            long undoneCount = moveRepository.findByGameIdOrderByMoveNumberAsc(g.getId()).stream()
                    .filter(Move::isUndone).count();
            long hintsCount = hintHistoryRepository.countByGameId(g.getId());

            totalMoves += movesCount;
            totalUndos += undoneCount;
            totalHints += hintsCount;

            long attempts = movesCount + g.getMistakes();
            double acc = attempts > 0 ? (double) movesCount / attempts : 1.0;
            accuracies.add(acc);
        }

        double avgMoves = totalGames > 0 ? (double) totalMoves / totalGames : 0.0;
        double avgUndos = totalGames > 0 ? (double) totalUndos / totalGames : 0.0;
        double avgHints = totalGames > 0 ? (double) totalHints / totalGames : 0.0;
        double avgAccuracy = accuracies.isEmpty() ? 0.0 : accuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Difficulty breakdown
        double hardComp = calculateDifficultyCompletionRate(games, "hard");
        double medComp = calculateDifficultyCompletionRate(games, "medium");
        double easyComp = calculateDifficultyCompletionRate(games, "easy");

        // Recent stats (last 5 games)
        List<Game> recentGames = games.stream().limit(5).toList();
        long recentMoves = 0;
        long recentErrors = 0;
        for (Game rg : recentGames) {
            recentMoves += moveRepository.countByGameId(rg.getId());
            recentErrors += rg.getMistakes();
        }
        long recentAttempts = recentMoves + recentErrors;
        double recentAcc = recentAttempts > 0 ? (double) recentMoves / recentAttempts : 1.0;
        double recentErrRate = recentAttempts > 0 ? (double) recentErrors / recentAttempts : 0.0;

        double totalDurationMinutes = totalDuration / 60.0;
        double movesPerMin = totalDurationMinutes > 0 ? totalMoves / totalDurationMinutes : 0.0;

        // Set entity fields
        // Calculate current streak and best streak
        int currentStreak = 0;
        for (Game g : games) {
            if (g.getStatus() == GameStatus.COMPLETED) {
                currentStreak++;
            } else {
                break;
            }
        }

        int bestStreak = 0;
        int runningStreak = 0;
        List<Game> chronologicalGames = new ArrayList<>(games);
        java.util.Collections.reverse(chronologicalGames);
        for (Game g : chronologicalGames) {
            if (g.getStatus() == GameStatus.COMPLETED) {
                runningStreak++;
                bestStreak = Math.max(bestStreak, runningStreak);
            } else {
                runningStreak = 0;
            }
        }

        stats.setGamesPlayed(totalGames);
        stats.setGamesCompleted(totalCompleted);
        stats.setGamesAbandoned(totalAbandoned);
        stats.setCompletionRate(round(completionRate, 4));
        stats.setAverageTime(avgTime);
        stats.setBestTime(bestTime);
        stats.setAverageScore(avgScore);
        stats.setAverageAccuracy(round(avgAccuracy, 4));
        stats.setAverageMistakes(round(avgMistakes, 2));
        stats.setAverageHints(round(avgHints, 2));
        stats.setAverageUndos(round(avgUndos, 2));
        stats.setAverageMoves(round(avgMoves, 2));
        stats.setHardCompletionRate(round(hardComp, 4));
        stats.setMediumCompletionRate(round(medComp, 4));
        stats.setEasyCompletionRate(round(easyComp, 4));
        stats.setRecentAccuracy(round(recentAcc, 4));
        stats.setRecentErrorRate(round(recentErrRate, 4));
        stats.setAverageMovesPerMinute(round(movesPerMin, 2));
        stats.setCurrentStreak(currentStreak);
        stats.setBestStreak(bestStreak);
        stats.setUpdatedAt(LocalDateTime.now());

        return playerStatisticsRepository.save(stats);
    }

    private double calculateDifficultyCompletionRate(List<Game> games, String diff) {
        List<Game> matched = games.stream()
                .filter(g -> g.getDifficulty() != null && g.getDifficulty().equalsIgnoreCase(diff))
                .toList();
        if (matched.isEmpty()) {
            return 0.0;
        }
        long completed = matched.stream().filter(g -> g.getStatus() == GameStatus.COMPLETED).count();
        return (double) completed / matched.size();
    }

    @Transactional(readOnly = true)
    public PlayerStatisticsDto getPlayerStatistics(Long userId) {
        return playerStatisticsRepository.findByUserId(userId)
                .map(PlayerStatisticsDto::fromEntity)
                .orElseGet(() -> PlayerStatisticsDto.fromEntity(calculateAndSaveStatistics(userId)));
    }

    @Transactional(readOnly = true)
    public List<DifficultyPerformanceDto> getDifficultyPerformance(Long userId) {
        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<DifficultyPerformanceDto> result = new ArrayList<>();

        for (String diff : List.of("Easy", "Medium", "Hard")) {
            List<Game> diffGames = games.stream()
                    .filter(g -> g.getDifficulty() != null && g.getDifficulty().equalsIgnoreCase(diff))
                    .toList();
            int total = diffGames.size();
            int comp = (int) diffGames.stream().filter(g -> g.getStatus() == GameStatus.COMPLETED).count();
            double compRate = total > 0 ? (double) comp / total : 0.0;
            long avgDuration = total > 0 ? (long) diffGames.stream().mapToLong(Game::getElapsedSeconds).average().orElse(0.0) : 0L;

            // Accuracy for this difficulty
            double avgAcc = 0.0;
            if (total > 0) {
                double accSum = 0;
                for (Game g : diffGames) {
                    long moves = moveRepository.countByGameId(g.getId());
                    long attempts = moves + g.getMistakes();
                    accSum += attempts > 0 ? (double) moves / attempts : 1.0;
                }
                avgAcc = accSum / total;
            }

            result.add(new DifficultyPerformanceDto(
                    diff, total, comp, round(compRate, 4), avgDuration, round(avgAcc, 4)
            ));
        }

        return result;
    }

    public SkillPredictionRequestDto buildPredictionPayload(PlayerStatistics stats) {
        if (stats == null) {
            return new SkillPredictionRequestDto(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        return new SkillPredictionRequestDto(
                stats.getGamesPlayed(),
                stats.getCompletionRate(),
                stats.getAverageTime(),
                stats.getAverageScore(),
                stats.getAverageAccuracy(),
                stats.getAverageMistakes(),
                stats.getAverageHints(),
                stats.getAverageUndos(),
                stats.getAverageMoves(),
                stats.getHardCompletionRate(),
                stats.getMediumCompletionRate(),
                stats.getEasyCompletionRate(),
                stats.getRecentAccuracy(),
                stats.getRecentErrorRate(),
                stats.getAverageMovesPerMinute()
        );
    }

    private double round(double value, int places) {
        if (places < 0 || Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        long factor = (long) Math.pow(10, places);
        return (double) Math.round(value * factor) / factor;
    }
}
