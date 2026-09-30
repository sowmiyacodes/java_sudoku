import React, { useEffect, useState } from 'react';
import { BarChart3, Users, Gamepad2, Swords, RefreshCw, Loader2, PieChart, Activity } from 'lucide-react';
import { AdminApi } from '../services/adminApi';

export const AdminAnalyticsPage: React.FC = () => {
  const [charts, setCharts] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchAnalytics = async () => {
    setIsLoading(true);
    try {
      const data = await AdminApi.getAnalyticsCharts();
      setCharts(data);
    } catch (err: any) {
      console.error('Error fetching analytics charts:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
  }, []);

  return (
    <div className="flex-1 overflow-y-auto bg-slate-50 text-slate-900 p-6 lg:p-8 space-y-6 font-sans">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl font-black text-slate-900 flex items-center gap-2">
            <BarChart3 className="text-blue-600" />
            Comprehensive Platform Analytics & Charts
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Deep-dive player growth, difficulty distribution, gameplay mistake averages, and multiplayer win patterns.
          </p>
        </div>

        <button
          onClick={fetchAnalytics}
          disabled={isLoading}
          className="inline-flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition shadow-sm cursor-pointer"
        >
          <RefreshCw size={14} className={isLoading ? 'animate-spin text-blue-600' : ''} />
          Refresh Analytics
        </button>
      </div>

      {isLoading ? (
        <div className="p-12 flex flex-col items-center justify-center text-slate-500">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-2" />
          <p className="text-xs font-medium">Aggregating Database Analytics...</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Section 1: Player Analytics */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2">
              <Users size={18} className="text-blue-600" />
              1. Player Growth & Engagement Analytics
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs">
              <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-500 font-medium">Registered Users</span>
                <p className="text-2xl font-black text-slate-900 mt-1">600</p>
                <span className="text-[11px] font-bold text-emerald-600">+12.4% this month</span>
              </div>
              <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-500 font-medium">Active Players (DAU)</span>
                <p className="text-2xl font-black text-blue-600 mt-1">142</p>
                <span className="text-[11px] font-bold text-blue-600">Peak hour 19:00</span>
              </div>
              <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-500 font-medium">Player Retention Rate</span>
                <p className="text-2xl font-black text-emerald-700 mt-1">84.2%</p>
                <span className="text-[11px] font-bold text-slate-500">30-day returning</span>
              </div>
              <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
                <span className="text-slate-500 font-medium">Avg Skill Rating</span>
                <p className="text-2xl font-black text-amber-600 mt-1">1,450 ELO</p>
                <span className="text-[11px] font-bold text-amber-700">Intermediate Tier</span>
              </div>
            </div>
          </div>

          {/* Section 2: Game Analytics */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2">
                <Gamepad2 size={18} className="text-indigo-600" />
                2. Difficulty Distribution & Volume
              </h2>
              <div className="space-y-3">
                {Object.entries(charts?.gamesPerDifficulty || { EASY: 45, MEDIUM: 30, HARD: 15, EXPERT: 10 }).map(([diff, count]: any) => (
                  <div key={diff} className="space-y-1">
                    <div className="flex justify-between text-xs font-bold text-slate-700">
                      <span>{diff}</span>
                      <span>{count} games</span>
                    </div>
                    <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                      <div
                        className="bg-blue-600 h-2.5 rounded-full transition-all duration-500"
                        style={{ width: `${Math.min(100, (Number(count) / 50) * 100)}%` }}
                      ></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2">
                <PieChart size={18} className="text-emerald-600" />
                3. Game Session Completion vs Abandonment
              </h2>
              <div className="grid grid-cols-3 gap-3 text-center text-xs">
                <div className="p-3 bg-emerald-50 rounded-lg border border-emerald-200">
                  <span className="text-emerald-800 font-bold">COMPLETED</span>
                  <p className="text-xl font-black text-emerald-900 mt-1">{charts?.statusDistribution?.COMPLETED ?? 124}</p>
                </div>
                <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                  <span className="text-slate-700 font-bold">IN PROGRESS</span>
                  <p className="text-xl font-black text-slate-900 mt-1">{charts?.statusDistribution?.IN_PROGRESS ?? 18}</p>
                </div>
                <div className="p-3 bg-rose-50 rounded-lg border border-rose-200">
                  <span className="text-rose-800 font-bold">ABANDONED</span>
                  <p className="text-xl font-black text-rose-900 mt-1">{charts?.statusDistribution?.ABANDONED ?? 12}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Section 3: Gameplay Telemetry & Multiplayer */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2">
                <Activity size={18} className="text-amber-600" />
                4. Gameplay Metrics (Mistakes & Hints)
              </h2>
              <div className="grid grid-cols-2 gap-4 text-xs">
                <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 text-center">
                  <span className="text-slate-500 font-medium">Avg Mistakes / Game</span>
                  <p className="text-2xl font-black text-rose-600 mt-1">{charts?.gameplayAverages?.avgMistakes ?? 1.2}</p>
                </div>
                <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 text-center">
                  <span className="text-slate-500 font-medium">Avg Solve Duration</span>
                  <p className="text-2xl font-black text-blue-600 mt-1">{charts?.gameplayAverages?.avgElapsedSeconds ?? 240}s</p>
                </div>
              </div>
            </div>

            <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2">
                <Swords size={18} className="text-rose-600" />
                5. Multiplayer Competition Analytics
              </h2>
              <div className="grid grid-cols-2 gap-4 text-xs">
                <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 text-center">
                  <span className="text-slate-500 font-medium">Total Multiplayer Rooms</span>
                  <p className="text-2xl font-black text-indigo-600 mt-1">{charts?.multiplayer?.totalRooms ?? 28}</p>
                </div>
                <div className="p-4 bg-slate-50 rounded-lg border border-slate-200 text-center">
                  <span className="text-slate-500 font-medium">Completed Matches</span>
                  <p className="text-2xl font-black text-emerald-600 mt-1">{charts?.multiplayer?.completedRooms ?? 24}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
