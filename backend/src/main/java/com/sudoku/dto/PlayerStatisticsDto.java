package com.sudoku.dto;

import com.sudoku.model.PlayerStatistics;
import java.time.LocalDateTime;

public record PlayerStatisticsDto(
        Long userId,
        int gamesPlayed,
        int gamesCompleted,
        int gamesAbandoned,
        double completionRate,
        long averageTime,
        long bestTime,
        int averageScore,
        double averageAccuracy,
        double averageMistakes,
        double averageHints,
        double averageUndos,
        double averageMoves,
        double hardCompletionRate,
        double mediumCompletionRate,
        double easyCompletionRate,
        double recentAccuracy,
        double recentErrorRate,
        double averageMovesPerMinute,
        int currentStreak,
        int bestStreak,
        LocalDateTime updatedAt
) {
    public static PlayerStatisticsDto fromEntity(PlayerStatistics stats) {
        if (stats == null) {
            return empty(null);
        }
        return new PlayerStatisticsDto(
                stats.getUserId(),
                stats.getGamesPlayed(),
                stats.getGamesCompleted(),
                stats.getGamesAbandoned(),
                stats.getCompletionRate(),
                stats.getAverageTime(),
                stats.getBestTime(),
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
                stats.getAverageMovesPerMinute(),
                stats.getCurrentStreak(),
                stats.getBestStreak(),
                stats.getUpdatedAt()
        );
    }

    public static PlayerStatisticsDto empty(Long userId) {
        return new PlayerStatisticsDto(
                userId,
                0, 0, 0, 0.0,
                0, 0, 0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0, 0,
                LocalDateTime.now()
        );
    }
}
