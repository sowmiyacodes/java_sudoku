package com.sudoku.dto;

public class PuzzleResponse {
    private String puzzleId;
    private String difficulty;
    private int difficultyScore;
    private int[][] initialBoard;
    private DifficultyAnalysis metadata;

    public PuzzleResponse() {}

    public PuzzleResponse(String puzzleId, String difficulty, int difficultyScore, int[][] initialBoard, DifficultyAnalysis metadata) {
        this.puzzleId = puzzleId;
        this.difficulty = difficulty;
        this.difficultyScore = difficultyScore;
        this.initialBoard = initialBoard;
        this.metadata = metadata;
    }

    public String getPuzzleId() { return puzzleId; }
    public void setPuzzleId(String puzzleId) { this.puzzleId = puzzleId; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getDifficultyScore() { return difficultyScore; }
    public void setDifficultyScore(int difficultyScore) { this.difficultyScore = difficultyScore; }

    public int[][] getInitialBoard() { return initialBoard; }
    public void setInitialBoard(int[][] initialBoard) { this.initialBoard = initialBoard; }

    public DifficultyAnalysis getMetadata() { return metadata; }
    public void setMetadata(DifficultyAnalysis metadata) { this.metadata = metadata; }
}
