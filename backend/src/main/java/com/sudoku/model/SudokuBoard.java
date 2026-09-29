package com.sudoku.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

public class SudokuBoard {
    public static final int SIZE = 9;
    public static final int BOX_SIZE = 3;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final int[][] grid;

    public SudokuBoard() {
        this.grid = new int[SIZE][SIZE];
    }

    public SudokuBoard(int[][] grid) {
        this.grid = new int[SIZE][SIZE];
        if (grid != null) {
            for (int r = 0; r < Math.min(SIZE, grid.length); r++) {
                for (int c = 0; c < Math.min(SIZE, grid[r].length); c++) {
                    this.grid[r][c] = grid[r][c];
                }
            }
        }
    }

    public int getCell(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return grid[row][col];
        }
        return 0;
    }

    public void setCell(int row, int col, int value) {
        if (isValidCoordinate(row, col)) {
            grid[row][col] = value;
        }
    }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public int[][] getGrid() {
        int[][] copy = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, SIZE);
        }
        return copy;
    }

    public boolean isFull() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(grid);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing Sudoku board to JSON", e);
        }
    }

    public static SudokuBoard fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new SudokuBoard();
        }
        try {
            int[][] grid = OBJECT_MAPPER.readValue(json, int[][].class);
            return new SudokuBoard(grid);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing Sudoku board from JSON", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SudokuBoard that = (SudokuBoard) o;
        return Arrays.deepEquals(grid, that.grid);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(grid);
    }
}
