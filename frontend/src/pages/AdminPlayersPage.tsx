import React, { useEffect, useState } from 'react';
import { Users, Search, Filter, ShieldCheck, ShieldAlert, RefreshCw, Loader2, CheckCircle2 } from 'lucide-react';
import { AdminApi, type AdminPlayerRecord } from '../services/adminApi';

export const AdminPlayersPage: React.FC = () => {
  const [players, setPlayers] = useState<AdminPlayerRecord[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'DISABLED'>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

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
                          <p className="font-bold text-slate-900">{player.displayName}</p>
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
                      <button
                        onClick={() => handleToggleStatus(player)}
                        className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                          player.active
                            ? 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200'
                            : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                        }`}
                      >
                        {player.active ? 'Disable' : 'Enable'}
                      </button>
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
