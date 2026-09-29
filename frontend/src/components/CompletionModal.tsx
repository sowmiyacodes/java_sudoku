import React, { useEffect } from 'react';
import confetti from 'canvas-confetti';
import { Trophy, Clock, AlertCircle, Sparkles, RotateCcw, Home } from 'lucide-react';

interface CompletionModalProps {
  isOpen: boolean;
  formattedTime: string;
  mistakes: number;
  difficulty: string;
  onNewGame: () => void;
  onHome: () => void;
}

export const CompletionModal: React.FC<CompletionModalProps> = ({
  isOpen,
  formattedTime,
  mistakes,
  difficulty,
  onNewGame,
  onHome,
}) => {
  useEffect(() => {
    if (isOpen) {
      // Cosmic confetti burst
      try {
        confetti({
          particleCount: 120,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#38bdf8', '#818cf8', '#34d399', '#f43f5e', '#fbbf24', '#ffffff'],
        });
      } catch (e) {
        console.error('Confetti error', e);
      }
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-slate-950/85 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="w-full max-w-md p-6 sm:p-8 rounded-3xl bg-slate-900/95 border border-cyan-400/50 shadow-[0_0_60px_rgba(6,182,212,0.3)] text-center animate-scaleUp">
        {/* Trophy icon */}
        <div className="relative w-20 h-20 mx-auto mb-4 rounded-3xl bg-gradient-to-tr from-cyan-500/20 to-blue-500/20 border border-cyan-400/40 flex items-center justify-center text-cyan-300 shadow-[0_0_30px_rgba(6,182,212,0.4)]">
          <Trophy className="w-10 h-10 text-cyan-300 animate-bounce" />
          <Sparkles className="absolute -top-1 -right-1 w-5 h-5 text-yellow-300 animate-pulse" />
        </div>

        <h2 className="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 via-sky-200 to-blue-400 tracking-wide mb-1">
          🎉 Sudoku Completed!
        </h2>
        <p className="text-sm text-slate-400 mb-6">
          Outstanding focus! You mastered this {difficulty.toLowerCase()} cosmic grid.
        </p>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 gap-3 mb-6">
          <div className="p-3.5 rounded-2xl bg-slate-950/70 border border-cyan-500/20 flex flex-col items-center">
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1">
              <Clock className="w-3.5 h-3.5 text-cyan-400" />
              <span>Time</span>
            </div>
            <span className="font-mono text-xl font-bold text-cyan-200">
              {formattedTime}
            </span>
          </div>

          <div className="p-3.5 rounded-2xl bg-slate-950/70 border border-cyan-500/20 flex flex-col items-center">
            <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1">
              <AlertCircle className="w-3.5 h-3.5 text-rose-400" />
              <span>Mistakes</span>
            </div>
            <span className="font-mono text-xl font-bold text-rose-300">
              {mistakes}
            </span>
          </div>
        </div>

        {/* Action buttons */}
        <div className="flex flex-col gap-2.5">
          <button
            type="button"
            onClick={onNewGame}
            className="w-full flex items-center justify-center gap-2 py-3 px-5 rounded-2xl font-bold text-slate-950 bg-gradient-to-r from-cyan-400 to-blue-500 hover:from-cyan-300 hover:to-blue-400 shadow-[0_0_25px_rgba(6,182,212,0.4)] transition-all active:scale-98 cursor-pointer"
          >
            <RotateCcw className="w-4 h-4 text-slate-950" />
            <span>Play Another Puzzle</span>
          </button>

          <button
            type="button"
            onClick={onHome}
            className="w-full flex items-center justify-center gap-2 py-3 px-5 rounded-2xl font-semibold text-slate-300 bg-slate-800/80 hover:bg-slate-700/80 hover:text-white border border-slate-700/50 transition-all active:scale-98 cursor-pointer"
          >
            <Home className="w-4 h-4 text-cyan-400" />
            <span>Return to Cosmic Hub</span>
          </button>
        </div>
      </div>
    </div>
  );
};
