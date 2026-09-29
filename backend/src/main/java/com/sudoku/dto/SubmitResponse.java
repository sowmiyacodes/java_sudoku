package com.sudoku.dto;

import java.util.ArrayList;
import java.util.List;

public class SubmitResponse {
    private boolean completed;
    private boolean valid;
    private String message;
    private int mistakes;
    private int emptyCells;
    private long elapsedSeconds;
    private Integer pointsAwarded;
    private List<CellPosition> incorrectCells = new ArrayList<>();

    public SubmitResponse() {}

    public static SubmitResponse success(long elapsedSeconds, int mistakes) {
        return success(elapsedSeconds, mistakes, null);
    }

    public static SubmitResponse success(long elapsedSeconds, int mistakes, Integer pointsAwarded) {
        SubmitResponse res = new SubmitResponse();
        res.completed = true;
        res.valid = true;
        res.emptyCells = 0;
        res.message = "Sudoku successfully completed!";
        res.elapsedSeconds = elapsedSeconds;
        res.mistakes = mistakes;
        res.pointsAwarded = pointsAwarded;
        return res;
    }

    public static SubmitResponse incomplete(int emptyCount, List<CellPosition> incorrectCells, int mistakes, long elapsedSeconds) {
        SubmitResponse res = new SubmitResponse();
        res.completed = false;
        res.valid = false;
        res.emptyCells = emptyCount;
        res.message = emptyCount > 0 ? "The board has " + emptyCount + " empty cell(s) remaining." : "The board contains errors.";
        res.incorrectCells = incorrectCells != null ? incorrectCells : new ArrayList<>();
        res.mistakes = mistakes;
        res.elapsedSeconds = elapsedSeconds;
        return res;
    }

    public int getEmptyCells() {
        return emptyCells;
    }

    public void setEmptyCells(int emptyCells) {
        this.emptyCells = emptyCells;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public List<CellPosition> getIncorrectCells() {
        return incorrectCells;
    }

    public void setIncorrectCells(List<CellPosition> incorrectCells) {
        this.incorrectCells = incorrectCells;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }
}
