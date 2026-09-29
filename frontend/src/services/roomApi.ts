import { apiFetch } from './http';
import type { GameState, HintResponse, MoveResponse, SubmitResponse } from '../types/sudoku';

export type RoomStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED' | 'EXPIRED';
export type RoomRole = 'HOST' | 'GUEST';

export interface RoomPlayer {
  userId: number;
  username: string;
  displayName: string;
  role: RoomRole;
  active: boolean;
  connected: boolean;
  joinedAt: string;
  finishedAt: string | null;
  pointsAwarded: number | null;
}

export interface RoomState {
  roomCode: string;
  status: RoomStatus;
  difficulty: string;
  hostUsername: string;
  guestUsername: string | null;
  stateVersion: number;
  createdAt: string;
  lastActivityAt: string;
  you: RoomPlayer;
  players: RoomPlayer[];
  game: GameState | null;
}

export interface RoomMoveResult {
  move: MoveResponse;
  stateVersion: number;
}

export class RoomApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'RoomApiError';
    this.status = status;
  }
}

function fallbackMessage(status: number): string {
  switch (status) {
    case 401:
      return 'Please log in to play multiplayer.';
    case 403:
      return 'You are not a member of this room.';
    case 404:
      return 'Room not found. Check the code and try again.';
    case 409:
      return 'This room cannot accept that request right now.';
    case 410:
      return 'This room has expired.';
    default:
      return `Request failed (${status}).`;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await apiFetch(path, init);
  if (!response.ok) {
    let message = '';
    try {
      const body = (await response.json()) as { message?: string } | null;
      message = body?.message ?? '';
    } catch {
      // Empty or non-JSON error body (e.g. 401 from the security filter).
    }
    throw new RoomApiError(response.status, message || fallbackMessage(response.status));
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

function jsonInit(method: string, body?: unknown): RequestInit {
  return {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body ?? {}),
  };
}

const base = '/api/rooms';

export const RoomApi = {
  /** Create a new room (authenticated). Returns the room state with its code. */
  createRoom(difficulty: string): Promise<RoomState> {
    return request<RoomState>(base, jsonInit('POST', { difficulty }));
  },

  /** Join a room by code; also used to reconnect to a room you belong to. */
  joinRoom(roomCode: string): Promise<RoomState> {
    return request<RoomState>(`${base}/join`, jsonInit('POST', { roomCode }));
  },

  /** Poll the full room state (members only; doubles as a presence heartbeat). */
  getState(roomCode: string): Promise<RoomState> {
    return request<RoomState>(`${base}/${encodeURIComponent(roomCode)}`);
  },

  /** Host starts the shared game using the existing puzzle service. */
  startGame(roomCode: string): Promise<RoomState> {
    return request<RoomState>(`${base}/${encodeURIComponent(roomCode)}/start`, { method: 'POST' });
  },

  /**
   * Shared move. `expectedStateVersion` is the last state version this client
   * saw; a stale value is rejected with 409 so the write cannot clobber a
   * teammate's newer move.
   */
  makeMove(
    roomCode: string,
    row: number,
    column: number,
    value: number,
    expectedStateVersion: number,
  ): Promise<RoomMoveResult> {
    return request<RoomMoveResult>(`${base}/${encodeURIComponent(roomCode)}/move`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ row, column, value, expectedStateVersion }),
    });
  },

  /**
   * "Submit Solution" on the shared board: reports the empty/incorrect cells
   * like the solo page, or completes the room and awards both players when
   * the puzzle is solved. Idempotent for an already-finished room.
   */
  submit(roomCode: string): Promise<SubmitResponse> {
    return request<SubmitResponse>(`${base}/${encodeURIComponent(roomCode)}/submit`, { method: 'POST' });
  },

  /**
   * Shared hint for the current board. It is recorded in the room's shared
   * hint history, so it counts against BOTH players' final scores.
   */
  requestHint(roomCode: string, level = 3): Promise<HintResponse> {
    return request<HintResponse>(`${base}/${encodeURIComponent(roomCode)}/hint?level=${level}`, {
      method: 'POST',
    });
  },

  /** Leave the room (marks this player disconnected; room rules apply). */
  async leaveRoom(roomCode: string): Promise<void> {
    await request<void>(`${base}/${encodeURIComponent(roomCode)}/leave`, { method: 'POST' });
  },
};
