package com.sudoku.dto;

public record LeaderboardEntryResponse(
        int rank,
        Long userId,
        String username,
        String displayName,
        long totalPoints,
        long gamesCompleted,
        double averageAccuracy,
        long bestTime
) {
    public static LeaderboardEntryResponse from(int rank, LeaderboardPlayerStats stats) {
        return new LeaderboardEntryResponse(
                rank,
                stats.getUserId(),
                stats.getUsername(),
                stats.getDisplayName(),
                stats.getTotalPoints(),
                stats.getGamesCompleted(),
                stats.getAverageAccuracy(),
                stats.getBestTime()
        );
    }
}
