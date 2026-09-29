package com.sudoku;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.dto.CreateRoomRequest;
import com.sudoku.dto.HintResponse;
import com.sudoku.dto.JoinRoomRequest;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.RoomMoveRequest;
import com.sudoku.dto.RoomMoveResponse;
import com.sudoku.dto.RoomStateResponse;
import com.sudoku.dto.SubmitResponse;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.RoomStatus;
import com.sudoku.model.SudokuBoard;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.HintHistoryRepository;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Room controls parity with the solo game page: Submit Solution and Hint on
 * the shared board, shared hint history, and room-member read access to the
 * shared game's analysis/history (Stage 3 follow-up).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-rooms-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class RoomControlsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private HintHistoryRepository hintHistoryRepository;

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

    /** Completes the shared puzzle through room moves, alternating players. */
    private void solveRoom(RoomSetup setup) {
        List<int[]> cells = emptySolutionCells(loadGame(setup.gameId()), 81);
        assertFalse(cells.isEmpty());
        boolean hostTurn = true;
        for (int[] cell : cells) {
            User mover = hostTurn ? setup.host() : setup.guest();
            RoomMoveResponse response = roomMoveService.makeMove(setup.code(), mover,
                    new RoomMoveRequest(cell[0], cell[1], cell[2], null));
            assertTrue(response.move().isValid(), "Solution moves must stay valid");
            hostTurn = !hostTurn;
        }
    }

    @Test
    @DisplayName("Submit gives the solo-style feedback on an incomplete shared board")
    void testSubmitFeedbackOnIncompleteBoard() {
        RoomSetup setup = startRoom();
        int totalEmpty = emptySolutionCells(loadGame(setup.gameId()), 81).size();

        // Make one correct move so the submit result must account for it.
        int[] cell = emptySolutionCells(loadGame(setup.gameId()), 1).get(0);
        roomMoveService.makeMove(setup.code(), setup.guest(),
                new RoomMoveRequest(cell[0], cell[1], cell[2], null));

        SubmitResponse result = roomMoveService.submit(setup.code(), setup.host());
        assertFalse(result.isValid());
        assertFalse(result.isCompleted());
        assertEquals(totalEmpty - 1, result.getEmptyCells(),
                "Submit must count the remaining empty cells like the solo page");
        assertNotNull(result.getMessage());

        // Nothing was completed: game and room both keep playing.
        assertEquals(GameStatus.IN_PROGRESS, loadGame(setup.gameId()).getStatus());
        assertEquals(RoomStatus.IN_PROGRESS, roomService.getRoomState(setup.code(), setup.host()).status());

        // Non-members cannot submit.
        User outsider = register("outsider");
        expectStatus(() -> roomMoveService.submit(setup.code(), outsider), HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Submitting a finished room is idempotent and never duplicates leaderboard points")
    void testSubmitAfterSolveIsIdempotent() {
        RoomSetup setup = startRoom();
        solveRoom(setup);

        assertEquals(RoomStatus.COMPLETED, roomService.getRoomState(setup.code(), setup.host()).status());

        // Late submit (both players may click it) returns success without double-awarding.
        SubmitResponse result = roomMoveService.submit(setup.code(), setup.guest());
        assertTrue(result.isValid());
        assertTrue(result.isCompleted());

        Long scoresForGame = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_scores WHERE game_id = ?",
                Long.class, setup.gameId());
        assertEquals(2L, scoresForGame, "Exactly one leaderboard row per participant");
    }

    @Test
    @DisplayName("Hints are shared: both players see the history and both scores carry the penalty")
    void testSharedHintHistoryAndScorePenalty() {
        RoomSetup setup = startRoom();

        // The guest (not the game owner) may request a hint on the shared board.
        HintResponse hint = roomMoveService.hint(setup.code(), setup.guest(), 3);
        assertNotNull(hint);
        assertTrue(hint.isAvailable(), "An Easy puzzle must offer at least one hint");

        // Hint history is shared and readable by the guest through the solo read endpoint.
        assertEquals(1, gameService.getHintHistory(setup.gameId(), setup.guest()).size(),
                "Hint must be recorded in the shared history");
        assertEquals(1, hintHistoryRepository.countByGameId(setup.gameId()));

        // The host's next hint appends to the same shared history.
        roomMoveService.hint(setup.code(), setup.host(), 3);
        assertEquals(2, hintHistoryRepository.countByGameId(setup.gameId()));

        // Non-members cannot request hints for the room.
        User outsider = register("outsider");
        expectStatus(() -> roomMoveService.hint(setup.code(), outsider, 3), HttpStatus.FORBIDDEN);

        // Solve the room; both awarded scores must carry the shared hint count.
        solveRoom(setup);
        for (User player : List.of(setup.host(), setup.guest())) {
            var score = leaderboardScoreRepository.findByGameIdAndUserId(setup.gameId(), player.getId());
            assertTrue(score.isPresent(), "Every participant must be awarded points");
            assertTrue(score.get().getPoints() > 0);
            assertEquals(2, score.get().getHintsUsed(),
                    "Shared hint history must penalize BOTH scores");
        }
    }

    @Test
    @DisplayName("Room members can read the shared game's analysis and hint history; outsiders cannot")
    void testMemberReadAccessToAnalysisAndHistory() {
        RoomSetup setup = startRoom();

        int[] cell = emptySolutionCells(loadGame(setup.gameId()), 1).get(0);
        roomMoveService.makeMove(setup.code(), setup.host(),
                new RoomMoveRequest(cell[0], cell[1], cell[2], null));
        roomMoveService.hint(setup.code(), setup.guest(), 3);

        // Guest is not the game owner but is a room member: reads succeed.
        assertDoesNotThrow(() -> gameService.getHintHistory(setup.gameId(), setup.guest()));
        assertDoesNotThrow(() -> gameService.analyzePerformance(setup.gameId(), setup.guest()));
        assertDoesNotThrow(() -> gameService.getGame(setup.gameId(), setup.guest()));

        // Owner still works.
        assertDoesNotThrow(() -> gameService.analyzePerformance(setup.gameId(), setup.host()));

        // Outsiders are still rejected with 403.
        User outsider = register("outsider");
        expectStatus(() -> gameService.getGame(setup.gameId(), outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> gameService.getHintHistory(setup.gameId(), outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> gameService.analyzePerformance(setup.gameId(), outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> gameService.getHintHistory(setup.gameId(), null), HttpStatus.FORBIDDEN);

        // Solo mutation routes still refuse the room game outright.
        expectStatus(() -> gameService.submitGame(setup.gameId(), setup.host()), HttpStatus.CONFLICT);
        expectStatus(() -> gameService.requestHint(setup.gameId(), 3, setup.host()), HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("A duplicate-key failure while scoring no longer poisons the Hibernate session")
    void testScoreInsertFailureRecoversGracefully() {
        // Recreate the Stage-2 schema bug: a stale UNIQUE on game_id alone.
        // (Normally removed at startup by LeaderboardScoreSchemaRepair.)
        jdbcTemplate.execute("ALTER TABLE leaderboard_scores ADD CONSTRAINT uk_test_stale_game UNIQUE (game_id)");
        try {
            RoomSetup setup = startRoom();

            // Completing the room used to abort here with:
            // "null id in com.sudoku.model.LeaderboardScore entry (don't flush the
            //  Session after an exception occurs)" — the guest's INSERT hits the
            // stale constraint and the poisoned entity blew up the commit flush.
            solveRoom(setup);

            RoomStateResponse state = roomService.getRoomState(setup.code(), setup.host());
            assertEquals(RoomStatus.COMPLETED, state.status(), "Room must complete despite the failed insert");

            // Exactly one participant holds the single row the stale constraint
            // allows: whoever made the board-completing move inserts first (from
            // the move path), so which role scores depends on the random parity
            // of the puzzle's empty cells. The other participant's INSERT hits
            // the duplicate key but is recovered from gracefully instead of
            // killing the transaction ("null id in ... LeaderboardScore entry").
            boolean hostScored = leaderboardScoreRepository
                    .existsByGameIdAndUserId(setup.gameId(), setup.host().getId());
            boolean guestScored = leaderboardScoreRepository
                    .existsByGameIdAndUserId(setup.gameId(), setup.guest().getId());
            assertTrue(hostScored ^ guestScored,
                    "exactly one participant's score row must survive the stale unique constraint");

            state.players().forEach(player -> {
                boolean scored = (player.role() == com.sudoku.model.RoomRole.HOST) == hostScored;
                if (scored) {
                    assertTrue(player.pointsAwarded() != null && player.pointsAwarded() > 0,
                            "the scoring participant must receive points");
                } else {
                    assertEquals(Integer.valueOf(0), player.pointsAwarded(),
                            "the blocked participant must be recorded with 0 points");
                }
            });
        } finally {
            jdbcTemplate.execute("ALTER TABLE leaderboard_scores DROP CONSTRAINT IF EXISTS uk_test_stale_game");
        }
    }

    @Test
    @DisplayName("HTTP flow: /hint and /submit are wired, authenticated room routes")
    void testHttpHintAndSubmitEndpoints() throws Exception {
        MockHttpSession hostSession = registerViaHttp("host");
        MockHttpSession guestSession = registerViaHttp("guest");

        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .session(hostSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"Easy\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String code = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("roomCode").asText();

        mockMvc.perform(post("/api/rooms/join")
                        .with(csrf())
                        .session(guestSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomCode\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/" + code + "/start").with(csrf()).session(hostSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Guest requests a shared hint over HTTP.
        mockMvc.perform(post("/api/rooms/" + code + "/hint?level=3").with(csrf()).session(guestSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").isBoolean());

        // Host submits an incomplete board and gets the solo-style feedback.
        mockMvc.perform(post("/api/rooms/" + code + "/submit").with(csrf()).session(hostSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.emptyCells").isNumber());

        // Unauthenticated callers are rejected on both new routes (CSRF token
        // present so the 401 is the authentication requirement, not CSRF).
        mockMvc.perform(post("/api/rooms/" + code + "/submit").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/rooms/" + code + "/hint").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession registerViaHttp(String prefix) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest request = new RegisterRequest(
                prefix + "_" + suffix,
                prefix + " " + suffix,
                prefix + "_" + suffix + "@sudoku.test",
                "Password123!"
        );
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        return session;
    }
}
