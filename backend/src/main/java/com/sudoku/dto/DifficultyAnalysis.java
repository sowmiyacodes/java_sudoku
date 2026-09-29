package com.sudoku.dto;

import java.util.List;

public class DifficultyAnalysis {
    private String difficulty;
    private int score;
    private int emptyCells;
    private int candidateCount;
    private int solvingSteps;
    private List<String> techniquesUsed;
    private boolean uniqueSolution;

    public DifficultyAnalysis() {}

    public DifficultyAnalysis(String difficulty, int score, int emptyCells, int candidateCount, int solvingSteps, List<String> techniquesUsed, boolean uniqueSolution) {
        this.difficulty = difficulty;
        this.score = score;
        this.emptyCells = emptyCells;
        this.candidateCount = candidateCount;
        this.solvingSteps = solvingSteps;
        this.techniquesUsed = techniquesUsed;
        this.uniqueSolution = uniqueSolution;
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getEmptyCells() { return emptyCells; }
    public void setEmptyCells(int emptyCells) { this.emptyCells = emptyCells; }

    public int getCandidateCount() { return candidateCount; }
    public void setCandidateCount(int candidateCount) { this.candidateCount = candidateCount; }

    public int getSolvingSteps() { return solvingSteps; }
    public void setSolvingSteps(int solvingSteps) { this.solvingSteps = solvingSteps; }

    public List<String> getTechniquesUsed() { return techniquesUsed; }
    public void setTechniquesUsed(List<String> techniquesUsed) { this.techniquesUsed = techniquesUsed; }

    public boolean isUniqueSolution() { return uniqueSolution; }
    public void setUniqueSolution(boolean uniqueSolution) { this.uniqueSolution = uniqueSolution; }
}
