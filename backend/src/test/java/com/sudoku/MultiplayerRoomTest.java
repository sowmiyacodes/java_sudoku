package com.sudoku;

import com.sudoku.dto.CreateRoomRequest;
import com.sudoku.dto.JoinRoomRequest;
import com.sudoku.dto.RegisterRequest;
import com.sudoku.dto.RoomPlayerResponse;
import com.sudoku.dto.RoomStateResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudoku.model.RoomRole;
import com.sudoku.model.RoomStatus;
import com.sudoku.model.User;
import com.sudoku.service.RoomService;
import com.sudoku.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Multiplayer room lifecycle: creation, joining, access control,
 * departure, reconnection and expiry (Stage 3 requirements 2, 3, 5, 10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sudoku-rooms-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class MultiplayerRoomTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User register(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userService.register(new RegisterRequest(
                prefix + "_" + suffix,
                prefix + " " + suffix,
                prefix + "_" + suffix + "@sudoku.test",
                "Password123!"
        ));
    }

    private ResponseStatusException expectStatus(Runnable action, HttpStatus expected) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(expected, ex.getStatusCode());
        return ex;
    }

    @Test
    @DisplayName("Room creation returns a unique shareable code with the creator as HOST")
    void testCreateRoom() {
        User host = register("host");
        RoomStateResponse state = roomService.createRoom(host, new CreateRoomRequest("Hard"));

        assertNotNull(state.roomCode());
        assertEquals(6, state.roomCode().length());
        assertEquals(RoomStatus.WAITING, state.status());
        assertEquals("Hard", state.difficulty());
        assertEquals(0, state.stateVersion());
        assertEquals(1, state.players().size());
        assertEquals(RoomRole.HOST, state.you().role());
        assertEquals(host.getUsername(), state.hostUsername());
        assertNull(state.guestUsername());
        assertNull(state.game());

        // The owner can fetch state; it matches the creation snapshot.
        RoomStateResponse fetched = roomService.getRoomState(state.roomCode(), host);
        assertEquals(state.roomCode(), fetched.roomCode());
        assertNotNull(fetched.you());
    }

    @Test
    @DisplayName("Second player joins by code; a third player is rejected as full")
    void testJoinRoomAndCapacityLimit() {
        User host = register("host");
        User guest = register("guest");
        User outsider = register("outsider");
        String code = roomService.createRoom(host, new CreateRoomRequest("Medium")).roomCode();

        RoomStateResponse joined = roomService.joinRoom(guest, new JoinRoomRequest(code).roomCode());
        assertEquals(RoomStatus.WAITING, joined.status());
        assertEquals(2, joined.players().size());
        assertEquals(RoomRole.GUEST, joined.you().role());
        assertEquals(guest.getUsername(), joined.guestUsername());
        assertEquals(guest.getUsername(), joined.players().get(1).username());

        // MVP cap: two players per room.
        expectStatus(() -> roomService.joinRoom(outsider, code), HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Joining with an unknown code returns 404")
    void testInvalidRoomCode() {
        User user = register("stranger");
        expectStatus(() -> roomService.joinRoom(user, "NOPE99"), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Room state and mutations are restricted to room members")
    void testAccessControl() {
        User host = register("host");
        User guest = register("guest");
        User outsider = register("outsider");
        String code = roomService.createRoom(host, new CreateRoomRequest("Medium")).roomCode();
        roomService.joinRoom(guest, new JoinRoomRequest(code).roomCode());

        // State: members OK, outsider forbidden.
        assertDoesNotThrow(() -> roomService.getRoomState(code, host));
        assertDoesNotThrow(() -> roomService.getRoomState(code, guest));
        expectStatus(() -> roomService.getRoomState(code, outsider), HttpStatus.FORBIDDEN);

        // Mutations: outsider cannot start, move or leave this room.
        expectStatus(() -> roomService.startGame(code, outsider), HttpStatus.FORBIDDEN);
        expectStatus(() -> roomService.leaveRoom(code, outsider), HttpStatus.FORBIDDEN);

        // Guests cannot start the game (host authority).
        expectStatus(() -> roomService.startGame(code, guest), HttpStatus.FORBIDDEN);

        // Anonymous callers are rejected outright.
        expectStatus(() -> roomService.joinRoom(null, code), HttpStatus.UNAUTHORIZED);
        expectStatus(() -> roomService.getRoomState(code, null), HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Guest departure frees the slot; host departure abandons the room; reconnection re-attaches")
    void testLeaveAndReconnection() {
        User host = register("host");
        User guest = register("guest");
        User replacement = register("replacement");
        String code = roomService.createRoom(host, new CreateRoomRequest("Medium")).roomCode();
        roomService.joinRoom(guest, new JoinRoomRequest(code).roomCode());

        // Guest leaves while waiting -> slot frees up for someone else.
        roomService.leaveRoom(code, guest);
        RoomStateResponse afterLeave = roomService.getRoomState(code, host);
        assertEquals(1, afterLeave.players().stream().filter(RoomPlayerResponse::active).count(),
                "Only the host remains active");

        RoomStateResponse replacementJoin = roomService.joinRoom(replacement, code);
        assertEquals(2, replacementJoin.players().stream().filter(RoomPlayerResponse::active).count(),
                "Host + replacement are the active players (left guest stays in history)");

        // The original guest cannot sneak back in while the room is full.
        expectStatus(() -> roomService.joinRoom(guest, code), HttpStatus.CONFLICT);

        // Reconnection: the replacement player can leave and re-attach with the same code.
        roomService.leaveRoom(code, replacement);
        RoomStateResponse reattached = roomService.joinRoom(replacement, code);
        assertEquals(RoomRole.GUEST, reattached.you().role());
        assertTrue(reattached.you().active());

        // Host departure while WAITING abandons the room for everyone.
        roomService.leaveRoom(code, host);
        RoomStateResponse abandoned = roomService.getRoomState(code, replacement);
        assertEquals(RoomStatus.ABANDONED, abandoned.status());
        expectStatus(() -> roomService.joinRoom(register("late"), code), HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Idle rooms expire and can no longer be joined")
    void testRoomExpiry() {
        User host = register("host");
        String code = roomService.createRoom(host, new CreateRoomRequest("Medium")).roomCode();

        // Backdate the last activity beyond the TTL (6 hours).
        jdbcTemplate.update(
                "UPDATE multiplayer_rooms SET last_activity_at = ? WHERE room_code = ?",
                Timestamp.valueOf(LocalDateTime.now().minusHours(7)),
                code);

        // A new player gets 410 Gone.
        expectStatus(() -> roomService.joinRoom(register("late"), code), HttpStatus.GONE);

        // A member polling sees the room marked EXPIRED instead of a dead WAITING room.
        RoomStateResponse state = roomService.getRoomState(code, host);
        assertEquals(RoomStatus.EXPIRED, state.status());
    }

    @Test
    @DisplayName("Room endpoints require authentication")
    void testRoomEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/rooms").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/rooms/join")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"roomCode\":\"ABC123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/rooms/ABC123"))
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

    @Test
    @DisplayName("Full REST flow: create, join, start and move over HTTP with useful error bodies")
    void testHttpFlow() throws Exception {
        MockHttpSession hostSession = registerViaHttp("host");
        MockHttpSession guestSession = registerViaHttp("guest");
        MockHttpSession outsiderSession = registerViaHttp("outsider");

        // Host creates a room (authenticated, session-based).
        MvcResult created = mockMvc.perform(post("/api/rooms")
                        .with(csrf())
                        .session(hostSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"Easy\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andReturn();
        JsonNode roomJson = objectMapper.readTree(created.getResponse().getContentAsString());
        String code = roomJson.get("roomCode").asText();
        assertEquals(6, code.length());

        // Guest joins with a lower-case code (codes are case-insensitive).
        mockMvc.perform(post("/api/rooms/join")
                        .with(csrf())
                        .session(guestSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomCode\":\"" + code.toLowerCase() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.you.role").value("GUEST"));

        // A third player gets 409 with a message the UI can display.
        MvcResult full = mockMvc.perform(post("/api/rooms/join")
                        .with(csrf())
                        .session(outsiderSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomCode\":\"" + code + "\"}"))
                .andExpect(status().isConflict())
                .andReturn();
        // MockMvc skips the servlet ERROR dispatch, so the reason is carried by
        // sendError's errorMessage here; on a real server include-message=always
        // renders it as {"message": "Room is full."} for the UI.
        String reason = full.getResponse().getErrorMessage();
        assertNotNull(reason, "Conflict must carry a human-readable reason");
        assertTrue(reason.contains("Room is full"),
                "Reason must be UI-displayable: " + reason);

        // Only the host may start; the guest sees 403.
        mockMvc.perform(post("/api/rooms/" + code + "/start").with(csrf()).session(guestSession))
                .andExpect(status().isForbidden());
        MvcResult started = mockMvc.perform(post("/api/rooms/" + code + "/start")
                        .with(csrf())
                        .session(hostSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.game.id").isNumber())
                .andReturn();
        long gameId = objectMapper.readTree(started.getResponse().getContentAsString())
                .get("game").get("id").asLong();

        // Outsider cannot read state or move.
        mockMvc.perform(get("/api/rooms/" + code).session(outsiderSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/rooms/" + code + "/move")
                        .with(csrf())
                        .session(outsiderSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"row\":0,\"column\":0,\"value\":5}"))
                .andExpect(status().isForbidden());

        // The shared game is not reachable through solo endpoints either.
        mockMvc.perform(post("/api/games/" + gameId + "/move")
                        .with(csrf())
                        .session(hostSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"row\":0,\"column\":0,\"value\":5}"))
                .andExpect(status().isConflict());

        // Unauthenticated callers are rejected on every room route.
        mockMvc.perform(get("/api/rooms/" + code))
                .andExpect(status().isUnauthorized());
    }
}
