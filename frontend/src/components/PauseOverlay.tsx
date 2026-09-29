import React from 'react';
import { Play, Clock } from 'lucide-react';

interface PauseOverlayProps {
  isOpen: boolean;
  formattedTime: string;
  onResume: () => void;
}

export const PauseOverlay: React.FC<PauseOverlayProps> = ({ isOpen, formattedTime, onResume }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="w-full max-w-sm p-6 sm:p-8 rounded-3xl bg-slate-900/95 border border-cyan-500/40 shadow-[0_0_50px_rgba(6,182,212,0.25)] text-center transform transition-all animate-scaleUp">
        <div className="w-16 h-16 mx-auto mb-4 rounded-2xl bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
          <Clock className="w-8 h-8" />
        </div>

        <h2 className="text-2xl font-bold tracking-wider text-white mb-1">
          GAME PAUSED
        </h2>
        <p className="text-sm text-slate-400 mb-6">
          Take your time. Your cosmic progress is safely saved.
        </p>

        {/* Current Time Display */}
        <div className="py-3 px-6 rounded-2xl bg-slate-950/80 border border-cyan-500/20 font-mono text-3xl font-bold text-cyan-300 tracking-widest mb-6">
          {formattedTime}
        </div>

        {/* Resume Button */}
        <button
          type="button"
          onClick={onResume}
          className="w-full flex items-center justify-center gap-2 py-3.5 px-6 rounded-2xl font-bold text-slate-950 bg-gradient-to-r from-cyan-400 to-blue-500 hover:from-cyan-300 hover:to-blue-400 shadow-[0_0_25px_rgba(6,182,212,0.5)] transition-all active:scale-98 cursor-pointer"
        >
          <Play className="w-5 h-5 fill-slate-950" />
          <span>Resume Game</span>
        </button>
      </div>
    </div>
  );
};
