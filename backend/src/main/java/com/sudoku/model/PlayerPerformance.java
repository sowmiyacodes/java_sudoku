package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_performance")
public class PlayerPerformance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "difficulty", length = 32)
    private String difficulty;

    @Column(nullable = false)
    private int accuracy;

    @Column(nullable = false)
    private int mistakes;

    @Column(name = "hints_used", nullable = false)
    private int hintsUsed;

    @Column(name = "moves_made", nullable = false)
    private int movesMade;

    @Column(name = "elapsed_seconds", nullable = false)
    private long elapsedSeconds;

    @Column(name = "predicted_difficulty", length = 32)
    private String predictedDifficulty;

    @Column(name = "model_confidence", nullable = false)
    private double modelConfidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PlayerPerformance() {}

    public PlayerPerformance(Long gameId, String difficulty, int accuracy, int mistakes,
                             int hintsUsed, int movesMade, long elapsedSeconds,
                             String predictedDifficulty, double modelConfidence) {
        this.gameId = gameId;
        this.difficulty = difficulty;
        this.accuracy = accuracy;
        this.mistakes = mistakes;
        this.hintsUsed = hintsUsed;
        this.movesMade = movesMade;
        this.elapsedSeconds = elapsedSeconds;
        this.predictedDifficulty = predictedDifficulty;
        this.modelConfidence = modelConfidence;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getGameId() { return gameId; }
    public String getDifficulty() { return difficulty; }
    public int getAccuracy() { return accuracy; }
    public int getMistakes() { return mistakes; }
    public int getHintsUsed() { return hintsUsed; }
    public int getMovesMade() { return movesMade; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public String getPredictedDifficulty() { return predictedDifficulty; }
    public double getModelConfidence() { return modelConfidence; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
