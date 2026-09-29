package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard_scores", uniqueConstraints = {
        // One score per (game, user): solo games award their single owner once,
        // multiplayer room games award each eligible participant exactly once.
        @UniqueConstraint(name = "uk_leaderboard_game_user", columnNames = {"game_id", "user_id"})
})
public class LeaderboardScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many scores may reference one game: the solo owner plus, for a
    // multiplayer room game, each eligible participant. The per-(game, user)
    // uniqueness is enforced by the table's unique constraint.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int points;

    @Column(length = 32, nullable = false)
    private String difficulty;

    @Column(name = "elapsed_seconds", nullable = false)
    private long elapsedSeconds;

    @Column(nullable = false)
    private int mistakes;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(nullable = false)
    private int accuracy;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LeaderboardScore() {}

    public LeaderboardScore(User user, Game game, int points, String difficulty,
                            long elapsedSeconds, int mistakes, int hintsUsed,
                            int accuracy, LocalDateTime completedAt) {
        this.user = user;
        this.game = game;
        this.points = points;
        this.difficulty = difficulty;
        this.elapsedSeconds = elapsedSeconds;
        this.mistakes = mistakes;
        this.hintsUsed = hintsUsed;
        this.accuracy = accuracy;
        this.completedAt = completedAt != null ? completedAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }

    public void setHintsUsed(int hintsUsed) {
        this.hintsUsed = hintsUsed;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
