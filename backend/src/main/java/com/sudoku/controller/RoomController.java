package com.sudoku.controller;

import com.sudoku.dto.CreateRoomRequest;
import com.sudoku.dto.HintResponse;
import com.sudoku.dto.JoinRoomRequest;
import com.sudoku.dto.RoomMoveRequest;
import com.sudoku.dto.SubmitResponse;
import com.sudoku.dto.RoomMoveResponse;
import com.sudoku.dto.RoomStateResponse;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.RoomMoveService;
import com.sudoku.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Cooperative multiplayer rooms (REST polling; designed so the same state
 * DTOs can later be pushed over WebSockets without changing clients).
 *
 * POST /api/rooms                 create a room (authenticated)
 * POST /api/rooms/join            join / reconnect by room code
 * GET  /api/rooms/{code}          poll full room state (members only, heartbeat)
 * POST /api/rooms/{code}/start    host starts the shared game
 * POST /api/rooms/{code}/move     shared move (locked + version checked)
 * POST /api/rooms/{code}/submit   validate the shared board / finish the room
 * POST /api/rooms/{code}/hint     shared hint (shared history + score penalty)
 * POST /api/rooms/{code}/leave    leave the room
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomMoveService roomMoveService;
    private final UserRepository userRepository;

    public RoomController(RoomService roomService, RoomMoveService roomMoveService, UserRepository userRepository) {
        this.roomService = roomService;
        this.roomMoveService = roomMoveService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }

    @PostMapping
    public ResponseEntity<RoomStateResponse> createRoom(
            @RequestBody(required = false) @Valid CreateRoomRequest request,
            Authentication authentication
    ) {
        RoomStateResponse state = roomService.createRoom(resolveUser(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(state);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomStateResponse> joinRoom(
            @Valid @RequestBody JoinRoomRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomService.joinRoom(resolveUser(authentication), request.roomCode()));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<RoomStateResponse> getRoomState(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomService.getRoomState(roomCode, resolveUser(authentication)));
    }

    @PostMapping("/{roomCode}/start")
    public ResponseEntity<RoomStateResponse> startGame(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomService.startGame(roomCode, resolveUser(authentication)));
    }

    @PostMapping("/{roomCode}/move")
    public ResponseEntity<RoomMoveResponse> makeMove(
            @PathVariable String roomCode,
            @Valid @RequestBody RoomMoveRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomMoveService.makeMove(roomCode, resolveUser(authentication), request));
    }

    @PostMapping("/{roomCode}/submit")
    public ResponseEntity<SubmitResponse> submitBoard(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomMoveService.submit(roomCode, resolveUser(authentication)));
    }

    @PostMapping("/{roomCode}/hint")
    public ResponseEntity<HintResponse> getHint(
            @PathVariable String roomCode,
            @RequestParam(defaultValue = "3") int level,
            Authentication authentication
    ) {
        return ResponseEntity.ok(roomMoveService.hint(roomCode, resolveUser(authentication), level));
    }

    @PostMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomCode,
            Authentication authentication
    ) {
        roomService.leaveRoom(roomCode, resolveUser(authentication));
        return ResponseEntity.noContent().build();
    }
}
