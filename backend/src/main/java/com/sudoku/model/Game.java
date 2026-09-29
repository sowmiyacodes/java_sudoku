package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "puzzle_id", length = 64)
    private String puzzleId;

    @Column(name = "difficulty", length = 32)
    private String difficulty;

    @Column(name = "difficulty_score")
    private Integer difficultyScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GameStatus status = GameStatus.IN_PROGRESS;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "elapsed_seconds", nullable = false)
    private long elapsedSeconds = 0;

    @Column(name = "mistakes", nullable = false)
    private int mistakes = 0;

    @Lob
    @Column(name = "initial_board_json", columnDefinition = "TEXT", nullable = false)
    private String initialBoardJson;

    @Lob
    @Column(name = "current_board_json", columnDefinition = "TEXT", nullable = false)
    private String currentBoardJson;

    @Lob
    @Column(name = "solution_board_json", columnDefinition = "TEXT")
    private String solutionBoardJson;

    @Lob
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Game() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(Integer difficultyScore) { this.difficultyScore = difficultyScore; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public long getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(long elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }

    public int getMistakes() { return mistakes; }
    public void setMistakes(int mistakes) { this.mistakes = mistakes; }

    public String getInitialBoardJson() { return initialBoardJson; }
    public void setInitialBoardJson(String initialBoardJson) { this.initialBoardJson = initialBoardJson; }

    public String getCurrentBoardJson() { return currentBoardJson; }
    public void setCurrentBoardJson(String currentBoardJson) { this.currentBoardJson = currentBoardJson; }

    public String getSolutionBoardJson() { return solutionBoardJson; }
    public void setSolutionBoardJson(String solutionBoardJson) { this.solutionBoardJson = solutionBoardJson; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
