import React, { useState } from 'react';
import { AlertCircle, ArrowLeft, Copy, Loader2, LogIn, Plus, Users } from 'lucide-react';
import { RoomApi, RoomApiError } from '../services/roomApi';
import { useAuth } from '../context/useAuth';

type DifficultyChoice = 'Easy' | 'Medium' | 'Hard' | 'Expert';

const DIFFICULTIES: DifficultyChoice[] = ['Easy', 'Medium', 'Hard', 'Expert'];

interface MultiplayerLandingPageProps {
  onBack: () => void;
  onRoomJoined: (roomCode: string) => void;
  onLogin: () => void;
}

/** Maps room API failures to the explicit UI states: invalid code, full room, expired, closed. */
function describeRoomError(error: unknown): string {
  if (error instanceof RoomApiError) {
    const message = error.message.toLowerCase();
    if (error.status === 404) return 'Invalid room code — double-check the code and try again.';
    if (error.status === 409 && message.includes('full')) return 'Room is full — this mission already has two players (2/2).';
    if (error.status === 409 && message.includes('started')) return 'This room has already started its game.';
    if (error.status === 409 && message.includes('finished')) return 'This room has already finished.';
    if (error.status === 409 && message.includes('no longer')) return 'This room is no longer available.';
    if (error.status === 410) return 'This room has expired — create a new one.';
    if (error.status === 401) return 'Please log in to play multiplayer.';
    return error.message;
  }
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}

