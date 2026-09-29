package com.sudoku.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A shared move inside a multiplayer room.
 *
 * {@code expectedStateVersion} is the last state version the client saw.
 * If it no longer matches the room's current version the move is rejected with
 * 409 so stale writes can never clobber a teammate's newer move. When null the
 * version check is skipped (used by tests and lenient clients).
 */
public record RoomMoveRequest(
        @NotNull @Min(0) @Max(8) Integer row,
        @NotNull @Min(0) @Max(8) Integer column,
        @NotNull @Min(0) @Max(9) Integer value,
        Long expectedStateVersion
) {}
