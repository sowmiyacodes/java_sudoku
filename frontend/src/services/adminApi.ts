import { apiFetch } from './http';
import type { DifficultyPerformance, PlayerProfile, RecommendationData } from '../types/sudoku';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || `Request failed with status ${res.status}`);
  }
  return res.json();
}

export interface AdminAnalyticsOverview {
  totalPlayers: number;
  totalGames: number;
  completedGames: number;
  activeGames: number;
  totalPuzzles: number;
  multiplayerMatches: number;
  averageAccuracy: number;
  averageSolvingTime: number;
  mlModels: number;
  mlPredictions: number;
}

export interface AdminPlayerRecord {
  id: number;
  username: string;
  displayName: string;
  email: string;
  gamesCount: number;
  winRate: number;
  accuracy: number;
  skill: string;
  joined: string;
  active: boolean;
}

export interface AdminPlayerDetails {
  profile: PlayerProfile;
  recommendation: RecommendationData;
  difficultyPerformance: DifficultyPerformance[];
}

export interface AdminGameRecord {
  id: number;
  username: string;
  puzzleId: string;
  difficulty: string;
  durationSeconds: number;
  score: number;
  mistakes: number;
  hints: number;
  status: string;
  completed: boolean;
  created: string;
}

export interface AdminHintRecord {
  id: number;
  gameId: number;
  hintType: string;
  targetCell: string;
  value: number;
  explanation: string;
  timestamp: string;
}

export interface AdminPuzzleRecord {
  id: number;
  puzzleId: string;
  givensCount: number;
  difficulty: string;
  predictedDifficulty: string;
  confidence: number;
  timesPlayed: number;
  avgSolvingTime: number;
  created: string;
}

export interface AdminAuditLogRecord {
  id: number;
  adminUsername: string;
  action: string;
  entity: string;
  entityId: string;
  description: string;
  timestamp: string;
}

export const AdminApi = {
  getAnalyticsOverview: async (): Promise<AdminAnalyticsOverview> => {
    const res = await apiFetch('/api/admin/analytics/overview');
    return handleResponse<AdminAnalyticsOverview>(res);
  },

  getAnalyticsCharts: async (): Promise<any> => {
    const res = await apiFetch('/api/admin/analytics/charts');
    return handleResponse<any>(res);
  },

  getPlayers: async (): Promise<AdminPlayerRecord[]> => {
    const res = await apiFetch('/api/admin/players');
    return handleResponse<AdminPlayerRecord[]>(res);
  },

  getPlayerDetails: async (id: number): Promise<AdminPlayerDetails> => {
    const res = await apiFetch(`/api/admin/players/${id}/details`);
    return handleResponse<AdminPlayerDetails>(res);
  },

  updatePlayerStatus: async (id: number, active: boolean): Promise<any> => {
    const res = await apiFetch(`/api/admin/players/${id}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ active })
    });
    return handleResponse<any>(res);
  },

  getGames: async (): Promise<AdminGameRecord[]> => {
    const res = await apiFetch('/api/admin/games');
    return handleResponse<AdminGameRecord[]>(res);
  },

  getHints: async (): Promise<AdminHintRecord[]> => {
    const res = await apiFetch('/api/admin/hints');
    return handleResponse<AdminHintRecord[]>(res);
  },

  getPuzzles: async (): Promise<AdminPuzzleRecord[]> => {
    const res = await apiFetch('/api/admin/puzzles');
    return handleResponse<AdminPuzzleRecord[]>(res);
  },

  getAuditLogs: async (entity?: string, action?: string): Promise<AdminAuditLogRecord[]> => {
    let url = '/api/admin/audit-logs';
    const params = new URLSearchParams();
    if (entity) params.append('entity', entity);
    if (action) params.append('action', action);
    if (params.toString()) url += `?${params.toString()}`;

    const res = await apiFetch(url);
    return handleResponse<AdminAuditLogRecord[]>(res);
  },

  createAuditLog: async (log: { adminUsername?: string; action: string; entity: string; entityId?: string; description: string }): Promise<AdminAuditLogRecord> => {
    const res = await apiFetch('/api/admin/audit-logs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(log)
    });
    return handleResponse<AdminAuditLogRecord>(res);
  }
};
