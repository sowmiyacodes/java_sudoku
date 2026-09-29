package com.sudoku.dto;

import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.SudokuBoard;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class GameResponse {
    private Long id;
    private String puzzleId;
    private String difficulty;
    private Integer difficultyScore;
    private Object metadata;
    private GameStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime completedAt;
    private long elapsedSeconds;
    private int mistakes;
    private int[][] board;
    private int[][] initialBoard;
    private boolean completed;
    private boolean canUndo;
    private boolean canRedo;

    public GameResponse() {}

    public static GameResponse fromGame(Game game, boolean canUndo, boolean canRedo) {
        GameResponse res = new GameResponse();
        res.id = game.getId();
        res.puzzleId = game.getPuzzleId();
        res.difficulty = game.getDifficulty();
        res.difficultyScore = game.getDifficultyScore();
        
        try {
            if (game.getMetadataJson() != null) {
                res.metadata = new ObjectMapper().readValue(game.getMetadataJson(), Object.class);
            }
        } catch (Exception e) {
            // Ignore parse errors
        }

        res.status = game.getStatus();
        res.startedAt = game.getStartedAt();
        res.pausedAt = game.getPausedAt();
        res.completedAt = game.getCompletedAt();
        res.elapsedSeconds = game.getElapsedSeconds();
        res.mistakes = game.getMistakes();
        res.board = SudokuBoard.fromJson(game.getCurrentBoardJson()).getGrid();
        res.initialBoard = SudokuBoard.fromJson(game.getInitialBoardJson()).getGrid();
        res.completed = game.getStatus() == GameStatus.COMPLETED;
        res.canUndo = canUndo;
        res.canRedo = canRedo;
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(Integer difficultyScore) { this.difficultyScore = difficultyScore; }

    public Object getMetadata() { return metadata; }
    public void setMetadata(Object metadata) { this.metadata = metadata; }

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

    public int[][] getBoard() { return board; }
    public void setBoard(int[][] board) { this.board = board; }

    public int[][] getInitialBoard() { return initialBoard; }
    public void setInitialBoard(int[][] initialBoard) { this.initialBoard = initialBoard; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public boolean isCanUndo() { return canUndo; }
    public void setCanUndo(boolean canUndo) { this.canUndo = canUndo; }

    public boolean isCanRedo() { return canRedo; }
    public void setCanRedo(boolean canRedo) { this.canRedo = canRedo; }
}
