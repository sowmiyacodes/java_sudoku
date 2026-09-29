export type GameStatus = 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'ABANDONED';

export type Difficulty = 'Easy' | 'Medium' | 'Hard' | 'Expert' | 'Custom';

export interface CellCoordinate {
  row: number;
  col: number;
}

export interface GameState {
  id: number;
  puzzleId: string;
  difficulty: Difficulty;
  difficultyScore?: number;
  metadata?: any;
  status: GameStatus;
  startedAt: string;
  pausedAt: string | null;
  completedAt: string | null;
  elapsedSeconds: number;
  mistakes: number;
  board: number[][];
  initialBoard: number[][];
  completed: boolean;
  canUndo: boolean;
  canRedo: boolean;
}

export interface HintResponse {
  available: boolean;
  level?: number;
  hintText?: string;
  row?: number;
  column?: number;
  value?: number;
  technique?: string;
  explanation?: string;
  candidates?: number[];
  message?: string;
}

export interface PerformanceAnalysis {
  accuracy: number;
  mistakes: number;
  hintsUsed: number;
  movesMade: number;
  elapsedSeconds: number;
  strongArea: string;
  improvementArea: string;
  recommendation: string;
  predictedDifficulty?: string;
  modelConfidence?: number;
  historyCount?: number;
}

export interface HintHistoryItem {
  id: number;
  row: number;
  column: number;
  value: number;
  technique: string;
  explanation: string;
  createdAt: string;
}

export interface MoveResponse {
  valid: boolean;
  row: number;
  column: number;
  value: number;
  reason?: string;
  completed: boolean;
  mistakes: number;
  board: number[][];
  elapsedSeconds: number;
  canUndo: boolean;
  canRedo: boolean;
}

export interface SubmitResponse {
  completed: boolean;
  valid: boolean;
  message: string;
  mistakes: number;
  elapsedSeconds: number;
  incorrectCells: { row: number; column: number }[];
}

export interface CreateGameParams {
  difficulty?: Difficulty;
  puzzleId?: string;
  initialBoard?: number[][];
  solutionBoard?: number[][];
}

export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';

export interface PlayerStatistics {
  userId: number;
  gamesPlayed: number;
  gamesCompleted: number;
  gamesAbandoned: number;
  completionRate: number;
  averageTime: number;
  bestTime: number;
  averageScore: number;
  averageAccuracy: number;
  averageMistakes: number;
  averageHints: number;
  averageUndos: number;
  averageMoves: number;
  hardCompletionRate: number;
  mediumCompletionRate: number;
  easyCompletionRate: number;
  recentAccuracy: number;
  recentErrorRate: number;
  averageMovesPerMinute: number;
  currentStreak?: number;
  bestStreak?: number;
  updatedAt: string;
}

export interface DifficultyPerformance {
  difficulty: string;
  games: number;
  completed: number;
  completionRate: number;
  averageTime: number;
  accuracy: number;
}

export interface PlayerProfile {
  userId: number;
  username: string;
  displayName: string;
  email: string;
  registrationDate: string;
  gamesPlayed: number;
  gamesCompleted: number;
  gamesAbandoned: number;
  completionRate: number;
  averageSolvingTime: number;
  bestSolvingTime: number;
  averageAccuracy: number;
  totalMistakes: number;
  averageMistakesPerGame: number;
  averageHintsPerGame: number;
  averageUndosPerGame: number;
  averageScore: number;
  currentStreak?: number;
  bestStreak?: number;
  currentSkillLevel: SkillLevel | string;
  skillConfidence: number;
  recommendedDifficulty: Difficulty;
  recommendationReason: string;
}

export interface RecommendationData {
  userId: number;
  skillLevel: SkillLevel | string;
  confidence: number;
  recommendedDifficulty: Difficulty;
  reason: string;
  modelVersion: string;
  topFactors: string[];
  mlServiceAvailable: boolean;
}

export interface GameHistoryItem {
  gameId: number;
  puzzleId: string;
  difficulty: string;
  startedAt: string;
  completedAt: string | null;
  duration: number;
  moves: number;
  mistakes: number;
  hints: number;
  undos: number;
  accuracy: number;
  score: number;
  completionStatus: GameStatus | string;
}

export interface GameHistoryDetail extends GameHistoryItem {
  recentMoves: MoveResponse[];
  hintHistory: HintHistoryItem[];
}

export interface MLModelRecord {
  model_id: string;
  model_type: string;
  name: string;
  algorithm: string;
  version: string;
  dataset_version?: string;
  accuracy: number;
  precision: number;
  recall: number;
  f1: number;
  status: 'ACTIVE' | 'ARCHIVED';
  created_at: string;
}

export interface MLExperimentRecord {
  experiment_id: string;
  model_type: string;
  selected_algorithm: string;
  dataset_version: string;
  validation_scores: Record<string, any>;
  test_metrics: Record<string, number>;
  duration_seconds: number;
  timestamp: string;
}

export interface MLOverviewData {
  active_models: number;
  total_models: number;
  total_training_runs: number;
  latest_accuracy: number;
  latest_f1: number;
  total_logged_predictions: number;
  last_training_date: string;
  status: string;
}

