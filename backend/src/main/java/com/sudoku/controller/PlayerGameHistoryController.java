package com.sudoku.controller;

import com.sudoku.dto.GameHistoryDetailDto;
import com.sudoku.dto.GameHistoryItemDto;
import com.sudoku.model.User;
import com.sudoku.repository.UserRepository;
import com.sudoku.service.PlayerGameHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerGameHistoryController {

    private final UserRepository userRepository;
    private final PlayerGameHistoryService historyService;

    public PlayerGameHistoryController(
            UserRepository userRepository,
            PlayerGameHistoryService historyService
    ) {
        this.userRepository = userRepository;
        this.historyService = historyService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    @GetMapping("/me/games")
    public ResponseEntity<List<GameHistoryItemDto>> getMyGames(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(historyService.getGameHistory(user.getId(), difficulty, status, search));
    }

    @GetMapping("/me/games/{gameId}")
    public ResponseEntity<GameHistoryDetailDto> getMyGameDetail(
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        return ResponseEntity.ok(historyService.getGameDetail(user.getId(), gameId));
    }

    @GetMapping("/{playerId}/games/{gameId}")
    public ResponseEntity<GameHistoryDetailDto> getPlayerGameDetail(
            @PathVariable Long playerId,
            @PathVariable Long gameId,
            Authentication authentication
    ) {
        User user = resolveAuthenticatedUser(authentication);
        if (!user.getId().equals(playerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to another player's game detail is restricted");
        }
        return ResponseEntity.ok(historyService.getGameDetail(playerId, gameId));
    }
}
