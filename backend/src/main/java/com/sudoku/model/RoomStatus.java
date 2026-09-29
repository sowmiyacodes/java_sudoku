package com.sudoku.model;

/**
 * Lifecycle of a cooperative multiplayer room.
 *
 * WAITING     - created, host present (optionally a guest), game not started yet.
 * IN_PROGRESS - host started the game; both participants share one Game.
 * COMPLETED   - shared game solved; participant results and points recorded.
 * ABANDONED   - host left while waiting, or every participant left mid-game.
 * EXPIRED     - room was inactive past its TTL and was swept.
 */
public enum RoomStatus {
    WAITING,
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
    EXPIRED
}
