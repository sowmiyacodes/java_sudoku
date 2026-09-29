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
        <p className="text-sm font-semibold tracking-wider font-mono">Loading telemetry & models from H2...</p>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <span>Pilot Analytics Dashboard</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1 font-mono">
            H2 Persisted Telemetry • Python Scikit-Learn Engine
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-950/60 border border-rose-500/40 text-rose-200 text-xs flex items-center gap-2">
          <AlertCircle size={16} className="text-rose-400 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Summary Metrics Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3.5">
        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Games</span>
            <Gamepad2 size={16} className="text-cyan-400" />
          </div>
          <p className="text-2xl font-black text-white">{stats?.gamesPlayed || 0}</p>
          <span className="text-[10px] text-slate-500">Missions initiated</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Completed</span>
            <CheckCircle size={16} className="text-emerald-400" />
          </div>
          <p className="text-2xl font-black text-emerald-300">{stats?.gamesCompleted || 0}</p>
          <span className="text-[10px] text-slate-500">Successfully solved</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Completion Rate</span>
            <Percent size={16} className="text-blue-400" />
          </div>
          <p className="text-2xl font-black text-blue-300">
            {stats ? Math.round(stats.completionRate * 100) : 0}%
          </p>
          <span className="text-[10px] text-slate-500">Solve success ratio</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Accuracy</span>
            <Target size={16} className="text-indigo-400" />
          </div>
          <p className="text-2xl font-black text-indigo-300">
            {stats ? Math.round(stats.averageAccuracy * 100) : 0}%
          </p>
          <span className="text-[10px] text-slate-500">Valid moves ratio</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Avg Time</span>
            <Clock size={16} className="text-purple-400" />
          </div>
          <p className="text-2xl font-black text-purple-300 font-mono">
            {formatSeconds(stats?.averageTime || 0)}
          </p>
          <span className="text-[10px] text-slate-500">Best: {formatSeconds(stats?.bestTime || 0)}</span>
        </div>

        <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800 backdrop-blur">
          <div className="flex items-center justify-between text-slate-400 mb-1">
            <span className="text-[11px] font-bold uppercase tracking-wider">Avg Score</span>
            <Trophy size={16} className="text-amber-400" />
          </div>
          <p className="text-2xl font-black text-amber-300">{stats?.averageScore || 0}</p>
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
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
            Difficulty Tier Performance
          </h3>
          <button
            onClick={onViewStats}
            className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 flex items-center gap-1 cursor-pointer"
          >
            <span>Deep Telemetry</span>
            <ArrowRight size={13} />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 font-bold uppercase tracking-wider">
                <th className="py-2.5 px-3">Difficulty</th>
                <th className="py-2.5 px-3 text-right">Missions</th>
                <th className="py-2.5 px-3 text-right">Completed</th>
                <th className="py-2.5 px-3 text-right">Completion Rate</th>
                <th className="py-2.5 px-3 text-right">Avg Time</th>
                <th className="py-2.5 px-3 text-right">Accuracy</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {diffPerf.map((d) => (
                <tr key={d.difficulty} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3 px-3 font-bold text-slate-200 flex items-center gap-2">
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
                  <td className="py-3 px-3 text-right font-mono text-slate-300">{d.games}</td>
                  <td className="py-3 px-3 text-right font-mono text-emerald-300">{d.completed}</td>
                  <td className="py-3 px-3 text-right font-mono text-blue-300">
                    {Math.round(d.completionRate * 100)}%
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-purple-300">
                    {formatSeconds(d.averageTime)}
                  </td>
                  <td className="py-3 px-3 text-right font-mono text-cyan-300">
                    {Math.round(d.accuracy * 100)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Recent Games List */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 backdrop-blur-md">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xs font-bold uppercase tracking-wider text-slate-200">
            Recent Mission History
          </h3>
          <button
            onClick={onViewHistory}
            className="text-xs font-semibold text-cyan-400 hover:text-cyan-300 flex items-center gap-1 cursor-pointer"
          >
            <span>View All Games</span>
            <ArrowRight size={13} />
          </button>
        </div>

        {recentGames.length === 0 ? (
          <p className="text-xs text-slate-400 italic py-3 text-center">
            No missions recorded in database yet. Launch a new game to generate live data!
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-bold uppercase tracking-wider">
                  <th className="py-2.5 px-3">Mission ID</th>
                  <th className="py-2.5 px-3">Difficulty</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Duration</th>
                  <th className="py-2.5 px-3 text-right">Accuracy</th>
                  <th className="py-2.5 px-3 text-right">Score</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {recentGames.map((g) => (
                  <tr key={g.gameId} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-2.5 px-3 font-mono text-slate-300">#{g.gameId}</td>
                    <td className="py-2.5 px-3 font-semibold text-slate-200">{g.difficulty}</td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                          g.completionStatus === 'COMPLETED'
                            ? 'bg-emerald-950 text-emerald-300 border border-emerald-500/30'
                            : 'bg-slate-800 text-slate-300'
                        }`}
                      >
                        {g.completionStatus}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono text-purple-300">
                      {formatSeconds(g.duration)}
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono text-cyan-300">
                      {Math.round(g.accuracy * 100)}%
                    </td>
                    <td className="py-2.5 px-3 text-right font-mono text-amber-300 font-bold">
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
