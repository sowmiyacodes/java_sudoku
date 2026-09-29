package com.sudoku.dto;

public class CreateGameRequest {
    private String difficulty;
    private String puzzleId;
    private int[][] initialBoard;
    private int[][] solutionBoard;

    public CreateGameRequest() {}

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getPuzzleId() {
        return puzzleId;
    }

    public void setPuzzleId(String puzzleId) {
        this.puzzleId = puzzleId;
    }

    public int[][] getInitialBoard() {
        return initialBoard;
    }

    public void setInitialBoard(int[][] initialBoard) {
        this.initialBoard = initialBoard;
    }

    public int[][] getSolutionBoard() {
        return solutionBoard;
    }

    public void setSolutionBoard(int[][] solutionBoard) {
        this.solutionBoard = solutionBoard;
    }
}
