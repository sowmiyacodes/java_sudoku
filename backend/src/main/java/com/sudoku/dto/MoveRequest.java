package com.sudoku.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class MoveRequest {

    @NotNull(message = "Row cannot be null")
    @Min(value = 0, message = "Row must be between 0 and 8")
    @Max(value = 8, message = "Row must be between 0 and 8")
    private Integer row;

    @NotNull(message = "Column cannot be null")
    @Min(value = 0, message = "Column must be between 0 and 8")
    @Max(value = 8, message = "Column must be between 0 and 8")
    private Integer column;

    @NotNull(message = "Value cannot be null")
    @Min(value = 0, message = "Value must be between 0 and 9 (0 represents empty/erase)")
    @Max(value = 9, message = "Value must be between 0 and 9")
    private Integer value;

    public MoveRequest() {}

    public MoveRequest(Integer row, Integer column, Integer value) {
        this.row = row;
        this.column = column;
        this.value = value;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
