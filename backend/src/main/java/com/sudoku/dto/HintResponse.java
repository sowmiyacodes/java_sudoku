package com.sudoku.dto;

import java.util.List;

public class HintResponse {
    private boolean available;
    private int row;
    private int column;
    private int value;
    private String technique;
    private String explanation;
    private List<Integer> candidates;
    private String message;
    private int level;
    private String hintText;

    public HintResponse() {}

    public static HintResponse notAvailable(String message) {
        HintResponse res = new HintResponse();
        res.available = false;
        res.message = message;
        return res;
    }

    public static HintResponse available(int row, int column, int value, String technique, String explanation, List<Integer> candidates) {
        HintResponse res = new HintResponse();
        res.available = true;
        res.row = row;
        res.column = column;
        res.value = value;
        res.technique = technique;
        res.explanation = explanation;
        res.candidates = candidates;
        return res;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getTechnique() { return technique; }
    public void setTechnique(String technique) { this.technique = technique; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<Integer> getCandidates() { return candidates; }
    public void setCandidates(List<Integer> candidates) { this.candidates = candidates; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getHintText() { return hintText; }
    public void setHintText(String hintText) { this.hintText = hintText; }
}
