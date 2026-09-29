import { apiFetch } from './http';

export interface LeaderboardEntry {
  rank: number;
  userId: number;
  username: string;
  displayName: string;
  totalPoints: number;
  gamesCompleted: number;
  averageAccuracy: number;
  bestTime: number;
}

export interface PublicPlayerProfile {
  userId: number;
  username: string;
  displayName: string;
  memberSince: string;
  allTimeRank: number | null;
  totalPoints: number;
  gamesCompleted: number;
  averageAccuracy: number;
  bestTime: number;
  recentScores: ScoreHistory[];
}

export interface ScoreHistory {
  gameId: number;
  difficulty: string;
  points: number;
  mistakes: number;
  hintsUsed: number;
  elapsedSeconds: number;
  accuracy: number;
  completedAt: string;
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string; detail?: string } | null;
    throw new Error(body?.message ?? body?.detail ?? `Request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const LeaderboardApi = {
  async getLeaderboard(period: string = 'all-time', limit?: number): Promise<LeaderboardEntry[]> {
    const params = new URLSearchParams({ period });
    if (limit) params.set('limit', String(limit));
    return readResponse(await apiFetch(`/api/leaderboard?${params}`));
  },

  async getMyRank(period: string = 'all-time'): Promise<LeaderboardEntry | null> {
    const response = await apiFetch(`/api/leaderboard/me?period=${period}`);
    if (response.status === 204) return null;
    return readResponse(response);
  },

  async getPublicProfile(username: string): Promise<PublicPlayerProfile> {
    return readResponse(await apiFetch(`/api/leaderboard/profile/${encodeURIComponent(username)}`));
  },

  async getMyHistory(): Promise<ScoreHistory[]> {
    return readResponse(await apiFetch('/api/leaderboard/history'));
  },
};
