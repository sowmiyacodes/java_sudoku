package com.sudoku.dto;

/**
 * Result of a shared room move: the standard solo {@link MoveResponse} payload
 * plus the room's new state version so the mover can immediately continue
 * without waiting for the next poll.
 */
public record RoomMoveResponse(MoveResponse move, long stateVersion) {}
