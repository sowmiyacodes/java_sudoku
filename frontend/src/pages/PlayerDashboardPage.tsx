import React, { useEffect, useState } from 'react';
import {
  Gamepad2,
  CheckCircle,
  Percent,
  Clock,
  Target,
  Trophy,
  Loader2,
  AlertCircle,
  ArrowRight
} from 'lucide-react';
import { PlayerApi } from '../services/playerApi';
import type {
  PlayerStatistics,
  DifficultyPerformance,
  RecommendationData,
  GameHistoryItem,
  Difficulty
} from '../types/sudoku';
import { MLInsightsCard } from '../components/MLInsightsCard';
import { RecommendationCard } from '../components/RecommendationCard';
import { useAuth } from '../context/useAuth';

interface PlayerDashboardPageProps {
  onStartGame: (difficulty: Difficulty) => void;
  onViewHistory: () => void;
  onViewStats: () => void;
}

export const PlayerDashboardPage: React.FC<PlayerDashboardPageProps> = ({
  onStartGame,
  onViewHistory,
  onViewStats
}) => {
  const { user } = useAuth();
  const [stats, setStats] = useState<PlayerStatistics | null>(null);
  const [diffPerf, setDiffPerf] = useState<DifficultyPerformance[]>([]);
  const [recommendation, setRecommendation] = useState<RecommendationData | null>(null);
  const [recentGames, setRecentGames] = useState<GameHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [statsData, diffData, recData, gamesData] = await Promise.all([
        PlayerApi.getMyStatistics(),
        PlayerApi.getMyDifficultyPerformance(),
        PlayerApi.getMyRecommendations(),
        PlayerApi.getMyGames()
      ]);
      setStats(statsData);
      setDiffPerf(diffData);
      setRecommendation(recData);
      setRecentGames(gamesData.slice(0, 5));
    } catch (err: any) {
      console.error('Error fetching player dashboard:', err);
      setError(err.message || 'Failed to load telemetry from server.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const formatSeconds = (sec: number) => {
    if (!sec || sec <= 0) return '0:00';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  if (isLoading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-cyan-200">
        <Loader2 className="w-10 h-10 animate-spin mb-3 text-cyan-400" />
        <p className="text-sm font-semibold text-slate-600">Loading your player dashboard...</p>
      </div>
    );
  }

  return (
    <div className="flex-1 space-y-6 overflow-y-auto bg-slate-50 p-4 text-slate-900 sm:p-6 lg:p-8">
      {/* Header Banner */}
      <div className="flex flex-col justify-between gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <span className="rounded border border-cyan-200 bg-cyan-50 px-2.5 py-1 text-[10px] font-bold uppercase text-cyan-800">Player workspace</span>
            <span className={`inline-flex items-center gap-1.5 rounded border px-2.5 py-1 text-[10px] font-semibold ${recommendation?.mlServiceAvailable ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-amber-200 bg-amber-50 text-amber-800'}`}>
              <span className={`size-1.5 rounded-full ${recommendation?.mlServiceAvailable ? 'bg-emerald-500' : 'bg-amber-500'}`} />
              {recommendation?.mlServiceAvailable ? 'ML personalization active' : 'Fallback coaching active'}
            </span>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-slate-950 sm:text-3xl">
            Welcome back, {user?.displayName || 'player'}
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Your Sudoku performance, Java-powered game history, and personalized ML coaching.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-600">
          <span className="rounded-md border border-slate-200 bg-white px-3 py-2">Spring Boot + H2</span>
          <span className="rounded-md border border-slate-200 bg-white px-3 py-2">Python ML service</span>
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">
          <AlertCircle size={16} className="shrink-0 text-rose-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Summary Metrics Grid */}
      <div className="grid grid-cols-2 gap-3.5 md:grid-cols-3 xl:grid-cols-6">
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Games</span>
            <Gamepad2 size={16} className="text-cyan-400" />
          </div>
          <p className="text-2xl font-black text-slate-950">{stats?.gamesPlayed || 0}</p>
          <span className="text-[10px] text-slate-500">Puzzles started</span>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Completed</span>
            <CheckCircle size={16} className="text-emerald-400" />
          </div>
          <p className="text-2xl font-black text-emerald-700">{stats?.gamesCompleted || 0}</p>
          <span className="text-[10px] text-slate-500">Successfully solved</span>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Completion Rate</span>
            <Percent size={16} className="text-blue-400" />
          </div>
          <p className="text-2xl font-black text-blue-700">
            {stats ? Math.round(stats.completionRate * 100) : 0}%
          </p>
          <span className="text-[10px] text-slate-500">Solve success ratio</span>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Accuracy</span>
            <Target size={16} className="text-indigo-400" />
          </div>
          <p className="text-2xl font-black text-indigo-700">
            {stats ? Math.round(stats.averageAccuracy * 100) : 0}%
          </p>
          <span className="text-[10px] text-slate-500">Valid moves ratio</span>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Avg Time</span>
            <Clock size={16} className="text-purple-400" />
          </div>
          <p className="font-mono text-2xl font-black text-violet-700">
            {formatSeconds(stats?.averageTime || 0)}
          </p>
          <span className="text-[10px] text-slate-500">Best: {formatSeconds(stats?.bestTime || 0)}</span>
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="mb-1 flex items-center justify-between text-slate-500">
            <span className="text-[11px] font-bold uppercase tracking-wider">Avg Score</span>
            <Trophy size={16} className="text-amber-400" />
          </div>
          <p className="text-2xl font-black text-amber-700">{stats?.averageScore || 0}</p>
          <span className="text-[10px] text-slate-500">Points per mission</span>
        </div>
      </div>

      {/* Intelligence & Personalization Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <MLInsightsCard recommendation={recommendation} isLoading={isLoading} />
        <RecommendationCard
          recommendation={recommendation}
          isLoading={isLoading}
          onRefresh={async () => {
            const refreshed = await PlayerApi.refreshRecommendations();
            setRecommendation(refreshed);
          }}
          onLaunchPuzzle={(diff) => onStartGame(diff)}
        />
      </div>

      {/* Difficulty Breakdown Table */}
      <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-slate-900">
            Difficulty Tier Performance
          </h3>
          <button
            onClick={onViewStats}
            className="flex items-center gap-1 text-xs font-semibold text-cyan-800 hover:text-cyan-600 cursor-pointer"
          >
            <span>Deep Telemetry</span>
            <ArrowRight size={13} />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider">
                <th className="py-2.5 px-3">Difficulty</th>
                <th className="py-2.5 px-3 text-right">Missions</th>
                <th className="py-2.5 px-3 text-right">Completed</th>
                <th className="py-2.5 px-3 text-right">Completion Rate</th>
                <th className="py-2.5 px-3 text-right">Avg Time</th>
                <th className="py-2.5 px-3 text-right">Accuracy</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {diffPerf.map((d) => (
                <tr key={d.difficulty} className="transition-colors hover:bg-slate-50">
                  <td className="flex items-center gap-2 px-3 py-3 font-bold text-slate-800">
                    <span
                      className={`w-2 h-2 rounded-full ${
                        d.difficulty.toLowerCase() === 'hard'
                          ? 'bg-rose-400'
                          : d.difficulty.toLowerCase() === 'medium'
                          ? 'bg-amber-400'
                          : 'bg-emerald-400'
                      }`}
                    ></span>
                    {d.difficulty}
                  </td>
                  <td className="px-3 py-3 text-right font-mono text-slate-700">{d.games}</td>
                  <td className="px-3 py-3 text-right font-mono text-emerald-700">{d.completed}</td>
                  <td className="px-3 py-3 text-right font-mono text-blue-700">
                    {Math.round(d.completionRate * 100)}%
                  </td>
                  <td className="px-3 py-3 text-right font-mono text-violet-700">
                    {formatSeconds(d.averageTime)}
                  </td>
                  <td className="px-3 py-3 text-right font-mono text-cyan-800">
                    {Math.round(d.accuracy * 100)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Games List */}
      <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-bold text-slate-900">
            Recent Mission History
          </h3>
          <button
            onClick={onViewHistory}
            className="flex items-center gap-1 text-xs font-semibold text-cyan-800 hover:text-cyan-600 cursor-pointer"
          >
            <span>View All Games</span>
            <ArrowRight size={13} />
          </button>
        </div>

        {recentGames.length === 0 ? (
          <p className="py-3 text-center text-sm italic text-slate-500">
            No games recorded yet. Start a puzzle to build your history.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500 font-bold uppercase tracking-wider">
                  <th className="py-2.5 px-3">Mission ID</th>
                  <th className="py-2.5 px-3">Difficulty</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Duration</th>
                  <th className="py-2.5 px-3 text-right">Accuracy</th>
                  <th className="py-2.5 px-3 text-right">Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentGames.map((g) => (
                  <tr key={g.gameId} className="transition-colors hover:bg-slate-50">
                    <td className="px-3 py-2.5 font-mono text-slate-700">#{g.gameId}</td>
                    <td className="px-3 py-2.5 font-semibold text-slate-800">{g.difficulty}</td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                          g.completionStatus === 'COMPLETED'
                            ? 'border border-emerald-200 bg-emerald-50 text-emerald-800'
                            : 'bg-slate-100 text-slate-600'
                        }`}
                      >
                        {g.completionStatus}
                      </span>
                    </td>
                    <td className="px-3 py-2.5 text-right font-mono text-violet-700">
                      {formatSeconds(g.duration)}
                    </td>
                    <td className="px-3 py-2.5 text-right font-mono text-cyan-800">
                      {Math.round(g.accuracy * 100)}%
                    </td>
                    <td className="px-3 py-2.5 text-right font-mono font-bold text-amber-700">
                      {g.score}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
