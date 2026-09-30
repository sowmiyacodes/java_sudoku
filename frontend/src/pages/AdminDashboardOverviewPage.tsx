import React, { useEffect, useState } from 'react';
import {
  Users,
  Gamepad2,
  CheckCircle2,
  PlayCircle,
  Puzzle,
  Swords,
  Target,
  Clock,
  Brain,
  Activity,
  RefreshCw,
  TrendingUp,
  AlertCircle,
  ChevronRight
} from 'lucide-react';
import { AdminApi, type AdminAnalyticsOverview } from '../services/adminApi';

interface AdminDashboardOverviewPageProps {
  onNavigate: (view: string) => void;
}

export const AdminDashboardOverviewPage: React.FC<AdminDashboardOverviewPageProps> = ({ onNavigate }) => {
  const [stats, setStats] = useState<AdminAnalyticsOverview | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchOverview = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await AdminApi.getAnalyticsOverview();
      setStats(data);
    } catch (err: any) {
      setError(err.message || 'Unable to connect to Admin Analytics Service');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOverview();
  }, []);

  const cards = [
    { key: 'totalPlayers', label: 'Total Players', value: stats?.totalPlayers ?? 0, icon: Users, color: 'text-blue-600', bg: 'bg-blue-50 border-blue-200', targetView: 'PLAYERS' },
    { key: 'totalGames', label: 'Total Games', value: stats?.totalGames ?? 0, icon: Gamepad2, color: 'text-indigo-600', bg: 'bg-indigo-50 border-indigo-200', targetView: 'GAMES' },
    { key: 'completedGames', label: 'Completed Games', value: stats?.completedGames ?? 0, icon: CheckCircle2, color: 'text-emerald-600', bg: 'bg-emerald-50 border-emerald-200', targetView: 'GAMES' },
    { key: 'activeGames', label: 'Active Games', value: stats?.activeGames ?? 0, icon: PlayCircle, color: 'text-amber-600', bg: 'bg-amber-50 border-amber-200', targetView: 'GAMES' },
    { key: 'totalPuzzles', label: 'Total Puzzles', value: stats?.totalPuzzles ?? 0, icon: Puzzle, color: 'text-purple-600', bg: 'bg-purple-50 border-purple-200', targetView: 'PUZZLES' },
    { key: 'multiplayerMatches', label: 'Multiplayer Matches', value: stats?.multiplayerMatches ?? 0, icon: Swords, color: 'text-rose-600', bg: 'bg-rose-50 border-rose-200', targetView: 'MULTIPLAYER' },
    { key: 'averageAccuracy', label: 'Average Accuracy', value: `${stats?.averageAccuracy ?? 0}%`, icon: Target, color: 'text-teal-600', bg: 'bg-teal-50 border-teal-200', targetView: 'ANALYTICS' },
    { key: 'averageSolvingTime', label: 'Avg Solving Time', value: `${stats?.averageSolvingTime ?? 0}s`, icon: Clock, color: 'text-cyan-600', bg: 'bg-cyan-50 border-cyan-200', targetView: 'ANALYTICS' },
    { key: 'mlModels', label: 'ML Active Models', value: stats?.mlModels ?? 5, icon: Brain, color: 'text-blue-700', bg: 'bg-blue-100/60 border-blue-300', targetView: 'ADMIN_ML' },
    { key: 'mlPredictions', label: 'ML Predictions', value: stats?.mlPredictions ?? 0, icon: Activity, color: 'text-emerald-700', bg: 'bg-emerald-100/60 border-emerald-300', targetView: 'ADMIN_ML' },
  ];

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wider rounded bg-blue-100 text-blue-800 border border-blue-200">
              Admin Platform
            </span>
            <span className="flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Telemetry
            </span>
          </div>
          <h1 className="text-2xl font-black text-slate-900 mt-1">
            System Executive Dashboard
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Real-time administrative metrics, game telemetry, player activity, and machine learning infrastructure status
          </p>
        </div>

        <button
          onClick={fetchOverview}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh Stats
        </button>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
          <AlertCircle size={16} className="text-rose-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* 10 Key Counters Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
            <div
              key={card.key}
              onClick={() => onNavigate(card.targetView)}
              className={`p-4 rounded-xl border ${card.bg} transition-all hover:shadow-md cursor-pointer flex flex-col justify-between`}
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-600">{card.label}</span>
                <Icon size={18} className={card.color} />
              </div>
              <div className="mt-3 flex items-baseline justify-between">
                <span className="text-2xl font-black text-slate-900 tracking-tight">{card.value}</span>
                <ChevronRight size={14} className="text-slate-400" />
              </div>
            </div>
          );
        })}
      </div>

      {/* Quick Navigation Action Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-2">
        <div
          onClick={() => onNavigate('PLAYERS')}
          className="p-5 rounded-xl bg-white border border-slate-200 shadow-sm hover:border-blue-300 transition cursor-pointer space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="w-10 h-10 rounded-lg bg-blue-50 border border-blue-200 flex items-center justify-center text-blue-600">
              <Users size={20} />
            </div>
            <span className="text-xs font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
              Management
            </span>
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Player Directory</h3>
            <p className="text-xs text-slate-500 mt-1">Search, filter, inspect accuracy, and manage player account statuses.</p>
          </div>
        </div>

        <div
          onClick={() => onNavigate('ADMIN_ML')}
          className="p-5 rounded-xl bg-white border border-slate-200 shadow-sm hover:border-blue-300 transition cursor-pointer space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="w-10 h-10 rounded-lg bg-indigo-50 border border-indigo-200 flex items-center justify-center text-indigo-600">
              <Brain size={20} />
            </div>
            <span className="text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-200">
              5 Models Active
            </span>
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">ML Intelligence Suite</h3>
            <p className="text-xs text-slate-500 mt-1">Inspect datasets, test predictions, compare metrics, and trigger retraining.</p>
          </div>
        </div>

        <div
          onClick={() => onNavigate('ANALYTICS')}
          className="p-5 rounded-xl bg-white border border-slate-200 shadow-sm hover:border-blue-300 transition cursor-pointer space-y-3"
        >
          <div className="flex items-center justify-between">
            <div className="w-10 h-10 rounded-lg bg-teal-50 border border-teal-200 flex items-center justify-center text-teal-600">
              <TrendingUp size={20} />
            </div>
            <span className="text-xs font-bold text-teal-600 bg-teal-50 px-2 py-0.5 rounded border border-teal-200">
              Analytics
            </span>
          </div>
          <div>
            <h3 className="text-sm font-bold text-slate-900">Telemetry & Analytics</h3>
            <p className="text-xs text-slate-500 mt-1">Examine gameplay mistake rates, difficulty distribution, and multiplayer wins.</p>
          </div>
        </div>
      </div>
    </div>
  );
};
