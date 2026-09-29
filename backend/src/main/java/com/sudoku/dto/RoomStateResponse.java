package com.sudoku.dto;

import com.sudoku.model.RoomStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete, self-contained snapshot of a room — the single DTO that REST
 * polling returns today and that a WebSocket upgrade can push unchanged.
 */
public record RoomStateResponse(
        String roomCode,
        RoomStatus status,
        String difficulty,
        String hostUsername,
        String guestUsername,
        long stateVersion,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        RoomPlayerResponse you,
        List<RoomPlayerResponse> players,
        GameResponse game
) {}
