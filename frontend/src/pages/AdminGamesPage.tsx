import React, { useEffect, useState } from 'react';
import { Gamepad2, Search, Filter, RefreshCw, Loader2, PlayCircle, CheckCircle2, XCircle } from 'lucide-react';
import { AdminApi, type AdminGameRecord } from '../services/adminApi';

export const AdminGamesPage: React.FC = () => {
  const [games, setGames] = useState<AdminGameRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [difficultyFilter, setDifficultyFilter] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [selectedGame, setSelectedGame] = useState<AdminGameRecord | null>(null);

  const fetchGames = async () => {
    setIsLoading(true);
    try {
      const data = await AdminApi.getGames();
      setGames(data);
    } catch (err: any) {
      console.error('Error fetching games:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchGames();
  }, []);

  const filteredGames = games.filter((g) => {
    const matchesSearch =
      g.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      String(g.id).includes(searchTerm) ||
      (g.puzzleId && g.puzzleId.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesDiff = difficultyFilter === 'ALL' || g.difficulty === difficultyFilter;
    return matchesSearch && matchesDiff;
  });

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            <Gamepad2 className="text-blue-600" />
            Game Operations Management
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Inspect all active and historical gameplay sessions, puzzle pairings, solve durations, mistakes, and scores.
          </p>
        </div>

        <button
          onClick={fetchGames}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh Sessions
        </button>
      </div>

      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search by Game ID, player, or puzzle ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter size={16} className="text-slate-400" />
          <span className="text-xs font-bold text-slate-600">Difficulty:</span>
          {['ALL', 'EASY', 'MEDIUM', 'HARD', 'EXPERT'].map((d) => (
            <button
              key={d}
              onClick={() => setDifficultyFilter(d)}
              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                difficultyFilter === d
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {d}
            </button>
          ))}
        </div>
      </div>

      {/* Games Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 flex flex-col items-center justify-center text-slate-500">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-2" />
            <p className="text-xs font-medium">Loading Game Operations Data...</p>
          </div>
        ) : filteredGames.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No game sessions recorded matching your search.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold uppercase tracking-wider">
                  <th className="py-3.5 px-4">Game ID</th>
                  <th className="py-3.5 px-4">Player</th>
                  <th className="py-3.5 px-4">Puzzle ID</th>
                  <th className="py-3.5 px-4 text-center">Difficulty</th>
                  <th className="py-3.5 px-4 text-center">Duration</th>
                  <th className="py-3.5 px-4 text-center">Mistakes</th>
                  <th className="py-3.5 px-4 text-center">Hints</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {filteredGames.map((game) => (
                  <tr key={game.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4 font-mono font-bold text-blue-700">#{game.id}</td>
                    <td className="py-3 px-4 font-bold text-slate-900">{game.username}</td>
                    <td className="py-3 px-4 font-mono text-slate-500 truncate max-w-32">{game.puzzleId || 'Standard Grid'}</td>
                    <td className="py-3 px-4 text-center">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
                        {game.difficulty}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center font-mono">{game.durationSeconds}s</td>
                    <td className="py-3 px-4 text-center font-bold text-rose-600">{game.mistakes}</td>
                    <td className="py-3 px-4 text-center font-bold text-amber-600">{game.hints}</td>
                    <td className="py-3 px-4 text-center">
                      {game.status === 'COMPLETED' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <CheckCircle2 size={12} /> Completed
                        </span>
                      ) : game.status === 'IN_PROGRESS' ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                          <PlayCircle size={12} /> Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-600 border border-slate-200">
                          <XCircle size={12} /> Abandoned
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => setSelectedGame(game)}
                        className="px-3 py-1 rounded bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200 font-bold transition cursor-pointer"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Game Details Modal */}
      {selectedGame && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 p-6 max-w-lg w-full shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-base font-black text-slate-900">Game Session Inspection #{selectedGame.id}</h3>
              <button onClick={() => setSelectedGame(null)} className="text-slate-400 hover:text-slate-700 font-bold">✕</button>
            </div>
            <div className="grid grid-cols-2 gap-3 text-xs">
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-400 font-semibold">Player:</span>
                <p className="font-bold text-slate-900 mt-0.5">{selectedGame.username}</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-400 font-semibold">Difficulty:</span>
                <p className="font-bold text-slate-900 mt-0.5">{selectedGame.difficulty}</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-400 font-semibold">Elapsed Time:</span>
                <p className="font-bold text-slate-900 mt-0.5">{selectedGame.durationSeconds} seconds</p>
              </div>
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-400 font-semibold">Mistakes & Hints:</span>
                <p className="font-bold text-slate-900 mt-0.5">{selectedGame.mistakes} mistakes, {selectedGame.hints} hints</p>
              </div>
            </div>
            <div className="pt-2 text-right">
              <button
                onClick={() => setSelectedGame(null)}
                className="px-4 py-2 bg-slate-900 text-white rounded-xl text-xs font-bold hover:bg-slate-800 transition cursor-pointer"
              >
                Close Inspection
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
