import React, { useEffect, useState } from 'react';
import { Users, Search, Filter, ShieldCheck, ShieldAlert, RefreshCw, Loader2, CheckCircle2, X, Brain, Clock3, Target, Trophy, Eye } from 'lucide-react';
import { AdminApi, type AdminPlayerDetails, type AdminPlayerRecord } from '../services/adminApi';

export const AdminPlayersPage: React.FC = () => {
  const [players, setPlayers] = useState<AdminPlayerRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DISABLED'>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [selectedPlayer, setSelectedPlayer] = useState<AdminPlayerRecord | null>(null);
  const [playerDetails, setPlayerDetails] = useState<AdminPlayerDetails | null>(null);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);

  const openPlayerDetails = async (player: AdminPlayerRecord) => {
    setSelectedPlayer(player);
    setPlayerDetails(null);
    setDetailsError(null);
    setIsLoadingDetails(true);
    try {
      setPlayerDetails(await AdminApi.getPlayerDetails(player.id));
    } catch (err) {
      setDetailsError(err instanceof Error ? err.message : 'Could not load player details.');
    } finally {
      setIsLoadingDetails(false);
    }
  };

  const formatSeconds = (seconds: number) => {
    if (!seconds || seconds <= 0) return '0:00';
    const minutes = Math.floor(seconds / 60);
    return `${minutes}:${String(seconds % 60).padStart(2, '0')}`;
  };

  const fetchPlayers = async () => {
    setIsLoading(true);
    try {
      const data = await AdminApi.getPlayers();
      setPlayers(data);
    } catch (err: any) {
      console.error('Error fetching players:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchPlayers();
  }, []);

  const handleToggleStatus = async (player: AdminPlayerRecord) => {
    const newStatus = !player.active;
    try {
      await AdminApi.updatePlayerStatus(player.id, newStatus);
      setActionMessage(`Updated ${player.username}'s account status to ${newStatus ? 'Active' : 'Disabled'}.`);
      fetchPlayers();
    } catch (err: any) {
      setActionMessage(`Failed to update status: ${err.message}`);
    }
  };

  const filteredPlayers = players.filter((p) => {
    const matchesSearch =
      p.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.displayName.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && p.active) ||
      (statusFilter === 'DISABLED' && !p.active);

    return matchesSearch && matchesStatus;
  });

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            <Users className="text-blue-600" />
            Player Management Console
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Inspect registered players, gameplay performance, estimated skill levels, and manage account statuses.
          </p>
        </div>

        <button
          onClick={fetchPlayers}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh List
        </button>
      </div>

      {actionMessage && (
        <div className="p-3.5 rounded-xl bg-blue-50 border border-blue-200 text-blue-900 text-xs flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2">
            <CheckCircle2 size={16} className="text-blue-600 shrink-0" />
            <span>{actionMessage}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-blue-500 hover:text-blue-800 font-bold ml-4">
            ✕
          </button>
        </div>
      )}

      {/* Search & Filters */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="Search players by name or email..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <Filter size={16} className="text-slate-400" />
          <span className="text-xs font-bold text-slate-600">Status:</span>
          {(['ALL', 'ACTIVE', 'DISABLED'] as const).map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                statusFilter === st
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Player Directory Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 flex flex-col items-center justify-center text-slate-500">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-2" />
            <p className="text-xs font-medium">Loading Player Directory...</p>
          </div>
        ) : filteredPlayers.length === 0 ? (
          <div className="p-12 text-center text-slate-500 text-xs">
            No player records found matching your filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold uppercase tracking-wider">
                  <th className="py-3.5 px-4">Player</th>
                  <th className="py-3.5 px-4">Email</th>
                  <th className="py-3.5 px-4 text-center">Games</th>
                  <th className="py-3.5 px-4 text-center">Win Rate</th>
                  <th className="py-3.5 px-4 text-center">Accuracy</th>
                  <th className="py-3.5 px-4 text-center">Skill Tier</th>
                  <th className="py-3.5 px-4 text-center">Status</th>
                  <th className="py-3.5 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
                {filteredPlayers.map((player) => (
                  <tr key={player.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xs border border-blue-200 shrink-0">
                          {player.displayName.charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <button
                            type="button"
                            onClick={() => openPlayerDetails(player)}
                            className="text-left font-bold text-slate-900 hover:text-blue-700 hover:underline"
                          >
                            {player.displayName}
                          </button>
                          <p className="text-[11px] text-slate-400 font-mono">@{player.username}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-600">{player.email}</td>
                    <td className="py-3 px-4 text-center font-bold text-slate-900">{player.gamesCount}</td>
                    <td className="py-3 px-4 text-center font-bold text-emerald-700">{player.winRate}%</td>
                    <td className="py-3 px-4 text-center font-bold text-blue-700">{player.accuracy}%</td>
                    <td className="py-3 px-4 text-center">
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                        {player.skill}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      {player.active ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                          <ShieldCheck size={12} /> Active
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
                          <ShieldAlert size={12} /> Disabled
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex flex-col items-end gap-1.5">
                        <button
                          type="button"
                          onClick={() => openPlayerDetails(player)}
                          className="inline-flex items-center gap-1.5 rounded-md border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-800 transition hover:bg-blue-100"
                        >
                          <Eye size={13} /> View stats
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleStatus(player)}
                          className={`rounded-md border px-2.5 py-1 text-xs font-bold transition cursor-pointer ${
                            player.active
                              ? 'border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100'
                              : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                          }`}
                        >
                          {player.active ? 'Disable' : 'Enable'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedPlayer && (
        <div
          className="fixed inset-0 z-[80] grid place-items-center overflow-y-auto bg-slate-950/60 p-3 backdrop-blur-sm sm:p-6"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) setSelectedPlayer(null);
          }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="player-details-title"
            className="flex max-h-[92vh] w-full max-w-5xl flex-col overflow-hidden rounded-xl border border-slate-200 bg-slate-50 shadow-2xl"
          >
            <header className="flex items-center justify-between gap-4 border-b border-slate-200 bg-white px-5 py-4 sm:px-6">
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-lg bg-blue-100 font-bold text-blue-800">
                  {selectedPlayer.displayName.charAt(0).toUpperCase()}
                </div>
                <div className="min-w-0">
                  <h2 id="player-details-title" className="truncate text-lg font-black text-slate-950">{selectedPlayer.displayName}</h2>
                  <p className="truncate font-mono text-xs text-slate-500">@{selectedPlayer.username} · {selectedPlayer.email}</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedPlayer(null)}
                aria-label="Close player details"
                className="rounded-md p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
              >
                <X size={18} />
              </button>
            </header>

            <div className="overflow-y-auto p-4 sm:p-6">
              {isLoadingDetails ? (
                <div className="flex min-h-64 flex-col items-center justify-center gap-3 text-slate-500">
                  <Loader2 className="size-8 animate-spin text-blue-600" />
                  <p className="text-sm font-medium">Loading player statistics and ML insights...</p>
                </div>
              ) : detailsError ? (
                <div role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{detailsError}</div>
              ) : playerDetails && (
                <div className="space-y-5">
                  <section>
                    <h3 className="mb-3 text-sm font-bold text-slate-900">Performance overview</h3>
                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
                      {[
                        { label: 'Games', value: playerDetails.profile.gamesPlayed, icon: Users, color: 'text-blue-700' },
                        { label: 'Completed', value: playerDetails.profile.gamesCompleted, icon: Trophy, color: 'text-emerald-700' },
                        { label: 'Completion', value: `${Math.round(playerDetails.profile.completionRate * 100)}%`, icon: CheckCircle2, color: 'text-cyan-800' },
                        { label: 'Accuracy', value: `${Math.round(playerDetails.profile.averageAccuracy * 100)}%`, icon: Target, color: 'text-indigo-700' },
                        { label: 'Average time', value: formatSeconds(playerDetails.profile.averageSolvingTime), icon: Clock3, color: 'text-violet-700' },
                        { label: 'Current streak', value: playerDetails.profile.currentStreak, icon: Trophy, color: 'text-amber-700' },
                      ].map((metric) => {
                        const Icon = metric.icon;
                        return (
                          <div key={metric.label} className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm">
                            <div className="flex items-center justify-between gap-2 text-[11px] font-semibold text-slate-500">
                              <span>{metric.label}</span><Icon size={15} className={metric.color} />
                            </div>
                            <p className="mt-2 text-xl font-black text-slate-950">{metric.value}</p>
                          </div>
                        );
                      })}
                    </div>
                  </section>

                  <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
                    <div className="rounded-lg border border-violet-200 bg-white p-5 shadow-sm">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="flex items-center gap-2">
                          <Brain size={18} className="text-violet-700" />
                          <div>
                            <h3 className="text-sm font-bold text-slate-950">Machine-learning profile</h3>
                            <p className="text-xs text-slate-500">Skill classification and personalized difficulty</p>
                          </div>
                        </div>
                        <span className={`rounded-full border px-2.5 py-1 text-[10px] font-bold ${playerDetails.recommendation.mlServiceAvailable ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-amber-200 bg-amber-50 text-amber-800'}`}>
                          {playerDetails.recommendation.mlServiceAvailable ? 'ML service active' : 'Heuristic fallback'}
                        </span>
                      </div>
                      <div className="mt-4 grid grid-cols-2 gap-3">
                        <div className="rounded-md bg-violet-50 p-3">
                          <p className="text-[10px] font-bold uppercase text-violet-700">Predicted skill</p>
                          <p className="mt-1 text-lg font-black text-slate-950">{playerDetails.recommendation.skillLevel}</p>
                        </div>
                        <div className="rounded-md bg-cyan-50 p-3">
                          <p className="text-[10px] font-bold uppercase text-cyan-800">Recommended tier</p>
                          <p className="mt-1 text-lg font-black text-slate-950">{playerDetails.recommendation.recommendedDifficulty}</p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3">
                          <p className="text-[10px] font-bold uppercase text-slate-500">Model confidence</p>
                          <p className="mt-1 text-lg font-black text-slate-950">{Math.round(playerDetails.recommendation.confidence * 100)}%</p>
                        </div>
                        <div className="rounded-md bg-slate-50 p-3">
                          <p className="text-[10px] font-bold uppercase text-slate-500">Model version</p>
                          <p className="mt-1 truncate font-mono text-sm font-bold text-slate-900">{playerDetails.recommendation.modelVersion}</p>
                        </div>
                      </div>
                      <p className="mt-3 rounded-md border border-slate-200 bg-slate-50 p-3 text-sm leading-relaxed text-slate-700">
                        {playerDetails.recommendation.reason}
                      </p>
                      <div className="mt-3">
                        <p className="mb-2 text-[10px] font-bold uppercase text-slate-500">Top model factors</p>
                        {playerDetails.recommendation.topFactors.length > 0 ? (
                          <ul className="flex flex-wrap gap-2">
                            {playerDetails.recommendation.topFactors.map((factor) => (
                              <li key={factor} className="rounded-md border border-violet-100 bg-violet-50 px-2.5 py-1.5 text-xs text-violet-900">{factor}</li>
                            ))}
                          </ul>
                        ) : <p className="text-xs text-slate-500">No model factors are available yet.</p>}
                      </div>
                    </div>

                    <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                      <h3 className="text-sm font-bold text-slate-950">Gameplay signals</h3>
                      <dl className="mt-3 divide-y divide-slate-100 text-sm">
                        {[
                          ['Best solve time', formatSeconds(playerDetails.profile.bestSolvingTime)],
                          ['Best completion streak', playerDetails.profile.bestStreak],
                          ['Total mistakes', playerDetails.profile.totalMistakes],
                          ['Avg. mistakes per game', playerDetails.profile.averageMistakesPerGame.toFixed(1)],
                          ['Avg. hints per game', playerDetails.profile.averageHintsPerGame.toFixed(1)],
                          ['Avg. score', playerDetails.profile.averageScore],
                        ].map(([label, value]) => (
                          <div key={label} className="flex items-center justify-between gap-3 py-2.5">
                            <dt className="text-slate-500">{label}</dt><dd className="font-mono font-bold text-slate-900">{value}</dd>
                          </div>
                        ))}
                      </dl>
                    </div>
                  </section>

                  <section className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
                    <h3 className="mb-3 text-sm font-bold text-slate-950">Performance by difficulty</h3>
                    <div className="overflow-x-auto">
                      <table className="w-full min-w-[560px] text-left text-xs">
                        <thead><tr className="border-b border-slate-200 text-[10px] uppercase text-slate-500">
                          <th className="px-3 py-2">Difficulty</th><th className="px-3 py-2 text-right">Games</th><th className="px-3 py-2 text-right">Completed</th><th className="px-3 py-2 text-right">Completion rate</th><th className="px-3 py-2 text-right">Avg time</th><th className="px-3 py-2 text-right">Accuracy</th>
                        </tr></thead>
                        <tbody className="divide-y divide-slate-100">
                          {playerDetails.difficultyPerformance.map((row) => (
                            <tr key={row.difficulty}>
                              <td className="px-3 py-3 font-bold text-slate-800">{row.difficulty}</td>
                              <td className="px-3 py-3 text-right font-mono">{row.games}</td>
                              <td className="px-3 py-3 text-right font-mono">{row.completed}</td>
                              <td className="px-3 py-3 text-right font-mono">{Math.round(row.completionRate * 100)}%</td>
                              <td className="px-3 py-3 text-right font-mono">{formatSeconds(row.averageTime)}</td>
                              <td className="px-3 py-3 text-right font-mono">{Math.round(row.accuracy * 100)}%</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </section>
                </div>
              )}
            </div>
          </section>
        </div>
      )}
    </div>
  );
};
