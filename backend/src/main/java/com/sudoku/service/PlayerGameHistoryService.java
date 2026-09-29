package com.sudoku.service;

import com.sudoku.dto.GameHistoryDetailDto;
import com.sudoku.dto.GameHistoryItemDto;
import com.sudoku.dto.HintHistoryResponse;
import com.sudoku.dto.MoveResponse;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.Move;
import com.sudoku.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class PlayerGameHistoryService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final HintHistoryRepository hintHistoryRepository;
    private final LeaderboardScoreRepository leaderboardScoreRepository;

    public PlayerGameHistoryService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            HintHistoryRepository hintHistoryRepository,
            LeaderboardScoreRepository leaderboardScoreRepository
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.hintHistoryRepository = hintHistoryRepository;
        this.leaderboardScoreRepository = leaderboardScoreRepository;
    }

    public List<GameHistoryItemDto> getGameHistory(
            Long userId,
            String difficulty,
            String status,
            String search
    ) {
        List<Game> games = gameRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return games.stream()
                .filter(g -> {
                    if (difficulty != null && !difficulty.isBlank() && !"ALL".equalsIgnoreCase(difficulty)) {
                        return g.getDifficulty() != null && g.getDifficulty().equalsIgnoreCase(difficulty);
                    }
                    return true;
                })
                .filter(g -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        return g.getStatus().name().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .filter(g -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase().trim();
                        boolean matchId = String.valueOf(g.getId()).contains(s);
                        boolean matchPuzzle = g.getPuzzleId() != null && g.getPuzzleId().toLowerCase().contains(s);
                        boolean matchDiff = g.getDifficulty() != null && g.getDifficulty().toLowerCase().contains(s);
                        return matchId || matchPuzzle || matchDiff;
                    }
                    return true;
                })
                .map(this::toItemDto)
                .toList();
    }

    public GameHistoryDetailDto getGameDetail(Long userId, Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + gameId));

        if (game.getUser() == null || !game.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to game history detail");
        }

        List<Move> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        List<MoveResponse> moveResponses = moves.stream()
                .map(m -> MoveResponse.valid(
                        m.getRow(), m.getColumn(), m.getValue(),
                        false, game.getMistakes(), null, game.getElapsedSeconds(),
                        false, false
                ))
                .toList();

        List<HintHistoryResponse> hints = hintHistoryRepository.findByGameIdOrderByCreatedAtDesc(gameId).stream()
                .map(HintHistoryResponse::fromEntity)
                .toList();

        int score = leaderboardScoreRepository.findByGameIdAndUserId(gameId, userId)
                .map(LeaderboardScore::getPoints)
                .orElse(0);

        long movesCount = moves.size();
        long attempts = movesCount + game.getMistakes();
        double accuracy = attempts > 0 ? (double) movesCount / attempts : 1.0;
        int undos = (int) moves.stream().filter(Move::isUndone).count();

        return new GameHistoryDetailDto(
                game.getId(),
                game.getPuzzleId() != null ? game.getPuzzleId() : "PUZZLE-" + game.getId(),
                game.getDifficulty(),
                game.getStartedAt(),
                game.getCompletedAt(),
                game.getElapsedSeconds(),
                (int) movesCount,
                game.getMistakes(),
                hints.size(),
                undos,
                Math.round(accuracy * 1000.0) / 1000.0,
                score,
                game.getStatus().name(),
                moveResponses,
                hints
        );
    }

    private GameHistoryItemDto toItemDto(Game g) {
        long movesCount = moveRepository.countByGameId(g.getId());
        long hintsCount = hintHistoryRepository.countByGameId(g.getId());
        long undosCount = moveRepository.findByGameIdOrderByMoveNumberAsc(g.getId()).stream()
                .filter(Move::isUndone).count();

        long attempts = movesCount + g.getMistakes();
        double accuracy = attempts > 0 ? (double) movesCount / attempts : 1.0;

        int score = 0;
        if (g.getUser() != null) {
            score = leaderboardScoreRepository.findByGameIdAndUserId(g.getId(), g.getUser().getId())
                    .map(LeaderboardScore::getPoints)
                    .orElse(0);
        }

        return new GameHistoryItemDto(
                g.getId(),
                g.getPuzzleId() != null ? g.getPuzzleId() : "P-" + g.getId(),
                g.getDifficulty() != null ? g.getDifficulty() : "Standard",
                g.getStartedAt(),
                g.getCompletedAt(),
                g.getElapsedSeconds(),
                (int) movesCount,
                g.getMistakes(),
                (int) hintsCount,
                (int) undosCount,
                Math.round(accuracy * 1000.0) / 1000.0,
                score,
                g.getStatus().name()
        );
    }
}
