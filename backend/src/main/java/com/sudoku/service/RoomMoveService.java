package com.sudoku.service;

import com.sudoku.dto.HintResponse;
import com.sudoku.dto.MoveResponse;
import com.sudoku.dto.RoomMoveRequest;
import com.sudoku.dto.SubmitResponse;
import com.sudoku.dto.RoomMoveResponse;
import com.sudoku.dto.MoveRequest;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.MultiplayerRoom;
import com.sudoku.model.RoomParticipant;
import com.sudoku.model.RoomStatus;
import com.sudoku.model.User;
import com.sudoku.repository.RoomParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Shared moves for cooperative multiplayer rooms.
 *
 * Concurrency model (requirement: no race conditions / conflicting writes):
 * 1. The whole method runs in ONE transaction.
 * 2. The room row is fetched with a pessimistic write lock
 *    (SELECT ... FOR UPDATE), so all moves inside one room are strictly
 *    serialized; different rooms never contend.
 * 3. On top of the lock, clients echo {@code expectedStateVersion}; a stale
 *    version is rejected with 409 so a write based on an outdated board can
 *    never clobber a teammate's newer move (optimistic version check).
 * 4. Move validation itself is the existing game engine
 *    (SudokuValidationService via GameService.applySharedMove): fixed puzzle
 *    cells, Sudoku rules, mistakes, completion detection and move history.
 *
 * On completion, both participants still in the room are recorded
 * (finishedAt + pointsAwarded) and each receives leaderboard points exactly
 * once (idempotent ScoringService + unique constraint on (game_id, user_id)).
 */
@Service
public class RoomMoveService {

    private static final Logger log = LoggerFactory.getLogger(RoomMoveService.class);

    private final RoomService roomService;
    private final RoomParticipantRepository participantRepository;
    private final GameService gameService;
    private final ScoringService scoringService;

    public RoomMoveService(
            RoomService roomService,
            RoomParticipantRepository participantRepository,
            GameService gameService,
            ScoringService scoringService
    ) {
        this.roomService = roomService;
        this.participantRepository = participantRepository;
        this.gameService = gameService;
        this.scoringService = scoringService;
    }

    @Transactional
    public RoomMoveResponse makeMove(String roomCode, User user, RoomMoveRequest request) {
        MultiplayerRoom room = roomService.lockRoom(roomCode);
        roomService.requireActiveExpiry(room);

        RoomParticipant me = roomService.requireMembership(room, user);
        if (!me.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You left this room. Rejoin before moving.");
        }
        me.setLastSeenAt(LocalDateTime.now());
        participantRepository.save(me);

        if (room.getStatus() != RoomStatus.IN_PROGRESS || room.getGame() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room game is not in progress.");
        }

        Game game = room.getGame();
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already completed.");
        }

        // Optimistic version check on top of the row lock: reject moves built
        // from a board state the client has not refreshed since.
        Long expected = request.expectedStateVersion();
        if (expected != null && expected != room.getStateVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Board changed since your last update. Refresh and retry.");
        }

        MoveResponse move = gameService.applySharedMove(
                game.getId(),
                new MoveRequest(request.row(), request.column(), request.value()),
                user
        );

        // Any accepted attempt can mutate shared state (board, mistakes, clock).
        room.bumpVersion();

        if (game.getStatus() == GameStatus.COMPLETED) {
            finalizeRoom(room, game);
        }
        return new RoomMoveResponse(move, room.getStateVersion());
    }

    /**
     * "Submit Solution" for the shared board (parity with the solo game page).
     *
     * An incomplete board comes back with the empty/incorrect cells so either
     * player sees exactly what is missing. When the board is solved the room is
     * completed and both eligible participants are awarded leaderboard points
     * exactly once (same idempotent path as a winning move). Safe to call
     * repeatedly: re-submitting a finished room returns the stored result.
     */
    @Transactional
    public SubmitResponse submit(String roomCode, User user) {
        MultiplayerRoom room = roomService.lockRoom(roomCode);
        roomService.requireActiveExpiry(room);

        RoomParticipant me = roomService.requireMembership(room, user);
        if (!me.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You left this room. Rejoin before submitting.");
        }
        if (room.getGame() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room game is not in progress.");
        }

        Game game = room.getGame();
        if (room.getStatus() == RoomStatus.COMPLETED || game.getStatus() == GameStatus.COMPLETED) {
            // Late re-submit of a finished room: idempotent success, no double-award.
            return SubmitResponse.success(game.getElapsedSeconds(), game.getMistakes(), me.getPointsAwarded());
        }
        if (room.getStatus() != RoomStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room game is not in progress.");
        }

        SubmitResponse result = gameService.evaluateAndComplete(game);
        if (result.isCompleted()) {
            room.bumpVersion();
            finalizeRoom(room, game); // records finishedAt + points for both players
            result.setPointsAwarded(me.getPointsAwarded());
        }
        return result;
    }

    /**
     * Shared hint: runs the existing hint engine on the shared board under the
     * room lock. The hint is recorded in the room's shared hint history, so it
     * is visible to both players and counts against BOTH scores.
     */
    @Transactional
    public HintResponse hint(String roomCode, User user, int level) {
        MultiplayerRoom room = roomService.lockRoom(roomCode);
        roomService.requireActiveExpiry(room);

        RoomParticipant me = roomService.requireMembership(room, user);
        if (!me.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You left this room. Rejoin before requesting hints.");
        }
        if (room.getStatus() != RoomStatus.IN_PROGRESS || room.getGame() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room game is not in progress.");
        }

        HintResponse response = gameService.requestHintForGame(room.getGame(), level);
        if (response != null && response.isAvailable()) {
            room.bumpVersion();
            room.touch();
        }
        return response;
    }

    /**
     * Records both participants' results and awards leaderboard points exactly
     * once per eligible participant (participants still active at completion).
     */
    private void finalizeRoom(MultiplayerRoom room, Game game) {
        room.setStatus(RoomStatus.COMPLETED);
        room.bumpVersion();

        List<RoomParticipant> participants = participantRepository.findByRoomIdOrderByJoinedAtAsc(room.getId());
        for (RoomParticipant participant : participants) {
            if (!participant.isActive() || participant.getFinishedAt() != null) {
                continue; // left before the finish: not eligible / already recorded
            }
            LeaderboardScore score = scoringService.awardScore(game, participant.getUser());
            participant.setFinishedAt(LocalDateTime.now());
            participant.setPointsAwarded(score != null ? score.getPoints() : 0);
            participantRepository.save(participant);
            log.info("Room {} participant {} awarded {} points for game {}",
                    room.getRoomCode(), participant.getUser().getUsername(),
                    participant.getPointsAwarded(), game.getId());
        }
    }
}
