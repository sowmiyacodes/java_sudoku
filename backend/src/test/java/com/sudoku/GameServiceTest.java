package com.sudoku;

import com.sudoku.dto.*;
import com.sudoku.model.Game;
import com.sudoku.model.GameStatus;
import com.sudoku.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GameServiceTest {

    @Autowired
    private GameService gameService;

    private int[][] createSampleInitialBoard() {
        return new int[][] {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };
    }

    private int[][] createSampleSolutionBoard() {
        return new int[][] {
            {5, 3, 4, 6, 7, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {4, 2, 6, 8, 5, 3, 7, 9, 1},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 8, 6, 1, 7, 9}
        };
    }

    private GameResponse startSampleGame() {
        CreateGameRequest req = new CreateGameRequest();
        req.setDifficulty("Medium");
        req.setInitialBoard(createSampleInitialBoard());
        req.setSolutionBoard(createSampleSolutionBoard());
        return gameService.createGame(req);
    }

    @Test
    public void testValidMove() {
        GameResponse game = startSampleGame();
        // (0, 2) is empty, valid placement is 4 according to solution and constraints
        MoveRequest moveReq = new MoveRequest(0, 2, 4);
        MoveResponse res = gameService.makeMove(game.getId(), moveReq);

        assertTrue(res.isValid(), "Move 4 at (0, 2) must be valid");
        assertEquals(4, res.getBoard()[0][2]);
        assertEquals(0, res.getMistakes());
        assertTrue(res.isCanUndo());
    }

    @Test
    public void testInvalidMoveAndMistakeCounting() {
        GameResponse game = startSampleGame();
        // (0, 2) is empty, but 5 already exists in row 0 at (0,0)
        MoveRequest moveReq = new MoveRequest(0, 2, 5);
        MoveResponse res = gameService.makeMove(game.getId(), moveReq);

        assertFalse(res.isValid(), "Placing 5 in row 0 should be invalid due to duplicate");
        assertEquals(1, res.getMistakes(), "Mistakes count should increment on invalid move");
    }

    @Test
    public void testFixedCellProtection() {
        GameResponse game = startSampleGame();
        // (0, 0) is initial fixed cell with value 5
        MoveRequest moveReq = new MoveRequest(0, 0, 9);
        MoveResponse res = gameService.makeMove(game.getId(), moveReq);

        assertFalse(res.isValid());
        assertTrue(res.getReason().contains("original puzzle cell"));
        assertEquals(5, res.getBoard()[0][0], "Original fixed cell value must not change");
    }

    @Test
    public void testUndoAndRedo() {
        GameResponse game = startSampleGame();
        MoveRequest moveReq = new MoveRequest(0, 2, 4);
        gameService.makeMove(game.getId(), moveReq);

        // Undo
        GameResponse undone = gameService.undoMove(game.getId());
        assertEquals(0, undone.getBoard()[0][2], "Board cell should be reverted to 0 after undo");
        assertTrue(undone.isCanRedo());

        // Redo
        GameResponse redone = gameService.redoMove(game.getId());
        assertEquals(4, redone.getBoard()[0][2], "Board cell should be restored to 4 after redo");
        assertFalse(redone.isCanRedo());
    }

    @Test
    public void testPauseAndResume() {
        GameResponse game = startSampleGame();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());

        GameResponse paused = gameService.pauseGame(game.getId());
        assertEquals(GameStatus.PAUSED, paused.getStatus());

        // Cannot make moves while paused
        assertThrows(ResponseStatusException.class, () -> {
            gameService.makeMove(game.getId(), new MoveRequest(0, 2, 4));
        });

        GameResponse resumed = gameService.resumeGame(game.getId());
        assertEquals(GameStatus.IN_PROGRESS, resumed.getStatus());
    }

    @Test
    public void testRestart() {
        GameResponse game = startSampleGame();
        // Make move
        gameService.makeMove(game.getId(), new MoveRequest(0, 2, 4));

        // Restart
        GameResponse restarted = gameService.restartGame(game.getId());
        assertEquals(0, restarted.getBoard()[0][2], "Cell must be cleared on restart");
        assertEquals(0, restarted.getMistakes());
        assertEquals(GameStatus.IN_PROGRESS, restarted.getStatus());
        assertFalse(restarted.isCanUndo());
    }

    @Test
    public void testIncorrectSolutionSubmission() {
        GameResponse game = startSampleGame();
        SubmitResponse sub = gameService.submitGame(game.getId());

        assertFalse(sub.isValid());
        assertFalse(sub.isCompleted());
        assertTrue(sub.getEmptyCells() > 0);
    }

    @Test
    public void testCorrectSolutionSubmissionAndCompletedProtection() {
        // Create game almost complete, with only 1 empty cell
        int[][] almostDone = createSampleSolutionBoard();
        almostDone[8][8] = 0; // last cell empty

        CreateGameRequest req = new CreateGameRequest();
        req.setDifficulty("Easy");
        req.setInitialBoard(almostDone);
        req.setSolutionBoard(createSampleSolutionBoard());
        GameResponse game = gameService.createGame(req);

        // Fill last cell with 9
        MoveResponse lastMove = gameService.makeMove(game.getId(), new MoveRequest(8, 8, 9));
        assertTrue(lastMove.isCompleted());

        // Now verify submit returns success
        SubmitResponse sub = gameService.submitGame(game.getId());
        assertTrue(sub.isCompleted());
        assertTrue(sub.isValid());

        // Completed game must reject moves
        assertThrows(ResponseStatusException.class, () -> {
            gameService.makeMove(game.getId(), new MoveRequest(8, 8, 1));
        });
    }
}
