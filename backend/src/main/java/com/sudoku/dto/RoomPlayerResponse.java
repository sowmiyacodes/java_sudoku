package com.sudoku.dto;

import com.sudoku.model.RoomParticipant;
import com.sudoku.model.RoomRole;

import java.time.LocalDateTime;

/** Public view of one participant slot (no private account details). */
public record RoomPlayerResponse(
        Long userId,
        String username,
        String displayName,
        RoomRole role,
        boolean active,
        boolean connected,
        LocalDateTime joinedAt,
        LocalDateTime finishedAt,
        Integer pointsAwarded
) {
    /** Seconds after which a participant with no polls is shown as disconnected. */
    public static final int CONNECTION_TIMEOUT_SECONDS = 30;

    public static RoomPlayerResponse from(RoomParticipant participant, LocalDateTime now) {
        boolean active = participant.isActive();
        boolean connected = active
                && participant.getLastSeenAt() != null
                && participant.getLastSeenAt().isAfter(now.minusSeconds(CONNECTION_TIMEOUT_SECONDS));
        return new RoomPlayerResponse(
                participant.getUser().getId(),
                participant.getUser().getUsername(),
                participant.getUser().getDisplayName(),
                participant.getRole(),
                active,
                connected,
                participant.getJoinedAt(),
                participant.getFinishedAt(),
                participant.getPointsAwarded()
        );
    }

    /** Convenience overload using the current time. */
    public static RoomPlayerResponse from(RoomParticipant participant) {
        return from(participant, LocalDateTime.now());
    }
}
