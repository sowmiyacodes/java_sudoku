import React from 'react';
import { BarChart3, Brain, Target, X } from 'lucide-react';
import type { PerformanceAnalysis } from '../types/sudoku';

interface PerformanceAnalysisModalProps {
  isOpen: boolean;
  analysis: PerformanceAnalysis | null;
  onClose: () => void;
}

export const PerformanceAnalysisModal: React.FC<PerformanceAnalysisModalProps> = ({
  isOpen,
  analysis,
  onClose,
}) => {
  if (!isOpen || !analysis) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-md">
      <div className="w-full max-w-md rounded-3xl border border-cyan-400/30 bg-slate-900 p-5 shadow-[0_0_50px_rgba(6,182,212,0.2)]">
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-3">
            <div className="rounded-xl border border-cyan-400/30 bg-cyan-500/10 p-2 text-cyan-300">
              <BarChart3 className="h-5 w-5" />
            </div>
            <div>
              <h2 className="font-bold tracking-wide text-cyan-200">PERFORMANCE ANALYSIS</h2>
              <p className="text-xs text-slate-400">A snapshot of this game session</p>
            </div>
          </div>
          <button type="button" onClick={onClose} aria-label="Close analysis" className="text-slate-400 hover:text-white">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="grid grid-cols-3 gap-2 py-4 text-center">
          <div className="rounded-xl border border-emerald-400/20 bg-emerald-950/30 p-3">
            <Target className="mx-auto mb-1 h-4 w-4 text-emerald-300" />
            <div className="text-xl font-bold text-emerald-200">{analysis.accuracy}%</div>
            <div className="text-[10px] uppercase tracking-wide text-slate-400">Accuracy</div>
          </div>
          <div className="rounded-xl border border-amber-400/20 bg-amber-950/30 p-3">
            <div className="text-xl font-bold text-amber-200">{analysis.hintsUsed}</div>
            <div className="text-[10px] uppercase tracking-wide text-slate-400">Hints</div>
          </div>
          <div className="rounded-xl border border-rose-400/20 bg-rose-950/30 p-3">
            <div className="text-xl font-bold text-rose-200">{analysis.mistakes}</div>
            <div className="text-[10px] uppercase tracking-wide text-slate-400">Mistakes</div>
          </div>
        </div>

        <div className="mb-3 rounded-xl border border-violet-400/20 bg-violet-950/20 p-3 text-sm">
          <div className="mb-1 font-semibold text-violet-200">ML difficulty prediction</div>
          <p className="text-slate-300">
            {analysis.predictedDifficulty || 'Pending'}
            {analysis.modelConfidence !== undefined && (
              <span className="ml-2 text-xs text-violet-300">
                ({Math.round(analysis.modelConfidence * 100)}% confidence)
              </span>
            )}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {analysis.historyCount || 0} performance snapshots stored
          </p>
        </div>

        <div className="space-y-3 text-sm">
          <div className="rounded-xl border border-emerald-400/20 bg-emerald-950/20 p-3">
            <div className="mb-1 flex items-center gap-2 font-semibold text-emerald-200"><Brain className="h-4 w-4" />Strong area</div>
            <p className="text-slate-300">{analysis.strongArea}</p>
          </div>
          <div className="rounded-xl border border-amber-400/20 bg-amber-950/20 p-3">
            <div className="mb-1 font-semibold text-amber-200">Needs improvement</div>
            <p className="text-slate-300">{analysis.improvementArea}</p>
          </div>
          <div className="rounded-xl border border-cyan-400/20 bg-cyan-950/20 p-3">
            <div className="mb-1 font-semibold text-cyan-200">Suggested next step</div>
            <p className="text-slate-300">{analysis.recommendation}</p>
          </div>
        </div>

        <button type="button" onClick={onClose} className="mt-4 w-full rounded-xl border border-slate-700 bg-slate-800 py-2.5 text-sm font-semibold text-slate-200 hover:bg-slate-700">
          Close
        </button>
      </div>
    </div>
  );
};
