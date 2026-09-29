import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AlertCircle, ArrowLeft, Check, Copy, Loader2, Play, UserRound, Wifi, WifiOff, X } from 'lucide-react';
import { RoomApi, RoomApiError, type RoomPlayer, type RoomState } from '../services/roomApi';

interface RoomWaitingPageProps {
  roomCode: string;
  onBack: () => void;
  onGameStarted: () => void;
}

const POLL_INTERVAL_MS = 2000;

function PlayerSlot({ label, player, emptyText }: { label: string; player: RoomPlayer | null; emptyText: string }) {
  return (
    <div
      className={`rounded-2xl border px-4 py-3 ${
        player
          ? player.connected
            ? 'border-emerald-500/40 bg-emerald-950/20'
            : 'border-rose-500/40 bg-rose-950/20'
          : 'border-dashed border-slate-700 bg-slate-950/40'
      }`}
    >
      <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-slate-500">{label}</p>
      {player ? (
        <div className="flex items-center justify-between gap-2">
          <div className="flex min-w-0 items-center gap-2">
            <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-xl bg-slate-800 text-cyan-300">
              <UserRound size={15} aria-hidden="true" />
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-bold text-white">{player.displayName}</p>
              <p className="truncate text-xs text-slate-500">@{player.username}</p>
            </div>
          </div>
          <span
            title={player.connected ? 'Connected' : 'Disconnected'}
            className={`inline-flex items-center gap-1 text-[10px] font-semibold ${
              player.connected ? 'text-emerald-300' : 'text-rose-300'
            }`}
          >
            {player.connected ? <Wifi size={12} aria-hidden="true" /> : <WifiOff size={12} aria-hidden="true" />}
            {player.connected ? 'Online' : 'Offline'}
          </span>
        </div>
      ) : (
        <div className="flex h-[46px] items-center gap-2 text-sm text-slate-500">
          <span className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-dashed border-slate-700">
            <Loader2 size={14} className="animate-spin text-slate-600" aria-hidden="true" />
          </span>
          {emptyText}
        </div>
      )}
    </div>
  );
}

