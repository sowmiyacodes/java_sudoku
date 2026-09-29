import { apiFetch } from './http';
import type {
  PlayerProfile,
  PlayerStatistics,
  DifficultyPerformance,
  RecommendationData,
  GameHistoryItem,
  GameHistoryDetail
} from '../types/sudoku';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const errorText = await res.text();
    throw new Error(errorText || `Request failed with status ${res.status}`);
  }
  return res.json();
}

export const PlayerApi = {
  getMyProfile: async (): Promise<PlayerProfile> => {
    const res = await apiFetch('/api/players/me/profile');
    return handleResponse<PlayerProfile>(res);
  },

  getPlayerProfile: async (playerId: number): Promise<PlayerProfile> => {
    const res = await apiFetch(`/api/players/${playerId}/profile`);
    return handleResponse<PlayerProfile>(res);
  },

  getMyStatistics: async (): Promise<PlayerStatistics> => {
    const res = await apiFetch('/api/players/me/statistics');
    return handleResponse<PlayerStatistics>(res);
  },

  getMyDifficultyPerformance: async (): Promise<DifficultyPerformance[]> => {
    const res = await apiFetch('/api/players/me/difficulty-performance');
    return handleResponse<DifficultyPerformance[]>(res);
  },

  getMyRecommendations: async (): Promise<RecommendationData> => {
    const res = await apiFetch('/api/players/me/recommendations');
    return handleResponse<RecommendationData>(res);
  },

  refreshRecommendations: async (): Promise<RecommendationData> => {
    const res = await apiFetch('/api/players/me/recommendations/refresh', {
      method: 'POST'
    });
    return handleResponse<RecommendationData>(res);
  },

  getMyGames: async (params?: { difficulty?: string; status?: string; search?: string }): Promise<GameHistoryItem[]> => {
    const query = new URLSearchParams();
    if (params?.difficulty) query.set('difficulty', params.difficulty);
    if (params?.status) query.set('status', params.status);
    if (params?.search) query.set('search', params.search);
    const qs = query.toString() ? `?${query.toString()}` : '';
    const res = await apiFetch(`/api/players/me/games${qs}`);
    return handleResponse<GameHistoryItem[]>(res);
  },

  getMyGameDetail: async (gameId: number): Promise<GameHistoryDetail> => {
    const res = await apiFetch(`/api/players/me/games/${gameId}`);
    return handleResponse<GameHistoryDetail>(res);
  }
};
