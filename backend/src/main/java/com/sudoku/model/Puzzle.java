package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "puzzles", uniqueConstraints = @UniqueConstraint(name = "uk_puzzles_puzzle_id", columnNames = "puzzle_id"))
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "puzzle_id", nullable = false, unique = true, length = 64)
    private String puzzleId;

    @Column(nullable = false, length = 81)
    private String puzzle;

    @Column(name = "solution", nullable = false, length = 81)
    private String solution;

    @Column(nullable = false, length = 16)
    private String difficulty;

    @Column(name = "predicted_difficulty", length = 16)
    private String predictedDifficulty;

    @Column(name = "difficulty_score")
    private Integer difficultyScore;

    @Column(name = "model_confidence")
    private Double modelConfidence;

    @Column(length = 100)
    private String source;

    private Double rating;

    @Lob
    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Lob
    @Column(name = "top_factors_json", columnDefinition = "TEXT")
    private String topFactorsJson;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }
    public String getPuzzle() { return puzzle; }
    public void setPuzzle(String puzzle) { this.puzzle = puzzle; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getPredictedDifficulty() { return predictedDifficulty; }
    public void setPredictedDifficulty(String predictedDifficulty) { this.predictedDifficulty = predictedDifficulty; }
    public Integer getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(Integer difficultyScore) { this.difficultyScore = difficultyScore; }
    public Double getModelConfidence() { return modelConfidence; }
    public void setModelConfidence(Double modelConfidence) { this.modelConfidence = modelConfidence; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
    public String getTopFactorsJson() { return topFactorsJson; }
    public void setTopFactorsJson(String topFactorsJson) { this.topFactorsJson = topFactorsJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}