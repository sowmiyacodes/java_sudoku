import { apiFetch } from './http';

export interface PuzzleRecord {
  id: number;
  puzzleId: string;
  puzzle: string;
  difficulty: string;
  predictedDifficulty: string;
  difficultyScore: number | null;
  modelConfidence: number | null;
  source: string;
  rating: number | null;
  featuresJson: string | null;
  topFactorsJson: string | null;
  active: boolean;
  createdAt: string;
}

export interface PuzzleDraft {
  puzzle: string;
  difficulty: string;
  source: string;
  rating: number | null;
  active: boolean;
}

async function readResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed (${response.status})`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export const PuzzleApi = {
  list: async (): Promise<PuzzleRecord[]> => readResponse(await apiFetch('/api/puzzles/manage')),
  analytics: async (): Promise<Record<string, number | Record<string, number>>> =>
    readResponse(await apiFetch('/api/puzzles/manage/analytics')),
  create: async (draft: PuzzleDraft): Promise<PuzzleRecord> => readResponse(await apiFetch('/api/puzzles/manage', {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(draft)
  })),
  update: async (puzzleId: string, draft: PuzzleDraft): Promise<PuzzleRecord> =>
    readResponse(await apiFetch(`/api/puzzles/manage/${encodeURIComponent(puzzleId)}`, {
      method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(draft)
    })),
  remove: async (puzzleId: string): Promise<void> => readResponse(await apiFetch(
    `/api/puzzles/manage/${encodeURIComponent(puzzleId)}`, { method: 'DELETE' }
  )),
};