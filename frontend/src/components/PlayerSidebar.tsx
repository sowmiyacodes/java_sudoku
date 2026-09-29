import React from 'react';
import {
  LayoutDashboard,
  User,
  History,
  BarChart3,
  Trophy,
  Sparkles,
  PlayCircle,
  LogOut,
  ChevronRight,
  Brain,
  Puzzle
} from 'lucide-react';
import { useAuth } from '../context/useAuth';

export type DashboardView =
  | 'DASHBOARD'
  | 'PROFILE'
  | 'GAMES'
  | 'STATISTICS'
  | 'RECOMMENDATIONS'
  | 'ADMIN_ML'
  | 'PUZZLES'
  | 'LEADERBOARD';

interface PlayerSidebarProps {
  currentView: DashboardView;
  onNavigate: (view: DashboardView) => void;
  onPlaySudoku: () => void;
  onLogout: () => void;
}

export const PlayerSidebar: React.FC<PlayerSidebarProps> = ({
  currentView,
  onNavigate,
  onPlaySudoku,
  onLogout
}) => {
  const { user } = useAuth();

  const navItems = [
    { id: 'DASHBOARD' as DashboardView, label: 'Analytics Dashboard', icon: LayoutDashboard },
    { id: 'ADMIN_ML' as DashboardView, label: 'Admin ML Console', icon: Brain },
    { id: 'PUZZLES' as DashboardView, label: 'Puzzle Library', icon: Puzzle },
    { id: 'PROFILE' as DashboardView, label: 'Pilot Profile', icon: User },
    { id: 'GAMES' as DashboardView, label: 'Game History', icon: History },
    { id: 'STATISTICS' as DashboardView, label: 'Telemetry & Stats', icon: BarChart3 },
    { id: 'RECOMMENDATIONS' as DashboardView, label: 'AI Intelligence', icon: Sparkles },
    { id: 'LEADERBOARD' as DashboardView, label: 'Leaderboard', icon: Trophy }
  ];

  return (
    <aside className="w-full md:w-64 bg-slate-900/80 border-r border-slate-800/80 backdrop-blur-md flex flex-col justify-between shrink-0 p-4">
      <div>
        {/* User Identity Header */}
        <div className="flex items-center gap-3 p-3 mb-6 rounded-2xl bg-gradient-to-r from-cyan-950/60 to-blue-950/60 border border-cyan-500/20">
          <div className="w-10 h-10 rounded-xl bg-cyan-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-300 font-black text-lg">
            {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'P'}
          </div>
          <div className="overflow-hidden">
            <h3 className="text-sm font-bold text-slate-100 truncate">{user?.displayName || 'Player'}</h3>
            <p className="text-xs text-cyan-400/80 font-mono truncate">@{user?.username || 'pilot'}</p>
          </div>
        </div>

        {/* Primary Navigation */}
        <nav className="space-y-1.5" aria-label="Player navigation">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentView === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-semibold tracking-wide transition-all ${
                  isActive
                    ? 'bg-gradient-to-r from-cyan-500/20 to-blue-600/20 text-cyan-200 border border-cyan-500/40 shadow-sm'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon size={16} className={isActive ? 'text-cyan-400' : 'text-slate-400'} />
                  <span>{item.label}</span>
                </div>
                {isActive && <ChevronRight size={14} className="text-cyan-400" />}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Action Footer */}
      <div className="space-y-2 pt-4 border-t border-slate-800/80">
        <button
          onClick={onPlaySudoku}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold text-xs shadow-md transition-all cursor-pointer"
        >
          <PlayCircle size={16} />
          <span>Launch Mission</span>
        </button>

        <button
          onClick={onLogout}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl text-xs font-semibold text-rose-300/80 hover:text-rose-200 hover:bg-rose-950/40 border border-transparent hover:border-rose-500/30 transition-all cursor-pointer"
        >
          <LogOut size={14} />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
};
