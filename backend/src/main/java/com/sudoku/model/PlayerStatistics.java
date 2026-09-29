package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_statistics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_stats_user", columnNames = {"user_id"})
})
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "games_played", nullable = false)
    private int gamesPlayed = 0;

    @Column(name = "games_completed", nullable = false)
    private int gamesCompleted = 0;

    @Column(name = "games_abandoned", nullable = false)
    private int gamesAbandoned = 0;

    @Column(name = "completion_rate", nullable = false)
    private double completionRate = 0.0;

    @Column(name = "average_time", nullable = false)
    private long averageTime = 0;

    @Column(name = "best_time", nullable = false)
    private long bestTime = 0;

    @Column(name = "average_score", nullable = false)
    private int averageScore = 0;

    @Column(name = "average_accuracy", nullable = false)
    private double averageAccuracy = 0.0;

    @Column(name = "average_mistakes", nullable = false)
    private double averageMistakes = 0.0;

    @Column(name = "average_hints", nullable = false)
    private double averageHints = 0.0;

    @Column(name = "average_undos", nullable = false)
    private double averageUndos = 0.0;

    @Column(name = "average_moves", nullable = false)
    private double averageMoves = 0.0;

    @Column(name = "hard_completion_rate", nullable = false)
    private double hardCompletionRate = 0.0;

    @Column(name = "medium_completion_rate", nullable = false)
    private double mediumCompletionRate = 0.0;

    @Column(name = "easy_completion_rate", nullable = false)
    private double easyCompletionRate = 0.0;

    @Column(name = "recent_accuracy", nullable = false)
    private double recentAccuracy = 0.0;

    @Column(name = "recent_error_rate", nullable = false)
    private double recentErrorRate = 0.0;

    @Column(name = "average_moves_per_minute", nullable = false)
    private double averageMovesPerMinute = 0.0;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PlayerStatistics() {}

    public PlayerStatistics(Long userId) {
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getGamesCompleted() { return gamesCompleted; }
    public void setGamesCompleted(int gamesCompleted) { this.gamesCompleted = gamesCompleted; }

    public int getGamesAbandoned() { return gamesAbandoned; }
    public void setGamesAbandoned(int gamesAbandoned) { this.gamesAbandoned = gamesAbandoned; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public long getAverageTime() { return averageTime; }
    public void setAverageTime(long averageTime) { this.averageTime = averageTime; }

    public long getBestTime() { return bestTime; }
    public void setBestTime(long bestTime) { this.bestTime = bestTime; }

    public int getAverageScore() { return averageScore; }
    public void setAverageScore(int averageScore) { this.averageScore = averageScore; }

    public double getAverageAccuracy() { return averageAccuracy; }
    public void setAverageAccuracy(double averageAccuracy) { this.averageAccuracy = averageAccuracy; }

    public double getAverageMistakes() { return averageMistakes; }
    public void setAverageMistakes(double averageMistakes) { this.averageMistakes = averageMistakes; }

    public double getAverageHints() { return averageHints; }
    public void setAverageHints(double averageHints) { this.averageHints = averageHints; }

    public double getAverageUndos() { return averageUndos; }
    public void setAverageUndos(double averageUndos) { this.averageUndos = averageUndos; }

    public double getAverageMoves() { return averageMoves; }
    public void setAverageMoves(double averageMoves) { this.averageMoves = averageMoves; }

    public double getHardCompletionRate() { return hardCompletionRate; }
    public void setHardCompletionRate(double hardCompletionRate) { this.hardCompletionRate = hardCompletionRate; }

    public double getMediumCompletionRate() { return mediumCompletionRate; }
    public void setMediumCompletionRate(double mediumCompletionRate) { this.mediumCompletionRate = mediumCompletionRate; }

    public double getEasyCompletionRate() { return easyCompletionRate; }
    public void setEasyCompletionRate(double easyCompletionRate) { this.easyCompletionRate = easyCompletionRate; }

    public double getRecentAccuracy() { return recentAccuracy; }
    public void setRecentAccuracy(double recentAccuracy) { this.recentAccuracy = recentAccuracy; }

    public double getRecentErrorRate() { return recentErrorRate; }
    public void setRecentErrorRate(double recentErrorRate) { this.recentErrorRate = recentErrorRate; }

    public double getAverageMovesPerMinute() { return averageMovesPerMinute; }
    public void setAverageMovesPerMinute(double averageMovesPerMinute) { this.averageMovesPerMinute = averageMovesPerMinute; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
