import React from 'react';
import { Play, RefreshCw, Sparkles } from 'lucide-react';
import type { Difficulty, RecommendationData } from '../types/sudoku';

interface RecommendationCardProps {
  recommendation: RecommendationData | null;
  isLoading?: boolean;
  onRefresh?: () => void;
  onLaunchPuzzle: (difficulty: Difficulty) => void;
}

export const RecommendationCard: React.FC<RecommendationCardProps> = ({
  recommendation,
  isLoading,
  onRefresh,
  onLaunchPuzzle
}) => {
  if (isLoading) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 animate-pulse flex flex-col gap-3">
        <div className="h-4 bg-slate-800 rounded w-1/3"></div>
        <div className="h-10 bg-slate-800 rounded w-1/2"></div>
        <div className="h-14 bg-slate-800 rounded w-full"></div>
      </div>
    );
  }

  if (!recommendation) return null;

  const diff = recommendation.recommendedDifficulty;

  const getDiffColor = (d: string) => {
    switch (d?.toLowerCase()) {
      case 'hard':
      case 'expert':
        return 'text-rose-400 border-rose-500/40 bg-rose-950/40';
      case 'medium':
        return 'text-amber-300 border-amber-500/40 bg-amber-950/40';
      default:
        return 'text-emerald-300 border-emerald-500/40 bg-emerald-950/40';
    }
  };

  return (
    <div className="rounded-2xl border border-amber-500/30 bg-gradient-to-b from-slate-900/90 to-slate-950/90 p-5 shadow-lg relative overflow-hidden backdrop-blur-md">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Sparkles size={18} className="text-amber-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-amber-200">
            Personalized Puzzle Recommendation
          </h3>
        </div>
        {onRefresh && (
          <button
            onClick={onRefresh}
            className="p-1.5 rounded-lg text-slate-400 hover:text-cyan-300 hover:bg-slate-800/80 transition-colors"
            title="Recalculate recommendations"
          >
            <RefreshCw size={14} />
          </button>
        )}
      </div>

      <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800/80 mb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
            Target Challenge Tier
          </span>
          <div className="mt-1 flex items-center gap-2">
            <span
              className={`px-3 py-1 rounded-lg text-xs font-extrabold tracking-wider uppercase border ${getDiffColor(
                diff
              )}`}
            >
              {diff} MISSION
            </span>
          </div>
        </div>

        <button
          onClick={() => onLaunchPuzzle(diff)}
          className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-gradient-to-r from-amber-400 to-yellow-500 hover:from-amber-300 hover:to-yellow-400 text-slate-950 font-black text-xs uppercase tracking-wider shadow-md hover:shadow-amber-500/20 transition-all cursor-pointer"
        >
          <Play size={14} className="fill-current" />
          <span>Start {diff} Puzzle</span>
        </button>
      </div>

      <div className="rounded-xl bg-slate-900/40 p-3.5 border border-slate-800/60">
        <p className="text-xs text-slate-300 leading-relaxed">
          <strong className="text-slate-100 font-semibold mr-1.5">Coaching Note:</strong>
          {recommendation.reason}
        </p>
      </div>
    </div>
  );
};
