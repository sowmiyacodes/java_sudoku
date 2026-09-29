import type { CreateGameParams, GameState, MoveResponse, PerformanceAnalysis, SubmitResponse } from '../types/sudoku';
import { apiFetch } from './http';

const API_BASE_URL = '/api/games';

export class GameApi {
  /**
   * Starts a new game with the chosen difficulty or puzzle
   */
  static async createGame(params?: CreateGameParams): Promise<GameState> {
    const response = await apiFetch(API_BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params || {}),
    });
    if (!response.ok) {
      throw new Error(`Failed to create game: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Retrieves game state by ID
   */
  static async getGame(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}`);
    if (!response.ok) {
      throw new Error(`Failed to fetch game #${gameId}: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Retrieves the most recent unfinished game (for Continue Game)
   */
  static async getActiveGame(): Promise<GameState | null> {
    const response = await apiFetch(`${API_BASE_URL}/active`);
    if (response.status === 204) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`Failed to check for active game: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Submits a player move (1-9 or 0 to erase)
   */
  static async makeMove(gameId: number, row: number, column: number, value: number): Promise<MoveResponse> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/move`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ row, column, value }),
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to submit move');
    }
    return response.json();
  }

  /**
   * Undo the most recent move
   */
  static async undo(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/undo`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to undo move: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Redo an undone move
   */
  static async redo(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/redo`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to redo move: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Pause the game
   */
  static async pause(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/pause`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to pause game: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Resume the game
   */
  static async resume(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/resume`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to resume game: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Submit and validate the entire completed board
   */
  static async submit(gameId: number): Promise<SubmitResponse> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/submit`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to submit board: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Request a hint for the current board
   */
  static async getHint(gameId: number, level = 3): Promise<import('../types/sudoku').HintResponse> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/hint?level=${level}`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to get hint: ${response.statusText}`);
    }
    return response.json();
  }

  static async getPerformanceAnalysis(gameId: number): Promise<PerformanceAnalysis> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/analysis`);
    if (!response.ok) {
      throw new Error(`Failed to fetch performance analysis: ${response.statusText}`);
    }
    return response.json();
  }

  static async getPerformanceHistory(): Promise<PerformanceAnalysis[]> {
    const response = await apiFetch(`${API_BASE_URL}/analysis/history`);
    if (!response.ok) {
      throw new Error(`Failed to fetch performance history: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Restarts the current game back to original puzzle board
   */
  static async restart(gameId: number): Promise<GameState> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/restart`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(`Failed to restart game: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Generate puzzle endpoint (Member 2 API)
   */
  static async generatePuzzle(difficulty?: import('../types/sudoku').Difficulty): Promise<any> {
    const response = await apiFetch('/api/puzzles/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ difficulty: difficulty || 'Medium' }),
    });
    if (!response.ok) {
      throw new Error(`Failed to generate puzzle: ${response.statusText}`);
    }
    return response.json();
  }

  /**
   * Fetch hint history for the current game
   */
  static async getHintHistory(gameId: number): Promise<import('../types/sudoku').HintHistoryItem[]> {
    const response = await apiFetch(`${API_BASE_URL}/${gameId}/hints`);
    if (!response.ok) {
      throw new Error(`Failed to fetch hint history: ${response.statusText}`);
    }
    return response.json();
  }
}
