package com.sudoku.service;

import com.sudoku.dto.CreateGameRequest;
import com.sudoku.dto.CreateRoomRequest;
import com.sudoku.dto.GameResponse;
import com.sudoku.dto.RoomPlayerResponse;
import com.sudoku.dto.RoomStateResponse;
import com.sudoku.model.Game;
import com.sudoku.model.MultiplayerRoom;
import com.sudoku.model.RoomParticipant;
import com.sudoku.model.RoomRole;
import com.sudoku.model.RoomStatus;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.MultiplayerRoomRepository;
import com.sudoku.repository.RoomParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Multiplayer room lifecycle and access control.
 *
 * Authorization model: room state (including the shared game board) is visible
 * and actionable only for users holding a RoomParticipant row for that room.
 * Mutations (join / start / leave / state touches) run in one transaction;
 * move-time serialization is handled by RoomMoveService via a pessimistic lock
 * on the room row.
 *
 * Lifecycle rules (MVP, two players max):
 * - create  -> WAITING with the creator as HOST participant
 * - join    -> adds a GUEST while WAITING; existing participants re-attach
 *              (reconnection); 404 invalid code, 409 full/closed, 410 expired
 * - start   -> HOST only; generates one shared Game via the existing puzzle
 *              service and flips the room to IN_PROGRESS
 * - leave   -> marks the participant left; host leaving while WAITING abandons
 *              the room; if everyone leaves mid-game the room is ABANDONED
 * - expiry  -> rooms idle for more than {@link MultiplayerRoom#TTL_HOURS}
 *              become EXPIRED (lazily on access, swept on create/join)
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    /** Room codes use an unambiguous alphabet (no I, O, 0, 1). */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MultiplayerRoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final GameRepository gameRepository;
    private final GameService gameService;

    public RoomService(
            MultiplayerRoomRepository roomRepository,
            RoomParticipantRepository participantRepository,
            GameRepository gameRepository,
            GameService gameService
    ) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.gameRepository = gameRepository;
        this.gameService = gameService;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Transactional
    public RoomStateResponse createRoom(User host, CreateRoomRequest request) {
        if (host == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to create a room.");
        }
        expireStaleRooms();

        String code = generateUniqueCode();
        String difficulty = normalizeDifficulty(request != null ? request.difficulty() : null);
        MultiplayerRoom room = roomRepository.save(new MultiplayerRoom(code, host, difficulty));
        participantRepository.save(new RoomParticipant(room, host, RoomRole.HOST));
        log.info("Room {} created by {} ({})", code, host.getUsername(), difficulty);
        return buildState(room, host);
    }

    @Transactional
    public RoomStateResponse joinRoom(User user, String roomCode) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to join a room.");
        }
        expireStaleRooms();

        MultiplayerRoom room = findRoom(roomCode);
        Optional<RoomParticipant> existing = participantRepository.findByRoomIdAndUserId(room.getId(), user.getId());
        boolean expired = applyExpiry(room);

        if (existing.isPresent()) {
            // Reconnection path: an existing member may always re-attach and see
            // the room's current status (IN_PROGRESS, ABANDONED, EXPIRED, ...).
            RoomParticipant participant = existing.get();
            if (!participant.isActive()) {
                // Re-attaching a departed participant consumes a player slot again,
                // so the capacity cap must still hold (MVP: two players max).
                long active = participantRepository.countByRoomIdAndLeftAtIsNull(room.getId());
                if (active >= MultiplayerRoom.MAX_PARTICIPANTS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full.");
                }
                participant.setLeftAt(null);
            }
            participant.setLastSeenAt(LocalDateTime.now());
            participantRepository.save(participant);
            if (!expired) {
                room.touch();
            }
            return buildState(room, user);
        }

        if (expired) {
            throw new ResponseStatusException(HttpStatus.GONE, "This room has expired.");
        }
        return switch (room.getStatus()) {
            case WAITING -> {
                long active = participantRepository.countByRoomIdAndLeftAtIsNull(room.getId());
                if (active >= MultiplayerRoom.MAX_PARTICIPANTS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is full.");
                }
                participantRepository.save(new RoomParticipant(room, user, RoomRole.GUEST));
                room.touch();
                log.info("User {} joined room {}", user.getUsername(), room.getRoomCode());
                yield buildState(room, user);
            }
            case IN_PROGRESS -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Room has already started.");
            case COMPLETED -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Room has already finished.");
            case ABANDONED -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is no longer available.");
            case EXPIRED -> throw new ResponseStatusException(HttpStatus.GONE, "This room has expired.");
        };
    }

    @Transactional
    public RoomStateResponse startGame(String roomCode, User user) {
        MultiplayerRoom room = lockRoom(roomCode);
        RoomParticipant me = requireMembership(room, user);
        requireActiveExpiry(room);
        if (me.getRole() != RoomRole.HOST) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game.");
        }
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room cannot be started (status=" + room.getStatus() + ").");
        }

        // Reuse the existing game engine + puzzle generation service.
        CreateGameRequest createRequest = new CreateGameRequest();
        createRequest.setDifficulty(room.getDifficulty());
        GameResponse created = gameService.createGame(createRequest, room.getHost());
        Game game = gameRepository.findById(created.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create room game."));

        room.setGame(game);
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.bumpVersion();
        log.info("Room {} started game {} ({})", room.getRoomCode(), game.getId(), room.getDifficulty());
        return buildState(room, user);
    }

    @Transactional
    public void leaveRoom(String roomCode, User user) {
        MultiplayerRoom room = lockRoom(roomCode);
        RoomParticipant me = requireMembership(room, user);
        requireActiveExpiry(room);

        me.setLeftAt(LocalDateTime.now());
        me.setLastSeenAt(LocalDateTime.now());
        participantRepository.save(me);
        room.bumpVersion();

        long active = participantRepository.countByRoomIdAndLeftAtIsNull(room.getId());
        if (active == 0 && (room.getStatus() == RoomStatus.WAITING || room.getStatus() == RoomStatus.IN_PROGRESS)) {
            // Everyone left: the room is abandoned.
            room.setStatus(RoomStatus.ABANDONED);
        } else if (room.getStatus() == RoomStatus.WAITING && me.getRole() == RoomRole.HOST) {
            // The host abandoned a room that never started; the guest cannot begin alone.
            room.setStatus(RoomStatus.ABANDONED);
        }
        log.info("User {} left room {} (status={})", user.getUsername(), room.getRoomCode(), room.getStatus());
    }

    /**
     * Room state for polling. Members only; each poll refreshes the room's
     * activity and the caller's presence so actively-polled rooms never expire.
     */
    @Transactional
    public RoomStateResponse getRoomState(String roomCode, User user) {
        MultiplayerRoom room = findRoom(roomCode);
        RoomParticipant me = requireMembership(room, user);
        boolean expired = applyExpiry(room);

        if (!expired) {
            room.touch();
            me.setLastSeenAt(LocalDateTime.now());
            participantRepository.save(me);
            if (room.getStatus() == RoomStatus.IN_PROGRESS && room.getGame() != null) {
                gameService.updateLiveElapsedTime(room.getGame());
            }
        }
        return buildState(room, user);
    }

    // ------------------------------------------------------------------
    // Shared helpers (used by RoomMoveService as well)
    // ------------------------------------------------------------------

    /** Finds a room by code (case-insensitive) or throws 404. */
    MultiplayerRoom findRoom(String roomCode) {
        String normalized = roomCode != null ? roomCode.trim().toUpperCase() : "";
        return roomRepository.findByRoomCode(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomCode));
    }

    /** Finds a room while holding a pessimistic write lock, or throws 404. */
    MultiplayerRoom lockRoom(String roomCode) {
        String normalized = roomCode != null ? roomCode.trim().toUpperCase() : "";
        return roomRepository.findByRoomCodeForUpdate(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomCode));
    }

    /** Verifies the caller belongs to the room (401/403 otherwise). */
    RoomParticipant requireMembership(MultiplayerRoom room, User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return participantRepository.findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room."));
    }

    /**
     * Marks the room EXPIRED when it has been idle past its TTL.
     * @return true when the room is (now) expired
     */
    boolean applyExpiry(MultiplayerRoom room) {
        if ((room.getStatus() == RoomStatus.WAITING || room.getStatus() == RoomStatus.IN_PROGRESS)
                && room.getLastActivityAt() != null
                && room.getLastActivityAt().isBefore(LocalDateTime.now().minusHours(MultiplayerRoom.TTL_HOURS))) {
            room.setStatus(RoomStatus.EXPIRED);
            room.bumpVersion();
            log.info("Room {} expired", room.getRoomCode());
            return true;
        }
        return room.getStatus() == RoomStatus.EXPIRED;
    }

    /** Applies expiry and throws 410 when the room is no longer playable. */
    void requireActiveExpiry(MultiplayerRoom room) {
        if (applyExpiry(room)) {
            throw new ResponseStatusException(HttpStatus.GONE, "This room has expired.");
        }
    }

    /** Builds the complete polled state snapshot for a member. */
    RoomStateResponse buildState(MultiplayerRoom room, User viewer) {
        List<RoomParticipant> participants = participantRepository.findByRoomIdOrderByJoinedAtAsc(room.getId());
        LocalDateTime now = LocalDateTime.now();

        List<RoomPlayerResponse> players = participants.stream()
                .map(p -> RoomPlayerResponse.from(p, now))
                .toList();
        RoomPlayerResponse you = participants.stream()
                .filter(p -> p.getUser().getId().equals(viewer.getId()))
                .findFirst()
                .map(p -> RoomPlayerResponse.from(p, now))
                .orElse(null);
        String guestUsername = participants.stream()
                .filter(p -> p.getRole() == RoomRole.GUEST)
                .findFirst()
                .map(p -> p.getUser().getUsername())
                .orElse(null);
        GameResponse game = room.getGame() != null
                ? GameResponse.fromGame(room.getGame(), false, false)
                : null;

        return new RoomStateResponse(
                room.getRoomCode(),
                room.getStatus(),
                room.getDifficulty(),
                room.getHost().getUsername(),
                guestUsername,
                room.getStateVersion(),
                room.getCreatedAt(),
                room.getLastActivityAt(),
                you,
                players,
                game
        );
    }

    /** Sweeps idle rooms so abandoned rooms do not accumulate as active. */
    private void expireStaleRooms() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(MultiplayerRoom.TTL_HOURS);
        for (MultiplayerRoom stale : roomRepository.findStaleRooms(
                List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS), cutoff)) {
            stale.setStatus(RoomStatus.EXPIRED);
            roomRepository.save(stale);
        }
    }

    private String generateUniqueCode() {
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        String code = builder.toString();
        return roomRepository.existsByRoomCode(code) ? generateUniqueCode() : code;
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "Medium";
        return switch (difficulty.trim().toLowerCase()) {
            case "easy" -> "Easy";
            case "hard" -> "Hard";
            case "expert" -> "Expert";
            default -> "Medium";
        };
    }
}
