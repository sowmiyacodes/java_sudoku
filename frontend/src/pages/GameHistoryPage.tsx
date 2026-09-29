import React, { useEffect, useState } from 'react';
import {
  Search,
  Filter,
  Eye,
  Award,
  HelpCircle,
  X,
  Loader2
} from 'lucide-react';
import { PlayerApi } from '../services/playerApi';
import type { GameHistoryItem, GameHistoryDetail } from '../types/sudoku';

export const GameHistoryPage: React.FC = () => {
  const [games, setGames] = useState<GameHistoryItem[]>([]);
  const [selectedDifficulty, setSelectedDifficulty] = useState<string>('ALL');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [selectedGame, setSelectedGame] = useState<GameHistoryDetail | null>(null);

  const fetchGames = async () => {
    setIsLoading(true);
    try {
      const data = await PlayerApi.getMyGames({
        difficulty: selectedDifficulty !== 'ALL' ? selectedDifficulty : undefined,
        status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
        search: searchQuery.trim() || undefined
      });
      setGames(data);
    } catch (err) {
      console.error('Error fetching game history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchGames();
  }, [selectedDifficulty, selectedStatus]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchGames();
  };

  const handleViewDetail = async (gameId: number) => {
    try {
      const detail = await PlayerApi.getMyGameDetail(gameId);
      setSelectedGame(detail);
    } catch (err) {
      console.error('Error fetching game detail:', err);
    }
  };

  const formatSeconds = (sec: number) => {
    if (!sec || sec <= 0) return '0:00';
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  return (
    <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 space-y-6">
      {/* Header */}
      <div className="border-b border-slate-800 pb-5">
        <h1 className="text-2xl font-black text-white">Mission History Logs</h1>
        <p className="text-xs text-slate-400 mt-1 font-mono">
          Persistent gameplay telemetry records stored in H2 database
        </p>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-slate-900/60 p-4 rounded-2xl border border-slate-800">
        <form onSubmit={handleSearch} className="flex items-center gap-2 w-full sm:w-72">
          <div className="relative w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by ID or difficulty..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-950/80 border border-slate-700/60 rounded-xl pl-9 pr-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-cyan-500/80 transition-colors"
            />
          </div>
          <button
            type="submit"
            className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-xs font-semibold rounded-xl text-slate-200 transition-colors cursor-pointer"
          >
            Filter
          </button>
        </form>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          {/* Difficulty Filter */}
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Filter size={13} />
            <select
              value={selectedDifficulty}
              onChange={(e) => setSelectedDifficulty(e.target.value)}
              className="bg-slate-950 border border-slate-700/60 rounded-xl px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-cyan-500/80"
            >
              <option value="ALL">All Difficulties</option>
              <option value="Easy">Easy</option>
              <option value="Medium">Medium</option>
              <option value="Hard">Hard</option>
            </select>
          </div>

          {/* Status Filter */}
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="bg-slate-950 border border-slate-700/60 rounded-xl px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-cyan-500/80"
            >
              <option value="ALL">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="PAUSED">Paused</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Table */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 backdrop-blur-md overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center p-12 text-cyan-300">
            <Loader2 className="w-8 h-8 animate-spin" />
          </div>
        ) : games.length === 0 ? (
          <div className="p-12 text-center text-slate-400 text-xs italic">
            No games found matching the selected filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-950/60 text-slate-400 font-bold uppercase tracking-wider">
                  <th className="py-3 px-4">Game ID</th>
                  <th className="py-3 px-4">Puzzle</th>
                  <th className="py-3 px-4">Difficulty</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Duration</th>
                  <th className="py-3 px-4 text-right">Moves</th>
                  <th className="py-3 px-4 text-right">Mistakes</th>
                  <th className="py-3 px-4 text-right">Hints</th>
                  <th className="py-3 px-4 text-right">Accuracy</th>
                  <th className="py-3 px-4 text-right">Score</th>
                  <th className="py-3 px-4 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {games.map((g) => (
                  <tr key={g.gameId} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-slate-300">#{g.gameId}</td>
                    <td className="py-3 px-4 font-mono text-slate-400 text-[11px] truncate max-w-28">
                      {g.puzzleId}
                    </td>
                    <td className="py-3 px-4 font-bold text-slate-200">
                      <span
                        className={`inline-block w-2 h-2 rounded-full mr-1.5 ${
                          g.difficulty?.toLowerCase() === 'hard'
                            ? 'bg-rose-400'
                            : g.difficulty?.toLowerCase() === 'medium'
                            ? 'bg-amber-400'
                            : 'bg-emerald-400'
                        }`}
                      ></span>
                      {g.difficulty}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                          g.completionStatus === 'COMPLETED'
                            ? 'bg-emerald-950 text-emerald-300 border border-emerald-500/40'
                            : 'bg-slate-800 text-slate-300'
                        }`}
                      >
                        {g.completionStatus}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-purple-300">
                      {formatSeconds(g.duration)}
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-slate-300">{g.moves}</td>
                    <td className="py-3 px-4 text-right font-mono text-rose-300">{g.mistakes}</td>
                    <td className="py-3 px-4 text-right font-mono text-amber-300">{g.hints}</td>
                    <td className="py-3 px-4 text-right font-mono text-cyan-300 font-bold">
                      {Math.round(g.accuracy * 100)}%
                    </td>
                    <td className="py-3 px-4 text-right font-mono text-amber-300 font-black">
                      {g.score}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <button
                        onClick={() => handleViewDetail(g.gameId)}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-cyan-950 hover:text-cyan-300 text-slate-300 transition-colors cursor-pointer"
                        title="View detailed telemetry"
                      >
                        <Eye size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Game Detail Modal */}
      {selectedGame && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-2xl max-h-[85vh] overflow-y-auto p-6 shadow-2xl relative">
            <button
              onClick={() => setSelectedGame(null)}
              className="absolute right-4 top-4 p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              <X size={18} />
            </button>

            <div className="flex items-center gap-3 border-b border-slate-800 pb-4 mb-4">
              <Award className="w-6 h-6 text-cyan-400" />
              <div>
                <h2 className="text-lg font-black text-white">
                  Mission #{selectedGame.gameId} Details
                </h2>
                <p className="text-xs text-slate-400 font-mono">
                  {selectedGame.difficulty} • {selectedGame.completionStatus}
                </p>
              </div>
            </div>

            {/* Metrics cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase">Duration</span>
                <p className="text-lg font-mono font-bold text-purple-300">
                  {formatSeconds(selectedGame.duration)}
                </p>
              </div>
              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase">Accuracy</span>
                <p className="text-lg font-mono font-bold text-cyan-300">
                  {Math.round(selectedGame.accuracy * 100)}%
                </p>
              </div>
              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase">Mistakes</span>
                <p className="text-lg font-mono font-bold text-rose-300">{selectedGame.mistakes}</p>
              </div>
              <div className="p-3 rounded-xl bg-slate-950/60 border border-slate-800">
                <span className="text-[10px] text-slate-400 font-bold uppercase">Points</span>
                <p className="text-lg font-mono font-bold text-amber-300">{selectedGame.score}</p>
              </div>
            </div>

            {/* Hint history */}
            <div className="mb-6">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-300 mb-2 flex items-center gap-1.5">
                <HelpCircle size={14} className="text-amber-400" />
                <span>Hints Invoked ({selectedGame.hintHistory?.length || 0})</span>
              </h4>
              {selectedGame.hintHistory && selectedGame.hintHistory.length > 0 ? (
                <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
                  {selectedGame.hintHistory.map((h, i) => (
                    <div
                      key={i}
                      className="p-2.5 rounded-lg bg-slate-950/60 border border-slate-800 text-xs text-slate-300"
                    >
                      <div className="flex justify-between font-mono text-[11px] text-cyan-400 mb-0.5">
                        <span>Technique: {h.technique || 'Logical deduction'}</span>
                        <span>Cell ({h.row + 1}, {h.column + 1}) → Val: {h.value}</span>
                      </div>
                      <p className="text-slate-400 text-[11px]">{h.explanation}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-500 italic">No hints used in this mission.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
