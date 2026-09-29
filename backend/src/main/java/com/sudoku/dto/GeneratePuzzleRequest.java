package com.sudoku.dto;

public class GeneratePuzzleRequest {
    private String difficulty;

    public GeneratePuzzleRequest() {}

    public GeneratePuzzleRequest(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
