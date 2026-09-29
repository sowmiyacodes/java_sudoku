package com.sudoku.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for joining (or reconnecting to) a room by its shareable code. */
public record JoinRoomRequest(
        @NotBlank(message = "Room code is required")
        @Size(max = 8)
        String roomCode
) {}
