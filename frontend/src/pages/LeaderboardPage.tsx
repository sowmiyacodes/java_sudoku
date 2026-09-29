import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AlertCircle, ArrowLeft, Clock, Loader2, RefreshCw, Target, Trophy, Zap } from 'lucide-react';
import { LeaderboardApi, type LeaderboardEntry } from '../services/leaderboardApi';
import { useAuth } from '../context/useAuth';

type Period = 'all-time' | 'weekly' | 'daily';

const PERIODS: { key: Period; label: string }[] = [
  { key: 'all-time', label: 'All-Time' },
  { key: 'weekly', label: 'Weekly' },
  { key: 'daily', label: 'Daily' },
];

const formatTime = (seconds: number): string => {
  if (!seconds || seconds <= 0) return '—';
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  if (hrs > 0) {
    return `${hrs}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
};

interface LeaderboardPageProps {
  onBack: () => void;
  onSelectProfile: (username: string) => void;
}

export const LeaderboardPage: React.FC<LeaderboardPageProps> = ({ onBack, onSelectProfile }) => {
  const { user } = useAuth();
  const [period, setPeriod] = useState<Period>('all-time');
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [myEntry, setMyEntry] = useState<LeaderboardEntry | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const requestIdRef = useRef(0);

  const loadLeaderboard = useCallback(
    (selectedPeriod: Period): Promise<void> => {
      const requestId = ++requestIdRef.current;
      const isCurrent = () => requestId === requestIdRef.current;
      return Promise.resolve()
        .then(() => {
          if (!isCurrent()) return null;
          setIsLoading(true);
          setError(null);
          return Promise.all([
            LeaderboardApi.getLeaderboard(selectedPeriod, 50),
            user ? LeaderboardApi.getMyRank(selectedPeriod).catch(() => null) : Promise.resolve(null),
          ]);
        })
        .then((result) => {
          if (!result || !isCurrent()) return;
          setEntries(result[0] ?? []);
          setMyEntry(result[1] ?? null);
        })
        .catch((caught: unknown) => {
          if (!isCurrent()) return;
          setError(caught instanceof Error ? caught.message : 'Unable to load the leaderboard. Please try again.');
          setEntries([]);
          setMyEntry(null);
        })
        .finally(() => {
          if (isCurrent()) setIsLoading(false);
        });
    },
    [user],
  );

  useEffect(() => {
    loadLeaderboard(period);
  }, [period, loadLeaderboard]);

  return (
    <div className="relative z-10 flex min-h-screen flex-col items-center p-4 pt-20 pb-16">
      <div className="w-full max-w-3xl rounded-3xl border border-cyan-500/30 bg-slate-900/85 p-5 shadow-[0_0_60px_rgba(6,182,212,0.16)] backdrop-blur-xl sm:p-8 animate-fadeIn">
        {/* Header */}
        <div className="flex items-start justify-between gap-3">
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-cyan-200"
          >
            <ArrowLeft size={16} aria-hidden="true" /> Back to mission control
          </button>
        </div>

        <div className="mt-5 mb-6 text-center">
          <div className="mb-3 inline-flex h-14 w-14 items-center justify-center rounded-2xl border border-cyan-400/40 bg-cyan-950/60 text-cyan-300 shadow-[0_0_30px_rgba(6,182,212,0.25)]">
            <Trophy size={26} aria-hidden="true" />
          </div>
          <p className="mb-1 font-mono text-xs uppercase tracking-[0.25em] text-cyan-300">✦ Hall of Fame ✦</p>
          <h1 className="text-3xl font-extrabold tracking-wider text-white">Leaderboard</h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
            Ranked by total points, then completed games, then best completion time.
          </p>
        </div>

        {/* Period Tabs */}
        <div className="mb-5 grid grid-cols-3 gap-2" role="tablist" aria-label="Leaderboard period">
          {PERIODS.map((option) => {
            const isSelected = period === option.key;
            return (
              <button
                key={option.key}
                type="button"
                role="tab"
                aria-selected={isSelected}
                onClick={() => setPeriod(option.key)}
                className={`rounded-xl py-2.5 px-3 text-xs font-bold tracking-wider transition-all duration-200 cursor-pointer ${
                  isSelected
                    ? 'bg-cyan-500 text-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.6)] border-transparent'
                    : 'bg-slate-800/70 text-slate-300 border border-slate-700/60 hover:text-white hover:border-cyan-500/30'
                }`}
              >
                {option.label}
              </button>
            );
          })}
        </div>

        {/* My Rank Card */}
        {user && !isLoading && !error && (
          <div className="mb-5 rounded-2xl border border-cyan-500/40 bg-cyan-950/40 p-4">
            {myEntry ? (
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <span className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-400 font-extrabold text-slate-950">
                    #{myEntry.rank}
                  </span>
                  <div>
                    <p className="text-sm font-bold text-white">Your rank · {myEntry.displayName}</p>
                    <p className="font-mono text-xs text-cyan-300/80">
                      {myEntry.totalPoints} pts · {myEntry.gamesCompleted} games
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4 text-xs text-slate-300">
                  <span className="inline-flex items-center gap-1">
                    <Target size={13} className="text-cyan-400" aria-hidden="true" />
                    {Math.round(myEntry.averageAccuracy)}% acc
                  </span>
                  <span className="inline-flex items-center gap-1 font-mono">
                    <Clock size={13} className="text-cyan-400" aria-hidden="true" />
                    {formatTime(myEntry.bestTime)}
                  </span>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-300">
                You are not ranked yet — complete a puzzle to earn points and claim your place among the stars.
              </p>
            )}
          </div>
        )}

        {/* Error State */}
        {error && (
          <div
            role="alert"
            className="mb-5 flex flex-col items-center gap-3 rounded-2xl border border-rose-500/40 bg-rose-950/40 px-4 py-6 text-center"
          >
            <AlertCircle size={22} className="text-rose-300" aria-hidden="true" />
            <p className="text-sm text-rose-200">{error}</p>
            <button
              type="button"
              onClick={() => loadLeaderboard(period)}
              className="inline-flex items-center gap-2 rounded-xl border border-cyan-500/40 bg-slate-950/70 px-4 py-2 text-xs font-bold text-cyan-100 transition hover:border-cyan-300/70 hover:bg-cyan-950/50 cursor-pointer"
            >
              <RefreshCw size={14} aria-hidden="true" /> Retry
            </button>
          </div>
        )}

        {/* Loading State */}
        {isLoading && (
          <div className="flex flex-col items-center gap-3 py-14" role="status" aria-live="polite">
            <Loader2 className="h-9 w-9 animate-spin text-cyan-400" aria-hidden="true" />
            <p className="text-sm font-semibold tracking-wider text-cyan-200">Syncing star charts...</p>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && entries.length === 0 && (
          <div className="flex flex-col items-center gap-3 rounded-2xl border border-slate-700/70 bg-slate-950/50 px-4 py-12 text-center">
            <Trophy size={26} className="text-slate-500" aria-hidden="true" />
            <p className="text-sm font-semibold text-slate-300">No scores on this chart yet</p>
            <p className="max-w-sm text-xs leading-5 text-slate-500">
              Finish a puzzle during this period and your name will light up the leaderboard.
            </p>
          </div>
        )}

        {/* Leaderboard Table */}
        {!isLoading && !error && entries.length > 0 && (
          <div className="overflow-hidden rounded-2xl border border-slate-700/70">
            {/* Table header */}
            <div className="grid grid-cols-[3rem_1fr_4.5rem_3.5rem_4rem_4.5rem] gap-2 border-b border-slate-700/70 bg-slate-950/70 px-3 py-2.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 sm:px-4">
              <span>Rank</span>
              <span>Pilot</span>
              <span className="text-right">Points</span>
              <span className="text-right">Games</span>
              <span className="text-right">Acc.</span>
              <span className="text-right">Best</span>
            </div>

            <ul className="divide-y divide-slate-800/80">
              {entries.map((entry) => {
                const isMe = user != null && entry.userId === user.id;
                return (
                  <li key={entry.userId}>
                    <button
                      type="button"
                      onClick={() => onSelectProfile(entry.username)}
                      title={`View ${entry.displayName}'s public profile`}
                      className={`grid w-full grid-cols-[3rem_1fr_4.5rem_3.5rem_4rem_4.5rem] items-center gap-2 px-3 py-3 text-left transition cursor-pointer sm:px-4 ${
                        isMe
                          ? 'bg-cyan-950/30 hover:bg-cyan-950/50'
                          : 'hover:bg-slate-800/50'
                      }`}
                    >
                      <span
                        className={`inline-flex h-7 w-7 items-center justify-center rounded-lg text-xs font-extrabold ${
                          entry.rank === 1
                            ? 'bg-yellow-400/90 text-slate-950'
                            : entry.rank === 2
                              ? 'bg-slate-300/80 text-slate-950'
                              : entry.rank === 3
                                ? 'bg-amber-600/80 text-white'
                                : 'bg-slate-800 text-slate-300'
                        }`}
                      >
                        {entry.rank}
                      </span>
                      <span className="min-w-0">
                        <span className="block truncate text-sm font-bold text-white">
                          {entry.displayName}
                          {isMe && <span className="ml-2 text-[10px] font-semibold text-cyan-300">(you)</span>}
                        </span>
                        <span className="block truncate text-xs text-slate-500">@{entry.username}</span>
                      </span>
                      <span className="text-right text-sm font-extrabold text-cyan-300">
                        <span className="inline-flex items-center gap-1">
                          <Zap size={12} className="hidden text-cyan-400 sm:inline" aria-hidden="true" />
                          {entry.totalPoints.toLocaleString()}
                        </span>
                      </span>
                      <span className="text-right text-sm text-slate-300">{entry.gamesCompleted}</span>
                      <span className="text-right text-sm text-slate-300">{Math.round(entry.averageAccuracy)}%</span>
                      <span className="text-right font-mono text-xs text-slate-400">{formatTime(entry.bestTime)}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        )}

        <p className="mt-5 text-center text-[11px] leading-5 text-slate-500">
          Tie-break order: total points → completed games → best completion time → player ID.
          <br />
          Click a pilot to open their public profile.
        </p>
      </div>
    </div>
  );
};

export default LeaderboardPage;
