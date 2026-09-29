package com.sudoku.dto;

public class PerformanceAnalysis {
    private int accuracy;
    private int mistakes;
    private int hintsUsed;
    private int movesMade;
    private long elapsedSeconds;
    private String strongArea;
    private String improvementArea;
    private String recommendation;
    private String predictedDifficulty;
    private double modelConfidence;
    private int historyCount;

    public PerformanceAnalysis() {}

    public PerformanceAnalysis(int accuracy, int mistakes, int hintsUsed, int movesMade,
                               long elapsedSeconds, String strongArea, String improvementArea,
                               String recommendation) {
        this.accuracy = accuracy;
        this.mistakes = mistakes;
        this.hintsUsed = hintsUsed;
        this.movesMade = movesMade;
        this.elapsedSeconds = elapsedSeconds;
        this.strongArea = strongArea;
        this.improvementArea = improvementArea;
        this.recommendation = recommendation;
    }

    public int getAccuracy() { return accuracy; }
    public int getMistakes() { return mistakes; }
    public int getHintsUsed() { return hintsUsed; }
    public int getMovesMade() { return movesMade; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public String getStrongArea() { return strongArea; }
    public String getImprovementArea() { return improvementArea; }
    public String getRecommendation() { return recommendation; }
    public void setPredictedDifficulty(String predictedDifficulty) { this.predictedDifficulty = predictedDifficulty; }
    public String getPredictedDifficulty() { return predictedDifficulty; }
    public void setModelConfidence(double modelConfidence) { this.modelConfidence = modelConfidence; }
    public double getModelConfidence() { return modelConfidence; }
    public void setHistoryCount(int historyCount) { this.historyCount = historyCount; }
    public int getHistoryCount() { return historyCount; }
}