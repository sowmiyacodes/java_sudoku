import React from 'react';
import { Undo2, Redo2, Eraser, RotateCcw, CheckCircle2, History, BarChart3 } from 'lucide-react';

interface GameControlsProps {
  canUndo: boolean;
  canRedo: boolean;
  onUndo: () => void;
  onRedo: () => void;
  onErase: () => void;
  onRestart: () => void;
  onSubmit: () => void;
  onHint: () => void;
  onHintHistory: () => void;
  onAnalysis: () => void;
  isPaused: boolean;
  isCompleted: boolean;
}

export const GameControls: React.FC<GameControlsProps> = ({
  canUndo,
  canRedo,
  onUndo,
  onRedo,
  onErase,
  onRestart,
  onSubmit,
  onHint,
  onHintHistory,
  onAnalysis,
  isPaused,
  isCompleted,
}) => {
  const disabled = isPaused || isCompleted;

  return (
    <div className="w-full flex flex-col gap-3">
      {/* Undo / Redo / Erase / Restart Group */}
      <div className="grid grid-cols-4 gap-1.5 sm:gap-2 w-full">
        {/* Undo */}
        <button
          type="button"
          disabled={!canUndo || disabled}
          onClick={onUndo}
          title="Undo move (Ctrl+Z)"
          className="flex flex-col sm:flex-row items-center justify-center gap-1 px-2 py-2.5 rounded-xl text-xs sm:text-sm font-medium bg-slate-800/70 hover:bg-slate-700/80 text-cyan-200 border border-cyan-500/25 transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer"
        >
          <Undo2 className="w-4 h-4 shrink-0" />
          <span className="hidden sm:inline">Undo</span>
        </button>

        {/* Redo */}
        <button
          type="button"
          disabled={!canRedo || disabled}
          onClick={onRedo}
          title="Redo move (Ctrl+Y)"
          className="flex flex-col sm:flex-row items-center justify-center gap-1 px-2 py-2.5 rounded-xl text-xs sm:text-sm font-medium bg-slate-800/70 hover:bg-slate-700/80 text-cyan-200 border border-cyan-500/25 transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer"
        >
          <Redo2 className="w-4 h-4 shrink-0" />
          <span className="hidden sm:inline">Redo</span>
        </button>

        {/* Erase */}
        <button
          type="button"
          disabled={disabled}
          onClick={onErase}
          title="Erase cell (Backspace / Del)"
          className="flex flex-col sm:flex-row items-center justify-center gap-1 px-2 py-2.5 rounded-xl text-xs sm:text-sm font-medium bg-rose-950/40 hover:bg-rose-900/50 text-rose-300 border border-rose-500/30 transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer"
        >
          <Eraser className="w-4 h-4 shrink-0" />
          <span>Erase</span>
        </button>

        {/* Restart */}
        <button
          type="button"
          disabled={isPaused}
          onClick={onRestart}
          title="Restart current puzzle"
          className="flex flex-col sm:flex-row items-center justify-center gap-1 px-2 py-2.5 rounded-xl text-xs sm:text-sm font-medium bg-slate-800/70 hover:bg-slate-700/80 text-slate-300 border border-slate-700/50 transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer"
        >
          <RotateCcw className="w-4 h-4 text-slate-400 shrink-0" />
          <span className="hidden sm:inline">Restart</span>
        </button>
      </div>

      {/* Hint and History Group */}
      <div className="flex flex-row gap-2">
        {/* Hint */}
        <button
          type="button"
          disabled={disabled}
          onClick={onHint}
          className="flex-1 flex items-center justify-center gap-1.5 py-2.5 sm:py-3 px-3 rounded-xl text-xs sm:text-sm font-bold bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/50 shadow-[0_0_15px_rgba(245,158,11,0.2)] hover:shadow-[0_0_20px_rgba(245,158,11,0.3)] transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer tracking-wider"
        >
          <span>💡 Hint</span>
        </button>

        {/* Hint History */}
        <button
          type="button"
          disabled={isPaused}
          onClick={onHintHistory}
          title="View hints used in current game"
          className="flex-1 flex items-center justify-center gap-1.5 py-2.5 sm:py-3 px-3 rounded-xl text-xs sm:text-sm font-bold bg-slate-800/80 hover:bg-slate-700/80 text-cyan-300 border border-cyan-500/30 shadow-[0_0_15px_rgba(6,182,212,0.15)] transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer tracking-wider"
        >
          <History className="w-4 h-4 shrink-0" />
          <span>History</span>
        </button>

        <button
          type="button"
          disabled={isPaused}
          onClick={onAnalysis}
          title="View performance analysis"
          className="flex items-center justify-center gap-1.5 rounded-xl border border-emerald-500/30 bg-slate-800/80 px-3 py-2.5 text-xs font-bold tracking-wider text-emerald-300 transition-all hover:bg-slate-700/80 disabled:cursor-not-allowed disabled:opacity-35"
        >
          <BarChart3 className="h-4 w-4" />
          <span className="hidden sm:inline">Analysis</span>
        </button>
      </div>

      {/* Submit / Check Puzzle Button */}
      <button
        type="button"
        disabled={disabled}
        onClick={onSubmit}
        className="w-full flex items-center justify-center gap-2 py-2.5 sm:py-3 px-4 rounded-xl text-xs sm:text-sm font-bold bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 shadow-[0_0_18px_rgba(6,182,212,0.4)] hover:shadow-[0_0_24px_rgba(6,182,212,0.6)] transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer uppercase tracking-wider"
      >
        <CheckCircle2 className="w-4 h-4 sm:w-5 sm:h-5 text-slate-950 shrink-0" />
        <span>Submit Solution</span>
      </button>
    </div>
  );
};
