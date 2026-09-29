import React from 'react';
import { Brain, CheckCircle2, AlertCircle, Cpu } from 'lucide-react';
import type { RecommendationData } from '../types/sudoku';

interface MLInsightsCardProps {
  recommendation: RecommendationData | null;
  isLoading?: boolean;
}

export const MLInsightsCard: React.FC<MLInsightsCardProps> = ({
  recommendation,
  isLoading
}) => {
  if (isLoading) {
    return (
      <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-5 animate-pulse flex flex-col gap-3">
        <div className="h-4 bg-slate-800 rounded w-1/3"></div>
        <div className="h-8 bg-slate-800 rounded w-1/2"></div>
        <div className="h-16 bg-slate-800 rounded w-full"></div>
      </div>
    );
  }

  if (!recommendation) {
    return null;
  }

  const skill = recommendation.skillLevel.toUpperCase();
  const confidencePercent = Math.round(recommendation.confidence * 100);

  const getBadgeStyle = () => {
    switch (skill) {
      case 'EXPERT':
        return 'from-amber-400 to-yellow-500 text-slate-950 border-amber-300 shadow-amber-500/20';
      case 'ADVANCED':
        return 'from-purple-500 to-indigo-600 text-white border-purple-400 shadow-purple-500/20';
      case 'INTERMEDIATE':
        return 'from-cyan-500 to-blue-600 text-slate-950 border-cyan-300 shadow-cyan-500/20';
      default:
        return 'from-emerald-400 to-teal-500 text-slate-950 border-emerald-300 shadow-emerald-500/20';
    }
  };

  return (
    <div className="rounded-2xl border border-cyan-500/30 bg-gradient-to-b from-slate-900/90 to-slate-950/90 p-5 shadow-lg relative overflow-hidden backdrop-blur-md">
      {/* Background Accent Glow */}
      <div className="absolute top-0 right-0 -mr-10 -mt-10 w-36 h-36 bg-cyan-500/10 rounded-full blur-2xl pointer-events-none"></div>

      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Brain size={18} className="text-cyan-400" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-cyan-200">
            Player Skill Intelligence
          </h3>
        </div>
        <div className="flex items-center gap-1.5 text-[11px] font-mono px-2 py-0.5 rounded-full bg-slate-800/80 text-slate-300 border border-slate-700/60">
          <Cpu size={11} className="text-cyan-400" />
          <span>Model {recommendation.modelVersion}</span>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 mb-4">
        <div>
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
            Evaluated Category
          </span>
          <div className="mt-1 flex items-center gap-2.5">
            <span
              className={`inline-block px-3.5 py-1 rounded-lg text-xs font-black tracking-wider uppercase bg-gradient-to-r border shadow-md ${getBadgeStyle()}`}
            >
              {skill}
            </span>
          </div>
        </div>

        {/* Confidence Meter */}
        <div className="sm:text-right">
          <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
            Model Confidence
          </span>
          <div className="mt-1 flex items-center sm:justify-end gap-2">
            <div className="w-24 h-2 bg-slate-800 rounded-full overflow-hidden border border-slate-700/50">
              <div
                className="h-full bg-gradient-to-r from-cyan-400 to-blue-500 rounded-full transition-all duration-500"
                style={{ width: `${confidencePercent}%` }}
              ></div>
            </div>
            <span className="text-xs font-mono font-bold text-cyan-300">
              {confidencePercent}%
            </span>
          </div>
        </div>
      </div>

      {/* Top Performance Drivers */}
      <div>
        <h4 className="text-[11px] font-bold text-slate-300 uppercase tracking-wider mb-2 flex items-center gap-1.5">
          <span>Top Performance Drivers</span>
        </h4>
        <ul className="space-y-1.5">
          {recommendation.topFactors && recommendation.topFactors.length > 0 ? (
            recommendation.topFactors.map((factor, idx) => (
              <li
                key={idx}
                className="flex items-start gap-2 text-xs text-slate-300 bg-slate-900/40 px-2.5 py-1.5 rounded-lg border border-slate-800/60"
              >
                <CheckCircle2 size={13} className="text-cyan-400 shrink-0 mt-0.5" />
                <span>{factor}</span>
              </li>
            ))
          ) : (
            <li className="flex items-center gap-2 text-xs text-slate-400 italic">
              <AlertCircle size={13} />
              <span>Complete more missions to build deep behavioral factors.</span>
            </li>
          )}
        </ul>
      </div>
    </div>
  );
};
