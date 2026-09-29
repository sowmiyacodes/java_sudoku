package com.sudoku;

import com.sudoku.dto.CreateRoomRequest;
import com.sudoku.dto.JoinRoomRequest;
import com.sudoku.dto.MoveRequest;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.RoomMoveRequest;
import com.sudoku.dto.RoomMoveResponse;
import com.sudoku.dto.RoomStateResponse;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.RoomStatus;
import com.sudoku.model.SudokuBoard;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.service.GameService;
import com.sudoku.service.RoomMoveService;
import com.sudoku.service.RoomService;
import com.sudoku.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Shared gameplay inside a room: one game for both players, move-validation
 * reuse, access control, version conflicts, simultaneous moves and completion
 * scoring (Stage 3 requirements 4, 6, 7, 9).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-rooms-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class RoomGameplayTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomMoveService roomMoveService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private LeaderboardScoreRepository leaderboardScoreRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private record RoomSetup(String code, Long gameId, User host, User guest) {}

    private User register(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.register(new RegisterRequest(
                prefix + "_" + suffix,
                prefix + " " + suffix,
                prefix + "_" + suffix + "@sudoku.test",
                "Password123!"
        ));
    }

    private RoomSetup startRoom() {
        User host = register("host");
        User guest = register("guest");
        String code = roomService.createRoom(host, new CreateRoomRequest("Easy")).roomCode();
        roomService.joinRoom(guest, new JoinRoomRequest(code).roomCode());
        RoomStateResponse started = roomService.startGame(code, host);
        assertNotNull(started.game(), "Host start must create the shared game");
        return new RoomSetup(code, started.game().getId(), host, guest);
    }

    private ResponseStatusException expectStatus(Runnable action, HttpStatus expected) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, ex.getStatusCode());
        return ex;
    }

    private Game loadGame(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow();
    }

    private int[][] currentBoard(Long gameId) {
        return SudokuBoard.fromJson(loadGame(gameId).getCurrentBoardJson()).getGrid();
    }

    /** Returns {row, col, solutionValue} for up to {@code count} initially-empty cells. */
    private List<int[]> emptySolutionCells(Game game, int count) {
        int[][] initial = SudokuBoard.fromJson(game.getInitialBoardJson()).getGrid();
        int[][] solution = SudokuBoard.fromJson(game.getSolutionBoardJson()).getGrid();
        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < 9 && cells.size() < count; r++) {
            for (int c = 0; c < 9 && cells.size() < count; c++) {
                if (initial[r][c] == 0) {
                    cells.add(new int[]{r, c, solution[r][c]});
                }
            }
        }
        return cells;
    }

    /** Finds an empty cell in a row that already has a clue, so placing that clue value violates the row. */
    private int[] findConstraintViolationCell(Game game) {
        int[][] initial = SudokuBoard.fromJson(game.getInitialBoardJson()).getGrid();
        for (int r = 0; r < 9; r++) {
            int clueValue = 0;
            for (int c = 0; c < 9; c++) {
                if (initial[r][c] != 0) {
                    clueValue = initial[r][c];
                    break;
                }
            }
            if (clueValue == 0) continue;
            for (int c = 0; c < 9; c++) {
                if (initial[r][c] == 0) {
                    return new int[]{r, c, clueValue};
                }
            }
        }
        return null;
    }

    private int[] firstFixedCell(Game game) {
        int[][] initial = SudokuBoard.fromJson(game.getInitialBoardJson()).getGrid();
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (initial[r][c] != 0) {
                    return new int[]{r, c, initial[r][c]};
                }
            }
        }
        throw new IllegalStateException("Puzzle has no fixed cells");
    }

    @Test
    @DisplayName("Host and guest play the same shared game created by the existing puzzle service")
    void testSharedGameSingleInstance() {
        RoomSetup setup = startRoom();

        RoomStateResponse hostView = roomService.getRoomState(setup.code(), setup.host());
        RoomStateResponse guestView = roomService.getRoomState(setup.code(), setup.guest());
        assertEquals(setup.gameId(), hostView.game().getId());
        assertEquals(setup.gameId(), guestView.game().getId());
        assertArrayEquals(hostView.game().getBoard(), guestView.game().getBoard());
        assertEquals(GameStatus.IN_PROGRESS, loadGame(setup.gameId()).getStatus());

        // The shared game must not appear in solo "Continue Game"...
        assertTrue(gameService.getLatestResumableGame(setup.host()).isEmpty(),
                "Room games must be excluded from solo resume");
        // ...and the solo move endpoint must refuse it (it would bypass the room lock).
        expectStatus(() -> gameService.makeMove(setup.gameId(),
                new MoveRequest(0, 0, 1), setup.host()), HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Moves from both players sync into one board with shared mistakes")
    void testSharedMovesSynchronize() {
        RoomSetup setup = startRoom();

        // Host plays a correct move.
        int[] hostCell = emptySolutionCells(loadGame(setup.gameId()), 1).get(0);
        RoomMoveResponse hostMove = roomMoveService.makeMove(setup.code(), setup.host(),
                new RoomMoveRequest(hostCell[0], hostCell[1], hostCell[2], null));
        assertTrue(hostMove.move().isValid());

        // Guest sees the host's cell filled in the polled state.
        RoomStateResponse guestView = roomService.getRoomState(setup.code(), setup.guest());
        assertEquals(hostCell[2], guestView.game().getBoard()[hostCell[0]][hostCell[1]]);

        // Guest plays a correct move; host sees it.
        int[] guestCell = emptySolutionCells(loadGame(setup.gameId()), 2).get(1);
        RoomMoveResponse guestMove = roomMoveService.makeMove(setup.code(), setup.guest(),
                new RoomMoveRequest(guestCell[0], guestCell[1], guestCell[2], null));
        assertTrue(guestMove.move().isValid());
        RoomStateResponse hostView = roomService.getRoomState(setup.code(), setup.host());
        assertEquals(guestCell[2], hostView.game().getBoard()[guestCell[0]][guestCell[1]]);

        // A rule-breaking move increments the SHARED mistake counter for both players.
        int[] bad = findConstraintViolationCell(loadGame(setup.gameId()));
        assertNotNull(bad, "Easy puzzle must contain a row with a clue and an empty cell");
        RoomMoveResponse invalid = roomMoveService.makeMove(setup.code(), setup.host(),
                new RoomMoveRequest(bad[0], bad[1], bad[2], null));
        assertFalse(invalid.move().isValid());
        assertEquals(1, invalid.move().getMistakes());
        assertEquals(1, roomService.getRoomState(setup.code(), setup.guest()).game().getMistakes());
    }

    @Test
    @DisplayName("Non-members are rejected and fixed puzzle cells cannot be overwritten")
    void testMoveAccessControlAndFixedCells() {
        RoomSetup setup = startRoom();
        User outsider = register("outsider");

        // Non-members: no state, no moves, no start, no leave.
        expectStatus(() -> roomService.getRoomState(setup.code(), outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> roomMoveService.makeMove(setup.code(), outsider,
                new RoomMoveRequest(0, 0, 1, null)), HttpStatus.FORBIDDEN);
        expectStatus(() -> roomService.startGame(setup.code(), outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> roomService.leaveRoom(setup.code(), outsider), HttpStatus.FORBIDDEN);

        // Overwriting a fixed (clue) cell is rejected by the existing engine.
        int[] fixed = firstFixedCell(loadGame(setup.gameId()));
        RoomMoveResponse rejected = roomMoveService.makeMove(setup.code(), setup.guest(),
                new RoomMoveRequest(fixed[0], fixed[1], (fixed[2] % 9) + 1, null));
        assertFalse(rejected.move().isValid());
        assertNotNull(rejected.move().getReason());
        assertTrue(rejected.move().getReason().contains("original puzzle cell"));
        assertEquals(fixed[2], currentBoard(setup.gameId())[fixed[0]][fixed[1]],
                "Fixed clue must remain unchanged");
    }

    @Test
    @DisplayName("Stale writes are rejected: an outdated state version yields 409 and changes nothing")
    void testVersionConflictDetection() {
        RoomSetup setup = startRoom();
        RoomStateResponse state = roomService.getRoomState(setup.code(), setup.host());
        long version = state.stateVersion();

        int[] first = emptySolutionCells(loadGame(setup.gameId()), 1).get(0);
        RoomMoveResponse ok = roomMoveService.makeMove(setup.code(), setup.host(),
                new RoomMoveRequest(first[0], first[1], first[2], version));
        assertEquals(version + 1, ok.stateVersion());

        // Guest still holds the pre-move version -> conflict, nothing written.
        int[] second = emptySolutionCells(loadGame(setup.gameId()), 2).get(1);
        expectStatus(() -> roomMoveService.makeMove(setup.code(), setup.guest(),
                new RoomMoveRequest(second[0], second[1], second[2], version)), HttpStatus.CONFLICT);
        assertEquals(0, currentBoard(setup.gameId())[second[0]][second[1]],
                "Conflicting write must not touch the board");
    }

    @Test
    @DisplayName("Simultaneous moves are serialized: exactly one applies, the other conflicts")
    void testSimultaneousMovesSerialized() throws Exception {
        RoomSetup setup = startRoom();
        List<int[]> cells = emptySolutionCells(loadGame(setup.gameId()), 2);
        long version = roomService.getRoomState(setup.code(), setup.host()).stateVersion();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        Callable<String> hostAttempt = () -> {
            startGate.await();
            try {
                roomMoveService.makeMove(setup.code(), setup.host(),
                        new RoomMoveRequest(cells.get(0)[0], cells.get(0)[1], cells.get(0)[2], version));
                return "success";
            } catch (ResponseStatusException ex) {
                return "status:" + ex.getStatusCode().value();
            }
        };
        Callable<String> guestAttempt = () -> {
            startGate.await();
            try {
                roomMoveService.makeMove(setup.code(), setup.guest(),
                        new RoomMoveRequest(cells.get(1)[0], cells.get(1)[1], cells.get(1)[2], version));
                return "success";
            } catch (ResponseStatusException ex) {
                return "status:" + ex.getStatusCode().value();
            }
        };
        try {
            Future<String> first = pool.submit(hostAttempt);
            Future<String> second = pool.submit(guestAttempt);
            startGate.countDown();
            List<String> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));

            assertEquals(1, results.stream().filter("success"::equals).count(),
                    "Exactly one concurrent move must succeed: " + results);
            assertEquals(1, results.stream().filter("status:409"::equals).count(),
                    "The conflicting move must be rejected with 409: " + results);

            int[][] board = currentBoard(setup.gameId());
            int filled = (board[cells.get(0)[0]][cells.get(0)[1]] != 0 ? 1 : 0)
                    + (board[cells.get(1)[0]][cells.get(1)[1]] != 0 ? 1 : 0);
            assertEquals(1, filled, "Only the winning move may be applied");
            assertEquals(version + 1, roomService.getRoomState(setup.code(), setup.host()).stateVersion(),
                    "Version must advance exactly once for one applied move");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Completion records both participants and awards leaderboard points exactly once")
    void testCompletionAwardsBothParticipantsOnce() {
        RoomSetup setup = startRoom();
        User host = setup.host();
        User guest = setup.guest();

        // Complete the puzzle, alternating players.
        List<int[]> cells = emptySolutionCells(loadGame(setup.gameId()), 81);
        assertFalse(cells.isEmpty());
        boolean hostTurn = true;
        for (int[] cell : cells) {
            User mover = hostTurn ? host : guest;
            RoomMoveResponse response = roomMoveService.makeMove(setup.code(), mover,
                    new RoomMoveRequest(cell[0], cell[1], cell[2], null));
            assertTrue(response.move().isValid(), "Solution moves must stay valid");
            hostTurn = !hostTurn;
        }

        // Room and game both completed.
        RoomStateResponse finalState = roomService.getRoomState(setup.code(), host);
        assertEquals(RoomStatus.COMPLETED, finalState.status());
        assertTrue(finalState.game().isCompleted());

        // Both participants recorded results with positive points.
        finalState.players().forEach(player -> {
            assertTrue(player.active());
            assertNotNull(player.finishedAt(), "Every eligible participant must be recorded");
            assertNotNull(player.pointsAwarded());
            assertTrue(player.pointsAwarded() > 0, "Points must be awarded to " + player.username());
        });

        // Exactly one leaderboard score per participant: two rows for this game.
        Long scoresForGame = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_scores WHERE game_id = ?",
                Long.class, setup.gameId());
        assertEquals(2L, scoresForGame);
        assertTrue(leaderboardScoreRepository.existsByGameIdAndUserId(setup.gameId(), host.getId()));
        assertTrue(leaderboardScoreRepository.existsByGameIdAndUserId(setup.gameId(), guest.getId()));

        // Further moves on the completed game are rejected with 409.
        expectStatus(() -> roomMoveService.makeMove(setup.code(), guest,
                new RoomMoveRequest(0, 0, 5, null)), HttpStatus.CONFLICT);

        // Solo endpoints refuse to touch the shared room game entirely.
        expectStatus(() -> gameService.makeMove(setup.gameId(), new MoveRequest(0, 0, 5), host),
                HttpStatus.CONFLICT);
        expectStatus(() -> gameService.submitGame(setup.gameId(), host), HttpStatus.CONFLICT);
    }
}
