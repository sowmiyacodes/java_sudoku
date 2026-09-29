package com.sudoku.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A cooperative multiplayer room (MVP: two players max) sharing a single Game.
 *
 * Synchronization model (REST polling today, WebSocket-ready later):
 * every state mutation bumps {@link #stateVersion} while holding a pessimistic
 * write lock on this row, so concurrent moves are serialized per room and
 * clients can detect conflicting writes by echoing the last version they saw.
 */
@Entity
@Table(name = "multiplayer_rooms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_multiplayer_rooms_code", columnNames = "room_code")
})
public class MultiplayerRoom {

    /** Rooms expire after this much inactivity (hours). */
    public static final int TTL_HOURS = 6;

    /** Maximum active participants for the MVP. */
    public static final int MAX_PARTICIPANTS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, length = 8, updatable = false)
    private String roomCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoomStatus status = RoomStatus.WAITING;

    @Column(length = 32)
    private String difficulty;

    /**
     * Monotonic state version. Incremented on every accepted move and on
     * lifecycle transitions. Clients echo it back to detect conflicts.
     */
    @Column(name = "state_version", nullable = false)
    private long stateVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    public MultiplayerRoom() {}

    public MultiplayerRoom(String roomCode, User host, String difficulty) {
        this.roomCode = roomCode;
        this.host = host;
        this.difficulty = difficulty;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastActivityAt = now;
    }

    public Long getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public long getStateVersion() { return stateVersion; }
    public void setStateVersion(long stateVersion) { this.stateVersion = stateVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    /** Refreshes the activity timestamp and advances the state version. */
    public void touch() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void bumpVersion() {
        this.stateVersion++;
        this.lastActivityAt = LocalDateTime.now();
    }
}
