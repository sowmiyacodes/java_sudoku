package com.sudoku.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "moves")
public class Move {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "row_index", nullable = false)
    private int row;

    @Column(name = "column_index", nullable = false)
    private int column;

    @Column(name = "cell_value", nullable = false)
    private int value;

    @Column(name = "previous_cell_value", nullable = false)
    private int previousValue;

    @Column(name = "move_number", nullable = false)
    private int moveNumber;

    @Column(name = "is_undone", nullable = false)
    private boolean undone = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Move() {}

    public Move(Long gameId, int row, int column, int value, int previousValue, int moveNumber) {
        this.gameId = gameId;
        this.row = row;
        this.column = column;
        this.value = value;
        this.previousValue = previousValue;
        this.moveNumber = moveNumber;
        this.undone = false;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
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

    public int getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(int previousValue) {
        this.previousValue = previousValue;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public boolean isUndone() {
        return undone;
    }

    public void setUndone(boolean undone) {
        this.undone = undone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
