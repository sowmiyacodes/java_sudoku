package com.sudoku;

import com.sudoku.dto.CreateGameRequest;
import com.sudoku.dto.GameResponse;
import com.sudoku.dto.HintHistoryResponse;
import com.sudoku.dto.HintResponse;
import com.sudoku.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HintHistoryTest {

    @Autowired
    private GameService gameService;

    private int[][] createBoardWithSingle() {
        // Solved board with only 1 empty cell at (0, 0)
        return new int[][] {
            {0, 3, 4, 6, 7, 8, 9, 1, 2},
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

    private int[][] createFullSolvedBoard() {
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

    private GameResponse createGameWithBoard(int[][] initialBoard) {
        CreateGameRequest req = new CreateGameRequest();
        req.setDifficulty("Easy");
        req.setInitialBoard(initialBoard);
        req.setSolutionBoard(createFullSolvedBoard());
        return gameService.createGame(req);
    }

    @Test
    public void testSuccessfulHintCreatesHistoryRecord() {
        GameResponse game = createGameWithBoard(createBoardWithSingle());
        HintResponse hint = gameService.requestHint(game.getId());

        assertTrue(hint.isAvailable());
        assertEquals(0, hint.getRow());
        assertEquals(0, hint.getColumn());
        assertEquals(5, hint.getValue());

        List<HintHistoryResponse> history = gameService.getHintHistory(game.getId());
        assertFalse(history.isEmpty());
        HintHistoryResponse first = history.get(0);
        assertEquals(0, first.getRow());
        assertEquals(0, first.getColumn());
        assertEquals(5, first.getValue());
        assertNotNull(first.getTechnique());
        assertNotNull(first.getExplanation());
        assertNotNull(first.getCreatedAt());
    }

    @Test
    public void testNoHintDoesNotCreateHistoryRecord() {
        // Completely solved board -> no hint available
        GameResponse game = createGameWithBoard(createFullSolvedBoard());
        
        List<HintHistoryResponse> initialHistory = gameService.getHintHistory(game.getId());
        assertTrue(initialHistory.isEmpty());

        HintResponse hint = gameService.requestHint(game.getId());
        assertFalse(hint.isAvailable());

        List<HintHistoryResponse> afterHistory = gameService.getHintHistory(game.getId());
        assertTrue(afterHistory.isEmpty(), "No hint should not create a history record");
    }

    @Test
    public void testHintHistoryReturnsForCorrectGameOnly() {
        GameResponse gameA = createGameWithBoard(createBoardWithSingle());
        GameResponse gameB = createGameWithBoard(createBoardWithSingle());

        // Request hint for Game A only
        gameService.requestHint(gameA.getId());

        List<HintHistoryResponse> historyA = gameService.getHintHistory(gameA.getId());
        List<HintHistoryResponse> historyB = gameService.getHintHistory(gameB.getId());

        assertEquals(1, historyA.size(), "Game A must have 1 hint recorded");
        assertEquals(0, historyB.size(), "Game B must have 0 hints recorded");
    }

    @Test
    public void testHintHistoryOrderedNewestFirst() throws InterruptedException {
        // Board with 2 empty cells: (0,0) which needs 5, and (0,1) which needs 3
        int[][] board = createFullSolvedBoard();
        board[0][0] = 0;
        board[0][1] = 0;

        GameResponse game = createGameWithBoard(board);

        // First hint
        HintResponse hint1 = gameService.requestHint(game.getId());
        assertTrue(hint1.isAvailable());

        Thread.sleep(100);

        // Second hint
        HintResponse hint2 = gameService.requestHint(game.getId());
        assertTrue(hint2.isAvailable());

        List<HintHistoryResponse> history = gameService.getHintHistory(game.getId());
        assertEquals(2, history.size());

        // Check newest is first
        assertTrue(history.get(0).getCreatedAt().isAfter(history.get(1).getCreatedAt()) ||
                   history.get(0).getCreatedAt().isEqual(history.get(1).getCreatedAt()));
        assertTrue(history.get(0).getId() > history.get(1).getId());
    }
}
