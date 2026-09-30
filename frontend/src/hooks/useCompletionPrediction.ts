import { useEffect, useRef, useState } from 'react';
import type { CompletionPrediction, CompletionPredictionRequest, GameState, SkillLevel } from '../types/sudoku';
import { useAuth } from '../context/useAuth';
import { PlayerApi } from '../services/playerApi';
import { GameApi } from '../services/gameApi';

interface CompletionPredictionState {
  prediction: CompletionPrediction | null;
  error: string | null;
  loading: boolean;
  profileNotice: string | null;
}

function inferSkillLevel(completionRate: number, accuracy: number): SkillLevel {
  if (completionRate >= 0.9 && accuracy >= 0.95) return 'EXPERT';
  if (completionRate >= 0.75 && accuracy >= 0.88) return 'ADVANCED';
  if (completionRate >= 0.5 && accuracy >= 0.75) return 'INTERMEDIATE';
  return 'BEGINNER';
}

function currentProgress(game: GameState): number {
  let emptyCells = 0;
  let filledCells = 0;
  for (let row = 0; row < 9; row += 1) {
    for (let column = 0; column < 9; column += 1) {
      if (game.initialBoard[row]?.[column] === 0) {
        emptyCells += 1;
        if (game.board[row]?.[column] !== 0) filledCells += 1;
      }
    }
  }
  return emptyCells > 0 ? filledCells / emptyCells : 1;
}

export function useCompletionPrediction(
  game: GameState | null,
  hintsUsed: number,
  enabled: boolean,
): CompletionPredictionState {
  const { user } = useAuth();
  const userId = user?.id ?? null;
  const [profileState, setProfileState] = useState<{
    userId: number | null;
    profile: {
      completionRate: number;
      averageTime: number;
      recentAccuracy: number;
      currentStreak: number;
    } | null;
    notice: string | null;
  }>({ userId: null, profile: null, notice: null });
  const profile = profileState.userId === userId ? profileState.profile : null;
  const profileNotice = userId === null
    ? 'Player history is unavailable in guest mode; using a baseline profile.'
    : profileState.userId === userId
      ? profileState.notice
      : null;
  const [prediction, setPrediction] = useState<CompletionPrediction | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const latestRef = useRef({ game, hintsUsed, profile });
  const boardKey = game?.board.map((row) => row.join('')).join('') ?? '';
  const initialBoardKey = game?.initialBoard.map((row) => row.join('')).join('') ?? '';
  const gameId = game?.id;
  const gameStatus = game?.status;

  useEffect(() => {
    latestRef.current = { game, hintsUsed, profile };
  }, [game, hintsUsed, profile]);

  useEffect(() => {
    let active = true;

    if (userId === null) return;

    PlayerApi.getMyStatistics()
      .then((stats) => {
        if (active) {
          setProfileState({
            userId,
            profile: {
              completionRate: stats.completionRate,
              averageTime: stats.averageTime || 450,
              recentAccuracy: stats.recentAccuracy,
              currentStreak: stats.currentStreak ?? 0,
            },
            notice: null,
          });
        }
      })
      .catch((caught: unknown) => {
        if (active) {
          setProfileState({
            userId,
            profile: null,
            notice: caught instanceof Error ? caught.message : 'Player history could not be loaded.',
          });
        }
      });

    return () => {
      active = false;
    };
  }, [userId]);

  useEffect(() => {
    if (gameId === undefined || !enabled || gameStatus !== 'IN_PROGRESS') return;

    let active = true;
    const requestPrediction = () => {
      const latest = latestRef.current;
      const currentGame = latest.game;
      if (!currentGame) return;
      const completionRate = latest.profile?.completionRate ?? 0.75;
      const averageTime = latest.profile?.averageTime ?? 450;
      const recentAccuracy = latest.profile?.recentAccuracy ?? 0.88;
      const request: CompletionPredictionRequest = {
        skill_level: inferSkillLevel(completionRate, recentAccuracy),
        difficulty: currentGame.difficulty,
        historical_completion_rate: completionRate,
        average_solving_time: averageTime,
        recent_accuracy: recentAccuracy,
        hints_used: latest.hintsUsed,
        mistakes_made: currentGame.mistakes,
        current_streak: latest.profile?.currentStreak ?? 0,
        current_progress: currentProgress(currentGame),
        elapsed_time: currentGame.elapsedSeconds,
      };
      setLoading(true);
      GameApi.predictCompletion(request)
        .then((result) => {
          if (active) {
            setPrediction(result);
            setError(null);
          }
        })
        .catch((caught: unknown) => {
          if (active) {
            setPrediction(null);
            setError(caught instanceof Error ? caught.message : 'Completion prediction is unavailable.');
          }
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    };

    void requestPrediction();
    const intervalId = window.setInterval(requestPrediction, 30_000);
    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, [
    enabled,
    gameId,
    gameStatus,
    game?.difficulty,
    game?.mistakes,
    boardKey,
    initialBoardKey,
    hintsUsed,
    profile,
  ]);

  return { prediction, error, loading, profileNotice };
}
