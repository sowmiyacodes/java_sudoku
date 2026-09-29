package com.sudoku.dto;

import com.sudoku.model.HintHistory;
import java.time.LocalDateTime;

public class HintHistoryResponse {
    private Long id;
    private int row;
    private int column;
    private int value;
    private String technique;
    private String explanation;
    private LocalDateTime createdAt;

    public HintHistoryResponse() {}

    public HintHistoryResponse(Long id, int row, int column, int value, String technique, String explanation, LocalDateTime createdAt) {
        this.id = id;
        this.row = row;
        this.column = column;
        this.value = value;
        this.technique = technique;
        this.explanation = explanation;
        this.createdAt = createdAt;
    }

    public static HintHistoryResponse fromEntity(HintHistory entity) {
        if (entity == null) return null;
        return new HintHistoryResponse(
                entity.getId(),
                entity.getRow(),
                entity.getColumn(),
                entity.getValue(),
                entity.getTechnique(),
                entity.getExplanation(),
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTechnique() {
        return technique;
    }

    public void setTechnique(String technique) {
        this.technique = technique;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
