import React from 'react';
import { AlertCircle, BrainCircuit, LoaderCircle } from 'lucide-react';
import type { CompletionPrediction } from '../types/sudoku';

interface CompletionPredictionCardProps {
  prediction: CompletionPrediction | null;
  loading: boolean;
  error: string | null;
  profileNotice?: string | null;
}

export const CompletionPredictionCard: React.FC<CompletionPredictionCardProps> = ({
  prediction,
  loading,
  error,
  profileNotice,
}) => (
  <section
    aria-live="polite"
    className="rounded-2xl border border-violet-500/30 bg-gradient-to-br from-slate-900/90 to-violet-950/30 p-4"
  >
    <div className="mb-3 flex items-center justify-between gap-2">
      <div className="flex items-center gap-2">
        <BrainCircuit className="h-4 w-4 text-violet-300" aria-hidden="true" />
        <h3 className="text-xs font-bold uppercase tracking-wider text-violet-100">
          Completion probability
        </h3>
      </div>
      {loading && <LoaderCircle className="h-4 w-4 animate-spin text-violet-300" aria-label="Updating" />}
      {prediction && (
        <span className="text-[10px] font-mono text-slate-400">
          {prediction.model_version}
        </span>
      )}
    </div>

    {error ? (
      <p role="status" className="flex items-center gap-2 text-xs text-amber-200">
        <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />
        {error}
      </p>
    ) : prediction ? (
      <>
        <div className="flex items-end justify-between gap-3">
          <strong className="text-3xl font-black tabular-nums text-white">
            {Math.round(prediction.completion_probability * 100)}%
          </strong>
          <span className={`pb-1 text-xs font-semibold ${prediction.predicted_completion ? 'text-emerald-300' : 'text-amber-300'}`}>
            {prediction.predicted_completion ? 'Likely to complete' : 'May need support'}
          </span>
        </div>
        <div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-800">
          <div
            className="h-full rounded-full bg-gradient-to-r from-violet-400 to-cyan-300 transition-[width] duration-500"
            style={{ width: `${Math.round(prediction.completion_probability * 100)}%` }}
          />
        </div>
        <p className="mt-2 text-[10px] text-slate-400">
          Confidence {Math.round(prediction.confidence * 100)}% · Refreshes during play
        </p>
        {prediction.top_factors.length > 0 && (
          <ul className="mt-3 space-y-1 text-[11px] text-slate-300">
            {prediction.top_factors.slice(0, 2).map((factor) => (
              <li key={factor}>• {factor}</li>
            ))}
          </ul>
        )}
      </>
    ) : (
      <p className="text-xs text-slate-400">Waiting for the first prediction…</p>
    )}

    {profileNotice && <p className="mt-2 text-[10px] text-slate-500">{profileNotice}</p>}
  </section>
);
