package com.sudoku.dto;

public class LeaderboardPlayerStats {
    private Long userId;
    private String username;
    private String displayName;
    private Long totalPoints;
    private Long gamesCompleted;
    private Double averageAccuracy;
    private Long bestTime;

    public LeaderboardPlayerStats(Long userId, String username, String displayName,
                                  Long totalPoints, Long gamesCompleted,
                                  Double averageAccuracy, Long bestTime) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.totalPoints = totalPoints != null ? totalPoints : 0L;
        this.gamesCompleted = gamesCompleted != null ? gamesCompleted : 0L;
        this.averageAccuracy = averageAccuracy != null ? averageAccuracy : 0.0;
        this.bestTime = bestTime != null ? bestTime : 0L;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public Long getTotalPoints() { return totalPoints; }
    public Long getGamesCompleted() { return gamesCompleted; }
    public Double getAverageAccuracy() { return averageAccuracy; }
    public Long getBestTime() { return bestTime; }
}
