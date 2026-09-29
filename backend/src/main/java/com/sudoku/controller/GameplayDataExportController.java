package com.sudoku.controller;

import com.sudoku.dto.PlayerGameplayExportDto;
import com.sudoku.model.Game;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.Move;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.HintHistoryRepository;
import com.sudoku.repository.LeaderboardScoreRepository;
import com.sudoku.repository.MoveRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class GameplayDataExportController {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final HintHistoryRepository hintHistoryRepository;
    private final LeaderboardScoreRepository leaderboardScoreRepository;

    public GameplayDataExportController(
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

    /**
     * Exposes game session records for hybrid data ingestion in ML pipeline.
     */
    @GetMapping("/gameplay-export")
    public ResponseEntity<List<PlayerGameplayExportDto>> exportGameplayRecords() {
        List<Game> games = gameRepository.findAll();
        List<PlayerGameplayExportDto> export = new ArrayList<>();

        for (Game g : games) {
            long moves = moveRepository.countByGameId(g.getId());
            long hints = hintHistoryRepository.countByGameId(g.getId());
            long undos = moveRepository.findByGameIdOrderByMoveNumberAsc(g.getId()).stream()
                    .filter(Move::isUndone).count();

            long attempts = moves + g.getMistakes();
            double acc = attempts > 0 ? (double) moves / attempts : 1.0;

            int score = 0;
            if (g.getUser() != null) {
                score = leaderboardScoreRepository.findByGameIdAndUserId(g.getId(), g.getUser().getId())
                        .map(LeaderboardScore::getPoints)
                        .orElse(0);
            }

            export.add(new PlayerGameplayExportDto(
                    g.getId(),
                    g.getUser() != null ? g.getUser().getId() : null,
                    g.getDifficulty() != null ? g.getDifficulty() : "Medium",
                    g.getElapsedSeconds(),
                    (int) moves,
                    g.getMistakes(),
                    (int) hints,
                    (int) undos,
                    Math.round(acc * 1000.0) / 1000.0,
                    score,
                    g.getStatus().name(),
                    g.getStartedAt() != null ? g.getStartedAt() : g.getCreatedAt()
            ));
        }

        return ResponseEntity.ok(export);
    }
}
