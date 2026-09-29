package com.sudoku.dto;

import jakarta.validation.constraints.Size;

/** Request body for creating a multiplayer room. Difficulty is optional (defaults to Medium). */
public record CreateRoomRequest(
        @Size(max = 32) String difficulty
) {}