export const RoomWaitingPage: React.FC<RoomWaitingPageProps> = ({ roomCode, onBack, onGameStarted }) => {
  const [state, setState] = useState<RoomState | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [pollFailures, setPollFailures] = useState(0);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState<'start' | 'leave' | null>(null);
  const [copied, setCopied] = useState(false);

  const inFlightRef = useRef(false);
  const startedRef = useRef(false);
  const stateRef = useRef<RoomState | null>(null);

  const describeError = useCallback((caught: unknown): string => {
    if (caught instanceof RoomApiError) {
      if (caught.status === 404) return 'Room not found — it may have been removed.';
      if (caught.status === 403) return 'You are not a member of this room.';
      if (caught.status === 410) return 'This room has expired.';
      return caught.message;
    }
    return caught instanceof Error ? caught.message : 'Unable to reach the room.';
  }, []);

  const fetchOnce = useCallback((): Promise<void> => {
    if (inFlightRef.current) return Promise.resolve();
    inFlightRef.current = true;
    return RoomApi.getState(roomCode)
      .then((next) => {
        stateRef.current = next;
        setState(next);
        setLoadError(null);
        setPollFailures(0);
      })
      .catch((caught: unknown) => {
        setPollFailures((failures) => failures + 1);
        // Only surface a blocking error when we have no live state yet.
        if (!stateRef.current) setLoadError(describeError(caught));
      })
      .finally(() => {
        inFlightRef.current = false;
      });
  }, [roomCode, describeError]);

  // Initial load + REST polling loop.
  useEffect(() => {
    void fetchOnce();
    const id = setInterval(() => void fetchOnce(), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [fetchOnce]);

  // Navigate to the game as soon as the host starts it.
  useEffect(() => {
    if (state?.status === 'IN_PROGRESS' && state.game && !startedRef.current) {
      startedRef.current = true;
      onGameStarted();
    }
  }, [state, onGameStarted]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(roomCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setActionError('Could not copy automatically — share the code manually.');
    }
  };

  const handleStart = async () => {
    setBusy('start');
    setActionError(null);
    try {
      const next = await RoomApi.startGame(roomCode);
      setState(next);
      if (!startedRef.current) {
        startedRef.current = true;
        onGameStarted();
      }
    } catch (caught) {
      setActionError(describeError(caught));
    } finally {
      setBusy(null);
    }
  };

  const handleLeave = async () => {
    setBusy('leave');
    try {
      await RoomApi.leaveRoom(roomCode);
    } catch {
      // Leaving is best-effort; navigation happens regardless.
    } finally {
      setBusy(null);
      onBack();
    }
  };

  // ---- Loading / error states -------------------------------------------------
  if (!state && pollFailures === 0) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3" role="status" aria-live="polite">
          <Loader2 className="h-9 w-9 animate-spin text-emerald-400" aria-hidden="true" />
          <p className="text-sm font-semibold tracking-wider text-emerald-200">Entering the room...</p>
        </div>
      </div>
    );
  }

  if (!state && loadError) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="w-full max-w-md rounded-3xl border border-rose-500/30 bg-slate-900/85 p-6 text-center backdrop-blur-xl">
          <AlertCircle size={26} className="mx-auto mb-3 text-rose-300" aria-hidden="true" />
          <p className="text-sm text-rose-200">{loadError}</p>
          <div className="mt-5 flex justify-center gap-3">
            <button
              type="button"
              onClick={() => { setPollFailures(0); setLoadError(null); void fetchOnce(); }}
              className="rounded-xl border border-emerald-500/40 bg-slate-950/70 px-4 py-2 text-xs font-bold text-emerald-100 transition hover:border-emerald-300/70 cursor-pointer"
            >
              Retry
            </button>
            <button
              type="button"
              onClick={onBack}
              className="rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-2 text-xs font-bold text-slate-300 transition hover:border-slate-500 cursor-pointer"
            >
              Back
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!state) return null;

  const activePlayers = state.players.filter((player) => player.active);
  const host = activePlayers.find((player) => player.role === 'HOST') ?? null;
  const guest = activePlayers.find((player) => player.role === 'GUEST') ?? null;
  const isHost = state.you.role === 'HOST';
  const roomClosed = state.status === 'ABANDONED' || state.status === 'EXPIRED';

  if (roomClosed) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="w-full max-w-md rounded-3xl border border-slate-700 bg-slate-900/85 p-6 text-center backdrop-blur-xl">
          <X size={26} className="mx-auto mb-3 text-slate-400" aria-hidden="true" />
          <h1 className="text-xl font-extrabold text-white">
            {state.status === 'EXPIRED' ? 'Room expired' : 'Room closed'}
          </h1>
          <p className="mt-2 text-sm text-slate-400">
            {state.status === 'EXPIRED'
              ? 'This room was idle for too long. Create a new one to keep playing.'
              : 'The room was abandoned by its players. Create or join another room.'}
          </p>
          <button
            type="button"
            onClick={onBack}
            className="mt-5 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-400 px-5 py-2.5 text-sm font-bold text-slate-950 transition hover:brightness-110 cursor-pointer"
          >
            Back to multiplayer
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="relative z-10 flex min-h-screen flex-col items-center p-4 pt-20 pb-16">
      <div className="w-full max-w-2xl rounded-3xl border border-emerald-500/30 bg-slate-900/85 p-6 shadow-[0_0_60px_rgba(16,185,129,0.14)] backdrop-blur-xl sm:p-8 animate-fadeIn">
        <button
          type="button"
          onClick={handleLeave}
          disabled={busy === 'leave'}
          className="inline-flex items-center gap-2 text-sm text-slate-400 transition hover:text-emerald-200 cursor-pointer disabled:opacity-60"
        >
          <ArrowLeft size={16} aria-hidden="true" /> {busy === 'leave' ? 'Leaving...' : 'Leave room'}
        </button>

        <div className="mt-5 mb-6 text-center">
          <p className="mb-1 font-mono text-xs uppercase tracking-[0.25em] text-emerald-300">✦ Room lobby ✦</p>
          <h1 className="text-2xl font-extrabold tracking-wider text-white">Waiting for your partner</h1>
        </div>

        {/* Shareable room code */}
        <div className="mb-5 rounded-2xl border border-emerald-500/40 bg-emerald-950/30 px-4 py-4 text-center">
          <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-300/80">Share this room code</p>
          <p className="mt-1 font-mono text-4xl font-extrabold tracking-[0.35em] text-white">{state.roomCode}</p>
          <button
            type="button"
            onClick={handleCopy}
            className="mt-3 inline-flex items-center gap-2 rounded-xl border border-emerald-400/50 bg-slate-950/60 px-4 py-2 text-xs font-bold text-emerald-100 transition hover:bg-emerald-900/40 cursor-pointer"
          >
            {copied ? <Check size={14} className="text-emerald-300" aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
            {copied ? 'Copied!' : 'Copy code'}
          </button>
        </div>

        {/* Connection banner */}
        {pollFailures >= 2 && (
          <div role="alert" className="mb-4 flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-950/40 px-3 py-2 text-xs text-amber-200">
            <WifiOff size={14} aria-hidden="true" />
            Connection lost — retrying... Your partner may show as offline until it recovers.
          </div>
        )}
        {actionError && (
          <div role="alert" className="mb-4 flex items-center gap-2 rounded-xl border border-rose-500/40 bg-rose-950/40 px-3 py-2 text-xs text-rose-200">
            <AlertCircle size={14} aria-hidden="true" />
            {actionError}
          </div>
        )}

        {/* Player slots */}
        <div className="mb-5 grid gap-3 sm:grid-cols-2">
          <PlayerSlot label={`Host · ${state.difficulty}`} player={host} emptyText="Host disconnected" />
          <PlayerSlot
            label="Challenger"
            player={guest}
            emptyText={isHost ? 'Waiting for a player to join...' : 'Waiting for another player...'}
          />
        </div>

        {/* Start / wait */}
        {isHost ? (
          <button
            type="button"
            onClick={handleStart}
            disabled={busy !== null || activePlayers.length < 2}
            className="w-full flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-emerald-400 to-teal-400 px-5 py-4 text-base font-extrabold text-slate-950 shadow-[0_0_25px_rgba(52,211,153,0.35)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-50 disabled:shadow-none cursor-pointer"
          >
            {busy === 'start' ? <Loader2 size={18} className="animate-spin" /> : <Play size={18} className="fill-slate-950" />}
            {busy === 'start' ? 'Starting...' : activePlayers.length < 2 ? 'Waiting for a player to start' : 'Start game'}
          </button>
        ) : (
          <div className="rounded-2xl border border-slate-700 bg-slate-950/50 px-4 py-4 text-center">
            <p className="text-sm font-semibold text-slate-300">Waiting for the host to start the game...</p>
            <p className="mt-1 text-xs text-slate-500">You will be moved to the board automatically.</p>
          </div>
        )}

        <p className="mt-5 text-center text-[11px] text-slate-500">
          Status: <span className="font-mono text-slate-400">{state.status}</span> · Difficulty:{' '}
          <span className="font-mono text-slate-400">{state.difficulty}</span> · Both players share one board once the game starts.
        </p>
      </div>
    </div>
  );
};

export default RoomWaitingPage;
