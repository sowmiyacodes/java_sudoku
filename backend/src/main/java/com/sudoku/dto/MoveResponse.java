package com.sudoku.dto;

public class MoveResponse {
    private boolean valid;
    private int row;
    private int column;
    private int value;
    private String reason;
    private boolean completed;
    private int mistakes;
    private int[][] board;
    private long elapsedSeconds;
    private boolean canUndo;
    private boolean canRedo;

    public MoveResponse() {}

    public static MoveResponse valid(int row, int column, int value, boolean completed, int mistakes, int[][] board, long elapsedSeconds, boolean canUndo, boolean canRedo) {
        MoveResponse res = new MoveResponse();
        res.valid = true;
        res.row = row;
        res.column = column;
        res.value = value;
        res.completed = completed;
        res.mistakes = mistakes;
        res.board = board;
        res.elapsedSeconds = elapsedSeconds;
        res.canUndo = canUndo;
        res.canRedo = canRedo;
        return res;
    }

    public static MoveResponse invalid(int row, int column, int value, String reason, int mistakes, int[][] board, long elapsedSeconds, boolean canUndo, boolean canRedo) {
        MoveResponse res = new MoveResponse();
        res.valid = false;
        res.row = row;
        res.column = column;
        res.value = value;
        res.reason = reason;
        res.completed = false;
        res.mistakes = mistakes;
        res.board = board;
        res.elapsedSeconds = elapsedSeconds;
        res.canUndo = canUndo;
        res.canRedo = canRedo;
        return res;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getMistakes() {
        return mistakes;
    }

    public void setMistakes(int mistakes) {
        this.mistakes = mistakes;
    }

    public int[][] getBoard() {
        return board;
    }

    public void setBoard(int[][] board) {
        this.board = board;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public boolean isCanUndo() {
        return canUndo;
    }

    public void setCanUndo(boolean canUndo) {
        this.canUndo = canUndo;
    }

    public boolean isCanRedo() {
        return canRedo;
    }

    public void setCanRedo(boolean canRedo) {
        this.canRedo = canRedo;
    }
}
