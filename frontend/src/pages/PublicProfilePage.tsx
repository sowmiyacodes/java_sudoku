import React, { useCallback, useEffect, useState } from 'react';
import { AlertCircle, ArrowLeft, Clock, Loader2, RefreshCw, Target, Trophy, UserRound, Zap } from 'lucide-react';
import { LeaderboardApi, type PublicPlayerProfile } from '../services/leaderboardApi';

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

const formatDate = (iso: string): string => {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
};

interface PublicProfilePageProps {
  username: string;
  onBack: () => void;
}

export const PublicProfilePage: React.FC<PublicProfilePageProps> = ({ username, onBack }) => {
  const [profile, setProfile] = useState<PublicPlayerProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadProfile = useCallback((): Promise<void> => {
    return Promise.resolve()
      .then(() => {
        setIsLoading(true);
        setError(null);
        return LeaderboardApi.getPublicProfile(username);
      })
      .then((result) => {
        setProfile(result);
      })
      .catch((caught: unknown) => {
        setError(caught instanceof Error ? caught.message : 'Unable to load this player profile.');
        setProfile(null);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [username]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const statCards = profile
    ? [
        { label: 'All-time rank', value: profile.allTimeRank != null ? `#${profile.allTimeRank}` : '—', icon: Trophy },
        { label: 'Total points', value: profile.totalPoints.toLocaleString(), icon: Zap },
        { label: 'Games completed', value: String(profile.gamesCompleted), icon: Target },
        { label: 'Avg. accuracy', value: `${Math.round(profile.averageAccuracy)}%`, icon: Target },
        { label: 'Best time', value: formatTime(profile.bestTime), icon: Clock },
      ]
    : [];

  return (
    <div className="relative z-10 flex min-h-screen flex-col items-center p-4 pt-20 pb-16">
      <div className="w-full max-w-2xl rounded-3xl border border-cyan-500/30 bg-slate-900/85 p-5 shadow-[0_0_60px_rgba(6,182,212,0.16)] backdrop-blur-xl sm:p-8 animate-fadeIn">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-cyan-200"
        >
          <ArrowLeft size={16} aria-hidden="true" /> Back to leaderboard
        </button>

        {isLoading && (
          <div className="flex flex-col items-center gap-3 py-16" role="status" aria-live="polite">
            <Loader2 className="h-9 w-9 animate-spin text-cyan-400" aria-hidden="true" />
            <p className="text-sm font-semibold tracking-wider text-cyan-200">Loading pilot record...</p>
          </div>
        )}

        {!isLoading && error && (
          <div
            role="alert"
            className="mt-6 flex flex-col items-center gap-3 rounded-2xl border border-rose-500/40 bg-rose-950/40 px-4 py-8 text-center"
          >
            <AlertCircle size={22} className="text-rose-300" aria-hidden="true" />
            <p className="text-sm text-rose-200">{error}</p>
            <button
              type="button"
              onClick={loadProfile}
              className="inline-flex items-center gap-2 rounded-xl border border-cyan-500/40 bg-slate-950/70 px-4 py-2 text-xs font-bold text-cyan-100 transition hover:border-cyan-300/70 hover:bg-cyan-950/50 cursor-pointer"
            >
              <RefreshCw size={14} aria-hidden="true" /> Retry
            </button>
          </div>
        )}

        {!isLoading && !error && profile && (
          <>
            {/* Player header */}
            <div className="mt-6 mb-6 text-center">
              <div className="mb-3 inline-flex h-16 w-16 items-center justify-center rounded-3xl border border-cyan-400/40 bg-cyan-950/60 text-cyan-300 shadow-[0_0_30px_rgba(6,182,212,0.25)]">
                <UserRound size={30} aria-hidden="true" />
              </div>
              <p className="mb-1 font-mono text-xs uppercase tracking-[0.25em] text-cyan-300">Public pilot profile</p>
              <h1 className="text-3xl font-extrabold tracking-wide text-white">{profile.displayName}</h1>
              <p className="mt-1 text-sm text-slate-400">@{profile.username}</p>
              <p className="mt-1 text-xs text-slate-500">Member since {formatDate(profile.memberSince)}</p>
            </div>

            {/* Stats grid */}
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {statCards.map((stat) => (
                <div
                  key={stat.label}
                  className="rounded-2xl border border-slate-700/70 bg-slate-950/60 px-4 py-3 text-center"
                >
                  <stat.icon size={15} className="mx-auto mb-1.5 text-cyan-400" aria-hidden="true" />
                  <p className="text-lg font-extrabold text-white">{stat.value}</p>
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-slate-500">{stat.label}</p>
                </div>
              ))}
            </div>

            {/* Recent scores */}
            <div className="mt-6">
              <h2 className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-300">
                <Trophy size={14} className="text-cyan-400" aria-hidden="true" /> Recent completions
              </h2>

              {profile.recentScores.length === 0 ? (
                <div className="rounded-2xl border border-slate-700/70 bg-slate-950/50 px-4 py-8 text-center">
                  <p className="text-sm text-slate-400">No completed puzzles on record yet.</p>
                </div>
              ) : (
                <ul className="space-y-2">
                  {profile.recentScores.map((score) => (
                    <li
                      key={score.gameId}
                      className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-slate-700/70 bg-slate-950/60 px-4 py-3"
                    >
                      <div className="flex items-center gap-3">
                        <span className="rounded-lg bg-cyan-950/70 border border-cyan-500/30 px-2 py-1 text-[11px] font-bold text-cyan-300">
                          {score.difficulty}
                        </span>
                        <span className="font-mono text-xs text-slate-500">{formatDate(score.completedAt)}</span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-slate-300">
                        <span className="inline-flex items-center gap-1 font-bold text-cyan-300">
                          <Zap size={12} aria-hidden="true" />
                          {score.points} pts
                        </span>
                        <span className="inline-flex items-center gap-1 font-mono">
                          <Clock size={12} className="text-slate-500" aria-hidden="true" />
                          {formatTime(score.elapsedSeconds)}
                        </span>
                        <span className="inline-flex items-center gap-1">
                          <Target size={12} className="text-slate-500" aria-hidden="true" />
                          {score.accuracy}%
                        </span>
                        <span className="text-slate-500">{score.mistakes} ❌ · {score.hintsUsed} 💡</span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <p className="mt-6 text-center text-[11px] leading-5 text-slate-500">
              Public statistics only — email, password and private game moves are never shown.
            </p>
          </>
        )}
      </div>
    </div>
  );
};

export default PublicProfilePage;
