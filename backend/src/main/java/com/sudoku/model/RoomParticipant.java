package com.sudoku.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A user's membership in a multiplayer room.
 *
 * One row per (room, user). Leaving sets {@code leftAt} instead of deleting the
 * row so reconnection (re-joining with the same code) can re-attach the player
 * and so completion time can exclude players who left before the finish.
 */
@Entity
@Table(name = "room_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_participants_room_user", columnNames = {"room_id", "user_id"})
})
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private MultiplayerRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private RoomRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /** Leaderboard points awarded for this room's game (null until finished/awarded). */
    @Column(name = "points_awarded")
    private Integer pointsAwarded;

    public RoomParticipant() {}

    public RoomParticipant(MultiplayerRoom room, User user, RoomRole role) {
        this.room = room;
        this.user = user;
        this.role = role;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.joinedAt == null) this.joinedAt = now;
        if (this.lastSeenAt == null) this.lastSeenAt = now;
    }

    public Long getId() { return id; }
    public MultiplayerRoom getRoom() { return room; }
    public void setRoom(MultiplayerRoom room) { this.room = room; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public RoomRole getRole() { return role; }
    public void setRole(RoomRole role) { this.role = role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Integer getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(Integer pointsAwarded) { this.pointsAwarded = pointsAwarded; }

    public boolean isActive() {
        return leftAt == null;
    }
}
