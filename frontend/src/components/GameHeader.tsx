import React from 'react';
import { Pause, Play, AlertCircle, Home, Clock, Sparkles } from 'lucide-react';
import type { Difficulty, GameStatus } from '../types/sudoku';

interface GameHeaderProps {
  difficulty: Difficulty;
  mistakes: number;
  formattedTime: string;
  status: GameStatus;
  onPauseToggle: () => void;
  onHomeClick: () => void;
}

export const GameHeader: React.FC<GameHeaderProps> = ({
  difficulty,
  mistakes,
  formattedTime,
  status,
  onPauseToggle,
  onHomeClick,
}) => {
  const isPaused = status === 'PAUSED';
  const isCompleted = status === 'COMPLETED';

  return (
    <header className="w-full max-w-[500px] mx-auto mb-3 flex items-center justify-between gap-2 p-2.5 sm:p-3 rounded-2xl bg-slate-900/70 border border-cyan-500/20 backdrop-blur-md">
      {/* Home / Back to landing */}
      <button
        type="button"
        onClick={onHomeClick}
        title="Return to Menu"
        className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-slate-300 hover:text-white border border-slate-700/50 transition-all active:scale-95 text-xs font-medium cursor-pointer"
      >
        <Home className="w-3.5 h-3.5 text-cyan-400" />
        <span className="hidden sm:inline">Menu</span>
      </button>

      {/* Center metadata: Difficulty & Mistakes */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Difficulty Badge */}
        <div className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-300 text-xs font-medium tracking-wide">
          <Sparkles className="w-3 h-3 text-cyan-400" />
          <span>{difficulty}</span>
        </div>

        {/* Mistakes Counter */}
        <div className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs font-medium">
          <AlertCircle className="w-3 h-3 text-rose-400" />
          <span>Mistakes: {mistakes}</span>
        </div>
      </div>

      {/* Timer & Pause Toggle */}
      <div className="flex items-center gap-1.5">
        <div className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-950/80 border border-cyan-500/30 font-mono text-cyan-200 text-xs sm:text-sm font-semibold tracking-wider">
          <Clock className="w-3.5 h-3.5 text-cyan-400" />
          <span>{formattedTime}</span>
        </div>

        {!isCompleted && (
          <button
            type="button"
            onClick={onPauseToggle}
            title={isPaused ? 'Resume Game' : 'Pause Game'}
            className={`p-1.5 rounded-lg border transition-all active:scale-95 cursor-pointer ${
              isPaused
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40 hover:bg-emerald-500/30 shadow-[0_0_10px_rgba(16,185,129,0.3)]'
                : 'bg-slate-800/80 text-cyan-300 border-cyan-500/30 hover:bg-cyan-500/20'
            }`}
          >
            {isPaused ? <Play className="w-4 h-4 fill-emerald-300" /> : <Pause className="w-4 h-4" />}
          </button>
        )}
      </div>
    </header>
  );
};