export const MultiplayerLandingPage: React.FC<MultiplayerLandingPageProps> = ({
  onBack,
  onRoomJoined,
  onLogin,
}) => {
  const { user } = useAuth();
  const [difficulty, setDifficulty] = useState<DifficultyChoice>('Medium');
  const [roomCode, setRoomCode] = useState('');
  const [busy, setBusy] = useState<'create' | 'join' | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async () => {
    setBusy('create');
    setError(null);
    try {
      const room = await RoomApi.createRoom(difficulty);
      onRoomJoined(room.roomCode);
    } catch (caught) {
      setError(describeRoomError(caught));
    } finally {
      setBusy(null);
    }
  };

  const handleJoin = async (event: React.FormEvent) => {
    event.preventDefault();
    const normalized = roomCode.trim().toUpperCase();
    if (!normalized) {
      setError('Enter a room code to join.');
      return;
    }
    setBusy('join');
    setError(null);
    try {
      const room = await RoomApi.joinRoom(normalized);
      onRoomJoined(room.roomCode);
    } catch (caught) {
      setError(describeRoomError(caught));
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="relative z-10 flex min-h-screen flex-col items-center p-4 pt-20 pb-16">
      <div className="w-full max-w-2xl rounded-3xl border border-emerald-500/30 bg-slate-900/85 p-6 shadow-[0_0_60px_rgba(16,185,129,0.14)] backdrop-blur-xl sm:p-9 animate-fadeIn">
        <button
          type="button"
          onClick={onBack}
          className="inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-emerald-200"
        >
          <ArrowLeft size={16} aria-hidden="true" /> Back to mission control
        </button>

        <div className="mt-5 mb-7 text-center">
          <div className="mb-3 inline-flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-400/40 bg-emerald-950/60 text-emerald-300 shadow-[0_0_30px_rgba(16,185,129,0.25)]">
            <Users size={26} aria-hidden="true" />
          </div>
          <p className="mb-1 font-mono text-xs uppercase tracking-[0.25em] text-emerald-300">✦ Co-op missions ✦</p>
          <h1 className="text-3xl font-extrabold tracking-wider text-white">Multiplayer Sudoku</h1>
          <p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-400">
            Solve one puzzle together — two pilots, one shared board, shared mistakes.
          </p>
        </div>

        {!user ? (
          <div className="rounded-2xl border border-slate-700/70 bg-slate-950/60 px-5 py-8 text-center">
            <p className="text-sm text-slate-300">Multiplayer rooms require an account so your partner knows who they are playing with.</p>
            <button
              type="button"
              onClick={onLogin}
              className="mt-4 inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-400 px-5 py-3 font-bold text-slate-950 transition hover:brightness-110 cursor-pointer"
            >
              <LogIn size={17} /> Log in to play
            </button>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Create room */}
            <section className="rounded-2xl border border-emerald-500/30 bg-emerald-950/20 p-4">
              <h2 className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-emerald-200">
                <Plus size={14} aria-hidden="true" /> Create a room
              </h2>
              <p className="mb-3 text-xs leading-5 text-slate-400">
                Pick a difficulty, get a room code and share it with a friend.
              </p>
              <div className="mb-4 grid grid-cols-4 gap-1.5">
                {DIFFICULTIES.map((diff) => (
                  <button
                    key={diff}
                    type="button"
                    onClick={() => setDifficulty(diff)}
                    className={`rounded-lg py-2 text-[11px] font-bold transition-all cursor-pointer ${
                      difficulty === diff
                        ? 'bg-emerald-400 text-slate-950 shadow-[0_0_15px_rgba(52,211,153,0.5)]'
                        : 'bg-slate-800/70 text-slate-300 border border-slate-700/60 hover:border-emerald-500/40'
                    }`}
                  >
                    {diff}
                  </button>
                ))}
              </div>
              <button
                type="button"
                onClick={handleCreate}
                disabled={busy !== null}
                className="w-full flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-400 px-4 py-3 text-sm font-extrabold text-slate-950 transition hover:brightness-110 disabled:opacity-60 cursor-pointer"
              >
                {busy === 'create' ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />}
                {busy === 'create' ? 'Creating...' : 'Create room'}
              </button>
            </section>

            {/* Join room */}
            <section className="rounded-2xl border border-slate-700/70 bg-slate-950/50 p-4">
              <h2 className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-300">
                <Copy size={14} aria-hidden="true" /> Join a room
              </h2>
              <p className="mb-3 text-xs leading-5 text-slate-400">
                Enter the 6-character code your friend shared with you.
              </p>
              <form onSubmit={handleJoin}>
                <input
                  value={roomCode}
                  onChange={(event) => setRoomCode(event.target.value.toUpperCase())}
                  placeholder="e.g. K7PMQ2"
                  maxLength={8}
                  autoComplete="off"
                  spellCheck={false}
                  aria-label="Room code"
                  className="mb-3 w-full rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-3 text-center font-mono text-lg tracking-[0.4em] uppercase text-cyan-100 outline-none transition placeholder:tracking-[0.3em] placeholder:text-slate-600 focus:border-emerald-400 focus:ring-2 focus:ring-emerald-500/20"
                />
                <button
                  type="submit"
                  disabled={busy !== null}
                  className="w-full flex items-center justify-center gap-2 rounded-xl border border-emerald-500/40 bg-slate-900 px-4 py-3 text-sm font-extrabold text-emerald-100 transition hover:border-emerald-300/70 hover:bg-emerald-950/50 disabled:opacity-60 cursor-pointer"
                >
                  {busy === 'join' ? <Loader2 size={16} className="animate-spin" /> : <Users size={16} />}
                  {busy === 'join' ? 'Joining...' : 'Join room'}
                </button>
              </form>
            </section>
          </div>
        )}

        {/* Error / full-room / invalid-code state */}
        {error && (
          <div
            role="alert"
            className="mt-4 flex items-start gap-3 rounded-2xl border border-rose-500/40 bg-rose-950/40 px-4 py-3"
          >
            <AlertCircle size={17} className="mt-0.5 shrink-0 text-rose-300" aria-hidden="true" />
            <p className="text-sm text-rose-200">{error}</p>
          </div>
        )}

        <p className="mt-6 text-center text-[11px] leading-5 text-slate-500">
          Two players per room · Both players share one board, mistakes and completion status.
        </p>
      </div>
    </div>
  );
};

export default MultiplayerLandingPage;
