import React, { useState } from 'react';
import {
  LayoutDashboard,
  Users,
  Puzzle,
  Gamepad2,
  HelpCircle,
  Swords,
  BarChart3,
  Brain,
  FileText,
  Settings,
  LogOut,
  ChevronRight,
  ChevronDown,
  PlayCircle,
  Database,
  Layers,
  History,
  Activity,
  Cpu,
  Sliders
} from 'lucide-react';
import { useAuth } from '../context/useAuth';

export type DashboardView =
  | 'DASHBOARD'
  | 'PLAYERS'
  | 'PUZZLES'
  | 'GAMES'
  | 'HINTS'
  | 'MULTIPLAYER'
  | 'ANALYTICS'
  | 'ADMIN_ML'
  | 'ML_OVERVIEW'
  | 'ML_DATASETS'
  | 'ML_MODELS'
  | 'ML_EXPERIMENTS'
  | 'ML_PREDICTIONS'
  | 'ML_TRAINING'
  | 'AUDIT_LOGS'
  | 'SETTINGS'
  | 'PROFILE'
  | 'STATISTICS'
  | 'RECOMMENDATIONS'
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
  const [isMlExpanded, setIsMlExpanded] = useState(
    ['ADMIN_ML', 'ML_OVERVIEW', 'ML_DATASETS', 'ML_MODELS', 'ML_EXPERIMENTS', 'ML_PREDICTIONS', 'ML_TRAINING'].includes(currentView)
  );

  const isMlActive = ['ADMIN_ML', 'ML_OVERVIEW', 'ML_DATASETS', 'ML_MODELS', 'ML_EXPERIMENTS', 'ML_PREDICTIONS', 'ML_TRAINING'].includes(currentView);

  const mainNav = [
    { id: 'DASHBOARD' as DashboardView, label: 'Dashboard', icon: LayoutDashboard },
    { id: 'PLAYERS' as DashboardView, label: 'Players', icon: Users },
    { id: 'PUZZLES' as DashboardView, label: 'Puzzles', icon: Puzzle },
    { id: 'GAMES' as DashboardView, label: 'Games', icon: Gamepad2 },
    { id: 'HINTS' as DashboardView, label: 'Hints', icon: HelpCircle },
    { id: 'MULTIPLAYER' as DashboardView, label: 'Multiplayer', icon: Swords },
    { id: 'ANALYTICS' as DashboardView, label: 'Analytics', icon: BarChart3 },
  ];

  const mlSubNav = [
    { id: 'ADMIN_ML' as DashboardView, label: 'Overview', icon: Activity },
    { id: 'ML_DATASETS' as DashboardView, label: 'Datasets', icon: Database },
    { id: 'ML_MODELS' as DashboardView, label: 'Models', icon: Layers },
    { id: 'ML_EXPERIMENTS' as DashboardView, label: 'Experiments', icon: History },
    { id: 'ML_PREDICTIONS' as DashboardView, label: 'Predictions', icon: Sliders },
    { id: 'ML_TRAINING' as DashboardView, label: 'Training', icon: Cpu },
  ];

  const bottomNav = [
    { id: 'AUDIT_LOGS' as DashboardView, label: 'Audit Logs', icon: FileText },
    { id: 'SETTINGS' as DashboardView, label: 'Settings', icon: Settings },
  ];

  return (
    <aside className="w-full md:w-64 bg-slate-900 border-r border-slate-800 text-slate-100 flex flex-col justify-between shrink-0 p-4 font-sans select-none">
      <div className="space-y-6">
        {/* Professional Header & Identity */}
        <div className="flex items-center gap-3 px-3 py-3 rounded-xl bg-slate-800/80 border border-slate-700/60 shadow-sm">
          <div className="w-9 h-9 rounded-lg bg-blue-600 border border-blue-400/50 flex items-center justify-center text-amber-300 font-black text-base shadow-sm">
            {user?.displayName ? user.displayName.charAt(0).toUpperCase() : 'A'}
          </div>
          <div className="overflow-hidden">
            <h3 className="text-xs font-bold text-white truncate">{user?.displayName || 'Admin Console'}</h3>
            <p className="text-[11px] text-blue-400 font-mono truncate">@{user?.username || 'admin'}</p>
          </div>
        </div>

        {/* Primary Navigation List */}
        <nav className="space-y-1" aria-label="Admin Navigation">
          <div className="px-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            Core Management
          </div>

          {mainNav.map((item) => {
            const Icon = item.icon;
            const isActive = currentView === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onNavigate(item.id)}
                className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold tracking-wide transition-all cursor-pointer ${
                  isActive
                    ? 'bg-blue-600 text-white font-bold shadow'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/70'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <Icon size={16} className={isActive ? 'text-amber-300' : 'text-slate-400'} />
                  <span>{item.label}</span>
                </div>
                {isActive && <ChevronRight size={14} className="text-amber-300" />}
              </button>
            );
          })}

          {/* Machine Learning Parent & Sub-items */}
          <div className="pt-2">
            <button
              onClick={() => setIsMlExpanded(!isMlExpanded)}
              className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold tracking-wide transition-all cursor-pointer ${
                isMlActive
                  ? 'bg-slate-800 text-blue-300 border border-blue-500/30'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/70'
              }`}
            >
              <div className="flex items-center gap-2.5">
                <Brain size={16} className={isMlActive ? 'text-blue-400' : 'text-slate-400'} />
                <span>Machine Learning</span>
              </div>
              {isMlExpanded ? <ChevronDown size={14} className="text-slate-400" /> : <ChevronRight size={14} className="text-slate-400" />}
            </button>

            {isMlExpanded && (
              <div className="ml-4 mt-1 pl-2 border-l border-slate-700/80 space-y-1">
                {mlSubNav.map((sub) => {
                  const SubIcon = sub.icon;
                  const isSubActive = currentView === sub.id;
                  return (
                    <button
                      key={sub.id}
                      onClick={() => onNavigate(sub.id)}
                      className={`w-full flex items-center gap-2 px-2.5 py-1.5 rounded-md text-[11px] font-medium transition-all cursor-pointer ${
                        isSubActive
                          ? 'bg-blue-600/90 text-white font-bold'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                      }`}
                    >
                      <SubIcon size={13} className={isSubActive ? 'text-amber-300' : 'text-slate-400'} />
                      <span>{sub.label}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Bottom Section: Audit Logs & Settings */}
          <div className="pt-3">
            <div className="px-3 pb-1 text-[10px] font-bold uppercase tracking-wider text-slate-400">
              System
            </div>
            {bottomNav.map((item) => {
              const Icon = item.icon;
              const isActive = currentView === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => onNavigate(item.id)}
                  className={`w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-semibold tracking-wide transition-all cursor-pointer ${
                    isActive
                      ? 'bg-blue-600 text-white font-bold shadow'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/70'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <Icon size={16} className={isActive ? 'text-amber-300' : 'text-slate-400'} />
                    <span>{item.label}</span>
                  </div>
                  {isActive && <ChevronRight size={14} className="text-amber-300" />}
                </button>
              );
            })}
          </div>
        </nav>
      </div>

      {/* Action Footer */}
      <div className="space-y-2 pt-4 border-t border-slate-800">
        <button
          onClick={onPlaySudoku}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow transition cursor-pointer"
        >
          <PlayCircle size={15} />
          <span>Launch Gameplay App</span>
        </button>

        <button
          onClick={onLogout}
          className="w-full flex items-center justify-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-400 hover:text-rose-300 hover:bg-rose-950/40 transition cursor-pointer"
        >
          <LogOut size={14} />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
};
