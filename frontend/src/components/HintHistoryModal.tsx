import React from 'react';
import { History, X, Lightbulb, Clock } from 'lucide-react';
import type { HintHistoryItem } from '../types/sudoku';

interface HintHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  hints: HintHistoryItem[];
}

export const HintHistoryModal: React.FC<HintHistoryModalProps> = ({
  isOpen,
  onClose,
  hints,
}) => {
  if (!isOpen) return null;

  const formatTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  };

  const formatTechnique = (tech: string) => {
    if (!tech) return 'Deduction';
    return tech.replace(/_/g, ' ');
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="w-full max-w-md max-h-[85vh] flex flex-col p-5 sm:p-6 rounded-3xl bg-slate-900/95 border border-cyan-400/40 shadow-[0_0_50px_rgba(6,182,212,0.25)] animate-scaleUp">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-400/30 text-cyan-300">
              <History className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 to-sky-200 tracking-wide">
                HINT HISTORY
              </h2>
              <p className="text-[11px] text-slate-400">
                Hints recorded for the current game
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-all cursor-pointer"
            aria-label="Close Hint History"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Hints List */}
        <div className="flex-1 overflow-y-auto py-4 space-y-3 pr-1 scrollbar-thin">
          {hints.length === 0 ? (
            <div className="py-12 flex flex-col items-center justify-center text-center text-slate-400">
              <div className="p-3 rounded-full bg-slate-800/60 border border-slate-700/50 mb-3 text-slate-500">
                <Lightbulb className="w-6 h-6" />
              </div>
              <p className="text-sm font-medium text-slate-300">No hints used yet.</p>
              <p className="text-xs text-slate-500 mt-1 max-w-[240px]">
                Click the 💡 Hint button during gameplay to record deductions here.
              </p>
            </div>
          ) : (
            hints.map((hint, idx) => (
              <div
                key={hint.id || idx}
                className="p-3.5 rounded-2xl bg-slate-950/70 border border-cyan-500/20 hover:border-cyan-500/40 transition-all"
              >
                <div className="flex items-center justify-between gap-2 mb-1.5">
                  <span className="px-2.5 py-0.5 rounded-lg text-[10px] font-bold tracking-wider uppercase bg-amber-500/15 text-amber-300 border border-amber-500/30">
                    {formatTechnique(hint.technique)}
                  </span>
                  {hint.createdAt && (
                    <span className="flex items-center gap-1 text-[10px] text-slate-400 font-mono">
                      <Clock className="w-3 h-3 text-cyan-400/70" />
                      {formatTime(hint.createdAt)}
                    </span>
                  )}
                </div>

                <div className="text-xs font-semibold text-cyan-200 mb-1">
                  Row {hint.row + 1}, Column {hint.column + 1} &rarr; Value:{' '}
                  <span className="text-amber-300 font-bold">{hint.value}</span>
                </div>

                <p className="text-xs text-slate-300/90 leading-relaxed">
                  {hint.explanation}
                </p>
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-white/10">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-2.5 rounded-xl font-bold text-xs tracking-wider bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-all cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
