package com.sudoku.controller;

import com.sudoku.dto.*;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final UserRepository userRepository;

    public GameController(GameService gameService, UserRepository userRepository) {
        this.gameService = gameService;
        this.userRepository = userRepository;
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
    }

    /**
     * Create/start a new game using a puzzle supplied by the system or custom request.
     */
    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @RequestBody(required = false) CreateGameRequest request,
            Authentication authentication
    ) {
        GameResponse response = gameService.createGame(request, resolveUser(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieve the current game state by ID.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.getGame(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve the most recent active/unfinished game (IN_PROGRESS or PAUSED) for "Continue Game".
     */
    @GetMapping("/active")
    public ResponseEntity<GameResponse> getActiveGame(Authentication authentication) {
        Optional<GameResponse> activeGame = gameService.getLatestResumableGame(resolveUser(authentication));
        return activeGame.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Submit a player's move.
     */
    @PostMapping("/{gameId}/move")
    public ResponseEntity<MoveResponse> submitMove(
            @PathVariable Long gameId,
            @Valid @RequestBody MoveRequest request,
            Authentication authentication
    ) {
        MoveResponse response = gameService.makeMove(gameId, request, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Undo the most recent user move.
     */
    @PostMapping("/{gameId}/undo")
    public ResponseEntity<GameResponse> undoMove(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.undoMove(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Redo a previously undone move.
     */
    @PostMapping("/{gameId}/redo")
    public ResponseEntity<GameResponse> redoMove(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.redoMove(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Pause the current game.
     */
    @PostMapping("/{gameId}/pause")
    public ResponseEntity<GameResponse> pauseGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.pauseGame(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Resume the current game.
     */
    @PostMapping("/{gameId}/resume")
    public ResponseEntity<GameResponse> resumeGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.resumeGame(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Restart the current game back to initial state.
     */
    @PostMapping("/{gameId}/restart")
    public ResponseEntity<GameResponse> restartGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        GameResponse response = gameService.restartGame(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Submit/check the completed Sudoku.
     */
    @PostMapping("/{gameId}/submit")
    public ResponseEntity<SubmitResponse> submitGame(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        SubmitResponse response = gameService.submitGame(gameId, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    /**
     * Get a hint for the current board state and record it in hint history.
     */
    @PostMapping("/{gameId}/hint")
    public ResponseEntity<HintResponse> getHint(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "3") int level,
            Authentication authentication
    ) {
        HintResponse response = gameService.requestHint(gameId, level, resolveUser(authentication));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{gameId}/analysis")
    public ResponseEntity<PerformanceAnalysis> getPerformanceAnalysis(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(gameService.analyzePerformance(gameId, resolveUser(authentication)));
    }

    @GetMapping("/analysis/history")
    public ResponseEntity<List<com.sudoku.model.PlayerPerformance>> getPerformanceHistory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(gameService.getPerformanceHistory(resolveUser(authentication)));
    }

    /**
     * Retrieve the hint history for a game, ordered from newest to oldest.
     */
    @GetMapping("/{gameId}/hints")
    public ResponseEntity<List<HintHistoryResponse>> getHintHistory(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        List<HintHistoryResponse> history = gameService.getHintHistory(gameId, resolveUser(authentication));
        return ResponseEntity.ok(history);
    }
}
