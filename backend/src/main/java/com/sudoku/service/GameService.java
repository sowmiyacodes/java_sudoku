package com.sudoku.service;

import com.sudoku.dto.*;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.model.LeaderboardScore;
import com.sudoku.model.Move;
import com.sudoku.model.SudokuBoard;
import com.sudoku.model.User;
import com.sudoku.repository.GameRepository;
import com.sudoku.repository.HintHistoryRepository;
import com.sudoku.repository.MoveRepository;
import com.sudoku.repository.MultiplayerRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final MoveRepository moveRepository;
    private final SudokuValidationService validationService;
    private final PuzzleProvider puzzleProvider;
    private final HintService hintService;
    private final HintHistoryRepository hintHistoryRepository;
    private final LearningAnalysisService learningAnalysisService;
    private final ScoringService scoringService;
    private final MultiplayerRoomRepository roomRepository;

    public GameService(
            GameRepository gameRepository,
            MoveRepository moveRepository,
            SudokuValidationService validationService,
            PuzzleProvider puzzleProvider,
            HintService hintService,
            HintHistoryRepository hintHistoryRepository,
            LearningAnalysisService learningAnalysisService,
            ScoringService scoringService,
            MultiplayerRoomRepository roomRepository
    ) {
        this.gameRepository = gameRepository;
        this.moveRepository = moveRepository;
        this.validationService = validationService;
        this.puzzleProvider = puzzleProvider;
        this.hintService = hintService;
        this.hintHistoryRepository = hintHistoryRepository;
        this.learningAnalysisService = learningAnalysisService;
        this.scoringService = scoringService;
        this.roomRepository = roomRepository;
    }

    public void assertGameAccess(Game game, User user) {
        if (game.getUser() != null) {
            if (user == null || !game.getUser().getId().equals(user.getId())) {
                // Both players of a multiplayer room share one game, so room
                // members get READ access via room membership (solo mutation
                // routes still reject room games with 409 through
                // assertNotRoomGame, which every mutating method calls).
                if (user != null && roomRepository.existsByGameIdAndUserId(game.getId(), user.getId())) {
                    return;
                }
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you do not have permission to access this game.");
            }
        }
    }

    /**
     * Solo endpoints must never mutate a game owned by a multiplayer room:
     * shared games are only writable through the room API, which holds the room
     * row lock and serializes concurrent moves. This stops the host from
     * bypassing room synchronization via the solo routes.
     */
    public void assertNotRoomGame(Long gameId) {
        if (roomRepository.existsByGameId(gameId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This game belongs to a multiplayer room. Use the room endpoints instead.");
        }
    }

    public GameResponse createGame(CreateGameRequest request) {
        return createGame(request, null);
    }

    public GameResponse createGame(CreateGameRequest request, User user) {
        PuzzleProvider.Puzzle puzzle;
        if (request != null && request.getInitialBoard() != null && request.getInitialBoard().length == 9) {
            int[][] initial = request.getInitialBoard();
            int[][] solution = request.getSolutionBoard() != null ? request.getSolutionBoard() : new int[9][9];
            String diff = request.getDifficulty() != null ? request.getDifficulty() : "Custom";
            String pid = request.getPuzzleId() != null ? request.getPuzzleId() : "custom-" + System.currentTimeMillis();
            puzzle = new PuzzleProvider.Puzzle(pid, diff, initial, solution, null);
        } else if (request != null && request.getDifficulty() != null) {
            puzzle = puzzleProvider.getPuzzleByDifficulty(request.getDifficulty());
        } else if (request != null && request.getPuzzleId() != null) {
            puzzle = puzzleProvider.getPuzzle(request.getPuzzleId());
        } else {
            puzzle = puzzleProvider.getDefaultPuzzle();
        }

        SudokuBoard initialBoard = new SudokuBoard(puzzle.initialBoard());
        SudokuBoard solutionBoard = new SudokuBoard(puzzle.solutionBoard());

        Game game = new Game();
        game.setUser(user);
        game.setPuzzleId(puzzle.id());
        game.setDifficulty(puzzle.difficulty());

        if (puzzle.metadata() != null) {
            game.setDifficultyScore(puzzle.metadata().getScore());
            try {
                game.setMetadataJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(puzzle.metadata()));
            } catch (Exception e) {
                // Ignore parse errors
            }
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        game.setElapsedSeconds(0);
        game.setMistakes(0);
        game.setInitialBoardJson(initialBoard.toJson());
        game.setCurrentBoardJson(initialBoard.toJson());
        game.setSolutionBoardJson(solutionBoard.toJson());

        Game savedGame = gameRepository.save(game);
        return GameResponse.fromGame(savedGame, false, false);
    }

    public GameResponse getGame(Long gameId) {
        return getGame(gameId, null);
    }

    public GameResponse getGame(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        updateLiveElapsedTime(game);
        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public Optional<GameResponse> getLatestResumableGame() {
        return getLatestResumableGame(null);
    }

    public Optional<GameResponse> getLatestResumableGame(User user) {
        // Skip multiplayer room games: those are resumed through the room API.
        List<Game> candidates = user != null
                ? gameRepository.findByUserIdAndStatusInOrderByUpdatedAtDesc(user.getId(), List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED))
                : gameRepository.findByUserIdIsNullAndStatusInOrderByUpdatedAtDesc(List.of(GameStatus.IN_PROGRESS, GameStatus.PAUSED));

        for (Game game : candidates) {
            if (roomRepository.existsByGameId(game.getId())) {
                continue;
            }
            updateLiveElapsedTime(game);
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(game.getId());
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(game.getId());
            return Optional.of(GameResponse.fromGame(game, canUndo, canRedo));
        }
        return Optional.empty();
    }

    public MoveResponse makeMove(Long gameId, MoveRequest request) {
        return makeMove(gameId, request, null);
    }

    public MoveResponse makeMove(Long gameId, MoveRequest request, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);
        return doMakeMove(game, request, user);
    }

    /**
     * Applies a move to a multiplayer room game. Room membership checks and the
     * pessimistic room lock are handled by RoomMoveService before this is called;
     * solo ownership rules do not apply because both players share one game.
     */
    public MoveResponse applySharedMove(Long gameId, MoveRequest request, User mover) {
        Game game = findGameById(gameId);
        return doMakeMove(game, request, mover);
    }

    private MoveResponse doMakeMove(Game game, MoveRequest request, User user) {
        Long gameId = game.getId();
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is already completed");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused. Resume before making moves");
        }

        updateLiveElapsedTime(game);

        SudokuBoard initialBoard = SudokuBoard.fromJson(game.getInitialBoardJson());
        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());

        int row = request.getRow();
        int col = request.getColumn();
        int value = request.getValue();

        // 1. Fixed cell check
        if (initialBoard.getCell(row, col) != 0) {
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.invalid(
                    row, col, value,
                    "Cannot modify original puzzle cell",
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        int previousValue = currentBoard.getCell(row, col);
        if (previousValue == value) {
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.valid(
                    row, col, value,
                    false,
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        // 2. Validate move against Sudoku rules
        SudokuBoard tempBoard = new SudokuBoard(currentBoard.getGrid());
        tempBoard.setCell(row, col, 0);
        SudokuValidationService.ValidationResult validation = validationService.validateMove(tempBoard, row, col, value);

        if (!validation.valid() && value != 0) {
            game.setMistakes(game.getMistakes() + 1);
            gameRepository.save(game);
            boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
            boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
            return MoveResponse.invalid(
                    row, col, value,
                    validation.reason(),
                    game.getMistakes(),
                    currentBoard.getGrid(),
                    game.getElapsedSeconds(),
                    canUndo, canRedo
            );
        }

        // Clear redo branch on new move
        moveRepository.deleteUndoneMovesByGameId(gameId);

        // Update board
        currentBoard.setCell(row, col, value);
        game.setCurrentBoardJson(currentBoard.toJson());

        // Save move history
        int nextMoveNumber = (int) moveRepository.countByGameId(gameId) + 1;
        Move move = new Move(gameId, row, col, value, previousValue, nextMoveNumber);
        moveRepository.save(move);

        // Check if solved
        boolean solved = validationService.isBoardSolved(currentBoard);
        if (solved) {
            game.setStatus(GameStatus.COMPLETED);
            game.setCompletedAt(LocalDateTime.now());
            if (game.getUser() == null && user != null) {
                game.setUser(user);
            }
            User activeUser = user != null ? user : game.getUser();
            if (activeUser != null) {
                scoringService.awardScore(game, activeUser);
            }
        }

        gameRepository.save(game);

        boolean canUndo = true;
        boolean canRedo = false;

        return MoveResponse.valid(
                row, col, value,
                solved,
                game.getMistakes(),
                currentBoard.getGrid(),
                game.getElapsedSeconds(),
                canUndo, canRedo
        );
    }

    public GameResponse undoMove(Long gameId) {
        return undoMove(gameId, null);
    }

    public GameResponse undoMove(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot undo moves on a completed game");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused");
        }

        updateLiveElapsedTime(game);

        Optional<Move> lastActiveMoveOpt = moveRepository.findTopByGameIdAndUndoneFalseOrderByMoveNumberDesc(gameId);
        if (lastActiveMoveOpt.isPresent()) {
            Move move = lastActiveMoveOpt.get();
            SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
            currentBoard.setCell(move.getRow(), move.getColumn(), move.getPreviousValue());
            game.setCurrentBoardJson(currentBoard.toJson());

            move.setUndone(true);
            moveRepository.save(move);
            gameRepository.save(game);
        }

        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public GameResponse redoMove(Long gameId) {
        return redoMove(gameId, null);
    }

    public GameResponse redoMove(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot redo moves on a completed game");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused");
        }

        updateLiveElapsedTime(game);

        Optional<Move> nextUndoneMoveOpt = moveRepository.findTopByGameIdAndUndoneTrueOrderByMoveNumberAsc(gameId);
        if (nextUndoneMoveOpt.isPresent()) {
            Move move = nextUndoneMoveOpt.get();
            SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
            currentBoard.setCell(move.getRow(), move.getColumn(), move.getValue());
            game.setCurrentBoardJson(currentBoard.toJson());

            move.setUndone(false);
            moveRepository.save(move);

            if (validationService.isBoardSolved(currentBoard)) {
                game.setStatus(GameStatus.COMPLETED);
                game.setCompletedAt(LocalDateTime.now());
                if (game.getUser() == null && user != null) {
                    game.setUser(user);
                }
                User activeUser = user != null ? user : game.getUser();
                if (activeUser != null) {
                    scoringService.awardScore(game, activeUser);
                }
            }

            gameRepository.save(game);
        }

        boolean canUndo = moveRepository.existsByGameIdAndUndoneFalse(gameId);
        boolean canRedo = moveRepository.existsByGameIdAndUndoneTrue(gameId);
        return GameResponse.fromGame(game, canUndo, canRedo);
    }

    public GameResponse pauseGame(Long gameId) {
        return pauseGame(gameId, null);
    }

    public GameResponse pauseGame(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed game cannot be paused");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            return GameResponse.fromGame(game,
                    moveRepository.existsByGameIdAndUndoneFalse(gameId),
                    moveRepository.existsByGameIdAndUndoneTrue(gameId));
        }

        updateLiveElapsedTime(game);
        game.setStatus(GameStatus.PAUSED);
        game.setPausedAt(LocalDateTime.now());
        gameRepository.save(game);

        return GameResponse.fromGame(game,
                moveRepository.existsByGameIdAndUndoneFalse(gameId),
                moveRepository.existsByGameIdAndUndoneTrue(gameId));
    }

    public GameResponse resumeGame(Long gameId) {
        return resumeGame(gameId, null);
    }

    public GameResponse resumeGame(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed game cannot be resumed");
        }
        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            return GameResponse.fromGame(game,
                    moveRepository.existsByGameIdAndUndoneFalse(gameId),
                    moveRepository.existsByGameIdAndUndoneTrue(gameId));
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setPausedAt(null);
        game.setStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        return GameResponse.fromGame(game,
                moveRepository.existsByGameIdAndUndoneFalse(gameId),
                moveRepository.existsByGameIdAndUndoneTrue(gameId));
    }

    public SubmitResponse submitGame(Long gameId) {
        return submitGame(gameId, null);
    }

    public SubmitResponse submitGame(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        SubmitResponse response = evaluateAndComplete(game);
        if (response.isCompleted()) {
            if (game.getUser() == null && user != null) {
                game.setUser(user);
                gameRepository.save(game);
            }
            User activeUser = user != null ? user : game.getUser();
            if (activeUser != null) {
                LeaderboardScore score = scoringService.awardScore(game, activeUser);
                if (score != null) {
                    response.setPointsAwarded(score.getPoints());
                }
            }
        }
        return response;
    }

    /**
     * Board evaluation shared with multiplayer rooms: validates the current
     * board and, when it is solved, marks the game COMPLETED. No leaderboard
     * points are awarded here — solo play awards its single owner, while a
     * room awards every eligible participant through RoomMoveService.
     */
    public SubmitResponse evaluateAndComplete(Game game) {
        updateLiveElapsedTime(game);

        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
        SudokuBoard solutionBoard = game.getSolutionBoardJson() != null ? SudokuBoard.fromJson(game.getSolutionBoardJson()) : null;

        boolean isSolved = validationService.isBoardSolved(currentBoard);

        if (isSolved) {
            game.setStatus(GameStatus.COMPLETED);
            game.setCompletedAt(LocalDateTime.now());
            gameRepository.save(game);
            return SubmitResponse.success(game.getElapsedSeconds(), game.getMistakes());
        }

        // Find empty count & incorrect cells
        int emptyCount = 0;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (currentBoard.getCell(r, c) == 0) {
                    emptyCount++;
                }
            }
        }

        List<CellPosition> incorrectCells = validationService.findIncorrectCells(currentBoard, solutionBoard);
        return SubmitResponse.incomplete(emptyCount, incorrectCells, game.getMistakes(), game.getElapsedSeconds());
    }

    public GameResponse restartGame(Long gameId) {
        return restartGame(gameId, null);
    }

    public GameResponse restartGame(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);

        game.setCurrentBoardJson(game.getInitialBoardJson());
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setStartedAt(LocalDateTime.now());
        game.setPausedAt(null);
        game.setCompletedAt(null);
        game.setElapsedSeconds(0);
        game.setMistakes(0);

        moveRepository.deleteByGameId(gameId);
        Game savedGame = gameRepository.save(game);

        return GameResponse.fromGame(savedGame, false, false);
    }

    public HintResponse requestHint(Long gameId) {
        return requestHint(gameId, 3, null);
    }

    public HintResponse requestHint(Long gameId, int level) {
        return requestHint(gameId, level, null);
    }

    public HintResponse requestHint(Long gameId, int level, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        assertNotRoomGame(gameId);
        return requestHintForGame(game, level);
    }

    /**
     * Hint core shared with multiplayer rooms: room membership and the room
     * row lock are enforced by the caller (RoomMoveService), so only game-level
     * state checks apply here. Every hint is recorded in the shared hint
     * history, which the scoring formula counts as a penalty for ALL players
     * of that game.
     */
    public HintResponse requestHintForGame(Game game, int level) {
        if (game.getStatus() == GameStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request hints on a completed game");
        }
        if (game.getStatus() == GameStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game is paused. Resume to get hints");
        }

        updateLiveElapsedTime(game);

        SudokuBoard currentBoard = SudokuBoard.fromJson(game.getCurrentBoardJson());
        HintResponse hint = hintService.getHint(currentBoard.getGrid(), level);

        if (hint != null && hint.isAvailable()) {
            com.sudoku.model.HintHistory history = new com.sudoku.model.HintHistory(
                    game.getId(),
                    hint.getRow(),
                    hint.getColumn(),
                    hint.getValue(),
                    hint.getTechnique(),
                    hint.getExplanation()
            );
            hintHistoryRepository.save(history);
        }

        return hint;
    }

    public PerformanceAnalysis analyzePerformance(Long gameId) {
        return analyzePerformance(gameId, null);
    }

    public PerformanceAnalysis analyzePerformance(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);
        updateLiveElapsedTime(game);

        List<Move> moves = moveRepository.findByGameIdOrderByMoveNumberAsc(gameId);
        List<com.sudoku.model.HintHistory> hints = hintHistoryRepository.findByGameIdOrderByCreatedAtDesc(gameId);
        return learningAnalysisService.analyzeAndPersist(game, moves, hints);
    }

    public List<com.sudoku.model.PlayerPerformance> getPerformanceHistory() {
        return getPerformanceHistory(null);
    }

    public List<com.sudoku.model.PlayerPerformance> getPerformanceHistory(User user) {
        if (user != null) {
            return learningAnalysisService.getHistoryForUser(user.getId());
        }
        return learningAnalysisService.getHistoryForUser(null);
    }

    public List<HintHistoryResponse> getHintHistory(Long gameId) {
        return getHintHistory(gameId, null);
    }

    public List<HintHistoryResponse> getHintHistory(Long gameId, User user) {
        Game game = findGameById(gameId);
        assertGameAccess(game, user);

        List<com.sudoku.model.HintHistory> historyList = hintHistoryRepository.findByGameIdOrderByCreatedAtDesc(gameId);
        return historyList.stream()
                .map(HintHistoryResponse::fromEntity)
                .toList();
    }

    /**
     * Advances the stored elapsed time for an in-progress game to "now".
     * Public so the multiplayer room state endpoint can keep the shared clock fresh.
     */
    public void updateLiveElapsedTime(Game game) {
        if (game.getStatus() == GameStatus.IN_PROGRESS && game.getStartedAt() != null) {
            long currentSessionSeconds = Duration.between(game.getStartedAt(), LocalDateTime.now()).getSeconds();
            if (currentSessionSeconds > 0) {
                game.setElapsedSeconds(game.getElapsedSeconds() + currentSessionSeconds);
                game.setStartedAt(LocalDateTime.now());
                gameRepository.save(game);
            }
        }
    }

    private Game findGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found with ID: " + gameId));
    }
}
