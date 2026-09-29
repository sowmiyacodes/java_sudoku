import { useEffect, useState, type FormEvent } from 'react';
import {
  Brain,
  Calendar,
  Gamepad2,
  Loader2,
  LogOut,
  Save
} from 'lucide-react';
import { useAuth } from '../context/useAuth';
import { PlayerApi } from '../services/playerApi';
import type { PlayerProfile } from '../types/sudoku';
import { AuthPageFrame, fieldClassName, labelClassName, primaryButtonClassName } from './AuthPageStyles';

interface ProfilePageProps {
  onBack: () => void;
  onLogout: () => void;
}

const formatTime = (seconds: number): string => {
  if (!seconds || seconds <= 0) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${String(secs).padStart(2, '0')}`;
};

export function ProfilePage({ onBack, onLogout }: ProfilePageProps) {
  const { user, updateProfile, logout } = useAuth();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const [profile, setProfile] = useState<PlayerProfile | null>(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);

  useEffect(() => {
    PlayerApi.getMyProfile()
      .then((data) => setProfile(data))
      .catch((err) => console.error('Error fetching player profile:', err))
      .finally(() => setIsProfileLoading(false));
  }, []);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');
    setError('');
    setIsSaving(true);
    try {
      await updateProfile({ displayName, email });
      setMessage('Profile updated.');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to update your profile.');
    } finally {
      setIsSaving(false);
    }
  };

  const signOut = async () => {
    setIsLoggingOut(true);
    try {
      await logout();
      onLogout();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Unable to log out.');
      setIsLoggingOut(false);
    }
  };

  return (
    <AuthPageFrame
      eyebrow="Pilot Profile & Intelligence"
      title="Player Profile"
      description="View your full intelligence telemetry, skill classification, and account credentials."
      onBack={onBack}
    >
      {/* Skill & AI Intelligence Badge */}
      <section className="mb-6 rounded-2xl border border-cyan-500/30 bg-gradient-to-r from-slate-900/90 to-cyan-950/40 p-4">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Brain size={16} className="text-cyan-400" />
            <h2 className="text-xs font-bold uppercase tracking-wider text-cyan-200">
              Evaluated Skill & Recommendation
            </h2>
          </div>
          {profile?.registrationDate && (
            <span className="text-[10px] text-slate-400 font-mono flex items-center gap-1">
              <Calendar size={11} /> Joined {new Date(profile.registrationDate).toLocaleDateString()}
            </span>
          )}
        </div>

        {isProfileLoading ? (
          <div className="flex items-center gap-2 py-3 text-xs text-slate-400">
            <Loader2 size={15} className="animate-spin text-cyan-400" /> Loading intelligence...
          </div>
        ) : profile ? (
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-3 p-3 rounded-xl bg-slate-950/60 border border-slate-800">
              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Skill Level</span>
                <p className="text-sm font-black text-cyan-300 uppercase tracking-wide">
                  {profile.currentSkillLevel}
                </p>
              </div>

              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Confidence</span>
                <p className="text-sm font-black text-blue-300 font-mono">
                  {Math.round(profile.skillConfidence * 100)}%
                </p>
              </div>

              <div>
                <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Recommended Tier</span>
                <p className="text-sm font-black text-amber-300 font-bold">
                  {profile.recommendedDifficulty}
                </p>
              </div>
            </div>

            <p className="text-xs text-slate-300 bg-slate-900/50 p-2.5 rounded-lg border border-slate-800/80">
              <strong className="text-cyan-300">Coaching Note: </strong>
              {profile.recommendationReason}
            </p>
          </div>
        ) : (
          <p className="text-xs text-slate-400">Telemetry unavailable.</p>
        )}
      </section>

      {/* Comprehensive Statistics Grid (18 attributes) */}
      <section className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/40 p-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300 mb-3 flex items-center gap-1.5">
          <Gamepad2 size={15} className="text-cyan-400" />
          <span>Gameplay History & Precision Telemetry</span>
        </h3>

        {profile && (
          <div className="grid grid-cols-3 gap-2.5 text-center text-xs">
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-white">{profile.gamesPlayed}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Played</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-emerald-300">{profile.gamesCompleted}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Completed</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-slate-400">{profile.gamesAbandoned}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Abandoned</p>
            </div>

            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-blue-300">{Math.round(profile.completionRate * 100)}%</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Solve Rate</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-indigo-300">{Math.round(profile.averageAccuracy * 100)}%</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Accuracy</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-amber-300">{profile.averageScore}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Avg Score</p>
            </div>

            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-purple-300 font-mono">{formatTime(profile.averageSolvingTime)}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Avg Time</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-emerald-300 font-mono">{formatTime(profile.bestSolvingTime)}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Best Time</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-rose-300">{profile.totalMistakes}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Total Mistakes</p>
            </div>

            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-rose-300">{profile.averageMistakesPerGame}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Avg Mistakes</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-amber-300">{profile.averageHintsPerGame}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Avg Hints</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-slate-800/60">
              <p className="text-base font-extrabold text-cyan-300">{profile.averageUndosPerGame}</p>
              <p className="text-[9px] uppercase tracking-wider text-slate-400 font-bold">Avg Undos</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-emerald-500/30">
              <p className="text-base font-extrabold text-emerald-400 font-mono">{profile.currentStreak ?? 0}</p>
              <p className="text-[9px] uppercase tracking-wider text-emerald-300 font-bold">Current Streak</p>
            </div>
            <div className="p-2 rounded-lg bg-slate-950/60 border border-amber-500/30">
              <p className="text-base font-extrabold text-amber-400 font-mono">{profile.bestStreak ?? 0}</p>
              <p className="text-[9px] uppercase tracking-wider text-amber-300 font-bold">Best Streak</p>
            </div>
          </div>
        )}
      </section>

      {/* Account Settings Form */}
      <form onSubmit={submit} className="space-y-4">
        <div className="rounded-xl border border-slate-700/70 bg-slate-950/50 px-4 py-3">
          <p className={labelClassName}>Username</p>
          <p className="mt-1 text-sm font-mono text-cyan-300">@{user?.username}</p>
        </div>
        <label className={labelClassName}>
          Display Name
          <input
            className={fieldClassName}
            autoComplete="name"
            maxLength={80}
            required
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
          />
        </label>
        <label className={labelClassName}>
          Email Address
          <input
            className={fieldClassName}
            type="email"
            autoComplete="email"
            maxLength={254}
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        {message && <p role="status" className="text-xs text-emerald-300">{message}</p>}
        {error && <p role="alert" className="text-xs text-rose-300">{error}</p>}
        <button className={primaryButtonClassName} type="submit" disabled={isSaving}>
          {isSaving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
          {isSaving ? 'Updating Credentials...' : 'Save Credentials'}
        </button>
      </form>

      <button
        type="button"
        onClick={signOut}
        disabled={isLoggingOut}
        className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl border border-rose-500/40 px-4 py-2.5 text-xs font-bold text-rose-200 hover:bg-rose-950/40 transition disabled:opacity-60 cursor-pointer"
      >
        {isLoggingOut ? <Loader2 size={15} className="animate-spin" /> : <LogOut size={15} />}
        {isLoggingOut ? 'Signing out...' : 'Sign Out of Terminal'}
      </button>
    </AuthPageFrame>
  );
}