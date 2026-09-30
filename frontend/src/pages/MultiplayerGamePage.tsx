import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlertCircle, ArrowLeft, BarChart3, CheckCircle2, Clock, History, Loader2, PlugZap, Trophy, UserRound, Wifi, WifiOff } from 'lucide-react';
import type { CellCoordinate, HintHistoryItem, HintResponse, PerformanceAnalysis } from '../types/sudoku';
import { GameApi } from '../services/gameApi';
import { SudokuBoard } from '../components/SudokuBoard';
import { NumberPad } from '../components/NumberPad';
import { HintHistoryModal } from '../components/HintHistoryModal';
import { PerformanceAnalysisModal } from '../components/PerformanceAnalysisModal';
import { CompletionPredictionCard } from '../components/CompletionPredictionCard';
import { useGameTimer } from '../hooks/useGameTimer';
import { useCompletionPrediction } from '../hooks/useCompletionPrediction';
import { RoomApi, RoomApiError, type RoomPlayer, type RoomState } from '../services/roomApi';

interface MultiplayerGamePageProps {
  roomCode: string;
  onHome: () => void;
}

const POLL_INTERVAL_MS = 2000;

function describeError(caught: unknown): string {
  if (caught instanceof RoomApiError) {
    if (caught.status === 404) return 'Room not found.';
    if (caught.status === 403) return 'You are not a member of this room.';
    if (caught.status === 410) return 'This room has expired.';
    return caught.message;
  }
  return caught instanceof Error ? caught.message : 'Unable to reach the room.';
}

function PlayerPill({ player, isYou }: { player: RoomPlayer | null; isYou: boolean }) {
  if (!player) {
    return (
      <div className="flex items-center gap-2 rounded-xl border border-dashed border-slate-700 bg-slate-950/40 px-3 py-1.5">
        <span className="text-xs text-slate-500">Empty slot</span>
      </div>
    );
  }
  return (
    <div
      className={`flex items-center gap-2 rounded-xl border px-3 py-1.5 ${
        player.connected ? 'border-cyan-500/30 bg-slate-950/60' : 'border-rose-500/40 bg-rose-950/20'
      }`}
      title={player.connected ? 'Connected' : 'Disconnected'}
    >
      <span className="inline-flex h-6 w-6 items-center justify-center rounded-lg bg-slate-800 text-cyan-300">
        <UserRound size={13} aria-hidden="true" />
      </span>
      <span className="max-w-28 truncate text-xs font-bold text-white">
        {player.displayName}
        {isYou && <span className="ml-1 text-[9px] font-semibold text-cyan-300">(you)</span>}
      </span>
      {player.connected ? (
        <Wifi size={12} className="text-emerald-300" aria-hidden="true" />
      ) : (
        <WifiOff size={12} className="text-rose-300" aria-hidden="true" />
      )}
      {player.pointsAwarded != null && (
        <span className="font-mono text-[10px] font-bold text-amber-300">{player.pointsAwarded}p</span>
      )}
    </div>
  );
}

export const MultiplayerGamePage: React.FC<MultiplayerGamePageProps> = ({ roomCode, onHome }) => {
  const [room, setRoom] = useState<RoomState | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [pollFailures, setPollFailures] = useState(0);
  const [selectedCell, setSelectedCell] = useState<CellCoordinate | null>(null);
  const [errorCells, setErrorCells] = useState<CellCoordinate[]>([]);
  const [notification, setNotification] = useState<{ type: 'error' | 'success' | 'info'; message: string } | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [isLeaving, setIsLeaving] = useState(false);
  const [activeHint, setActiveHint] = useState<HintResponse | null>(null);
  const [hintHistory, setHintHistory] = useState<HintHistoryItem[]>([]);
  const [isHintHistoryOpen, setIsHintHistoryOpen] = useState(false);
  const [analysis, setAnalysis] = useState<PerformanceAnalysis | null>(null);
  const [isAnalysisOpen, setIsAnalysisOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isHintPending, setIsHintPending] = useState(false);
  const [hintLevel, setHintLevel] = useState(1);

  const inFlightRef = useRef(false);
  const stateRef = useRef<RoomState | null>(null);
  const versionRef = useRef(-1);
  const hintHistoryVersionRef = useRef(-1);

  const notify = useCallback((type: 'error' | 'success' | 'info', message: string, duration = 3500) => {
    setNotification({ type, message });
    setTimeout(() => {
      setNotification((prev) => (prev?.message === message ? null : prev));
    }, duration);
  }, []);

  // Hints are shared: both players read the same per-game history.
  const loadHintHistory = useCallback((gameId: number) => {
    GameApi.getHintHistory(gameId)
      .then((items) => setHintHistory(items))
      .catch((caught: unknown) => console.warn('Could not load hint history:', caught));
  }, []);

  const fetchOnce = useCallback((): Promise<void> => {
    if (inFlightRef.current) return Promise.resolve();
    inFlightRef.current = true;
    return RoomApi.getState(roomCode)
      .then((next) => {
        // Drop out-of-order responses: a poll that was in flight while one of our
        // own moves bumped the version must not roll the board/version back.
        if (next.stateVersion < versionRef.current) return;
        versionRef.current = next.stateVersion;
        stateRef.current = next;
        setRoom(next);
        setLoadError(null);
        setPollFailures(0);
      })
      .catch((caught: unknown) => {
        setPollFailures((failures) => failures + 1);
        if (!stateRef.current) setLoadError(describeError(caught));
      })
      .finally(() => {
        inFlightRef.current = false;
      });
  }, [roomCode]);

  // Initial load + REST polling loop (reset per room).
  useEffect(() => {
    versionRef.current = -1;
    hintHistoryVersionRef.current = -1;
    stateRef.current = null;
    void fetchOnce();
    const id = setInterval(() => void fetchOnce(), POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [fetchOnce]);

  const game = room?.game ?? null;
  const isCompleted = room?.status === 'COMPLETED' || (game?.completed ?? false);
  const connectionLost = pollFailures >= 2;
  const completionPrediction = useCompletionPrediction(
    game,
    hintHistory.length,
    room?.status === 'IN_PROGRESS' && !isCompleted,
  );

  const { formattedTime } = useGameTimer({
    initialSeconds: game?.elapsedSeconds ?? 0,
    status: game?.status ?? 'IN_PROGRESS',
  });

  const gameId = game?.id ?? null;

  useEffect(() => {
    if (gameId == null || room?.stateVersion == null) return;
    if (room.stateVersion > hintHistoryVersionRef.current) {
      hintHistoryVersionRef.current = room.stateVersion;
      loadHintHistory(gameId);
    }
  }, [gameId, room?.stateVersion, loadHintHistory]);

  const numberCounts = useMemo(() => {
    const counts: { [num: number]: number } = {};
    if (!game) return counts;
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        const value = game.board[r]?.[c];
        if (value && value >= 1 && value <= 9) {
          counts[value] = (counts[value] || 0) + 1;
        }
      }
    }
    return counts;
  }, [game]);

  const handleCellClick = useCallback(
    (row: number, col: number) => {
      if (!game || isCompleted || connectionLost) return;
      setSelectedCell({ row, col });
      setErrorCells((prev) => prev.filter((err) => !(err.row === row && err.col === col)));
      setActiveHint(null);
    },
    [game, isCompleted, connectionLost],
  );

  const handleInputNumber = useCallback(
    async (value: number) => {
      if (!room || !room.game || isSending) return;
      if (room.status !== 'IN_PROGRESS' || room.game.completed) return;
      if (!selectedCell) return;
      if (connectionLost) {
        notify('error', 'Connection lost — reconnecting before accepting moves.');
        return;
      }

      const { row, col } = selectedCell;
      if (room.game.initialBoard[row]?.[col] !== 0) {
        notify('info', 'Clue cells are locked.');
        return;
      }

      setIsSending(true);
      try {
        const result = await RoomApi.makeMove(roomCode, row, col, value, versionRef.current);
        versionRef.current = result.stateVersion;

        setRoom((prev) =>
          prev && prev.game
            ? {
                ...prev,
                stateVersion: result.stateVersion,
                game: {
                  ...prev.game,
                  board: result.move.board,
                  mistakes: result.move.mistakes,
                  elapsedSeconds: result.move.elapsedSeconds,
                  completed: result.move.completed || prev.game.completed,
                  status: result.move.completed ? 'COMPLETED' : prev.game.status,
                },
              }
            : prev,
        );

        if (result.move.valid) {
          setErrorCells((prev) => prev.filter((err) => !(err.row === row && err.col === col)));
          if (result.move.completed) {
            notify('success', '🎉 Puzzle solved — checking out the results!');
            void fetchOnce(); // refresh participant points/status
          }
        } else {
          setErrorCells((prev) => [...prev.filter((err) => !(err.row === row && err.col === col)), { row, col }]);
          notify('error', result.move.reason || 'That move is not allowed.');
        }
        setActiveHint(null);
      } catch (caught) {
        if (caught instanceof RoomApiError && caught.status === 409) {
          notify('info', 'Your teammate changed the board — refreshing.');
          void fetchOnce();
        } else {
          notify('error', describeError(caught));
        }
      } finally {
        setIsSending(false);
      }
    },
    [room, selectedCell, isSending, connectionLost, roomCode, notify, fetchOnce],
  );

  const handleErase = useCallback(() => {
    void handleInputNumber(0);
  }, [handleInputNumber]);

  /** Shared hint: applies to the room board and lands in the shared history. */
  const handleHint = useCallback(() => {
    if (!room || isCompleted || connectionLost || isHintPending || room.status !== 'IN_PROGRESS') return;
    setIsHintPending(true);
    RoomApi.requestHint(roomCode, hintLevel)
      .then((hint) => {
        setActiveHint(hint);
        setHintLevel((current) => (current === 3 ? 1 : current + 1));
        if (hint.available && hint.row !== undefined && hint.column !== undefined) {
          setSelectedCell({ row: hint.row, col: hint.column });
          if (gameId != null) loadHintHistory(gameId);
        } else if (!hint.available) {
          notify('info', hint.message || 'No basic hint available.');
        }
      })
      .catch((caught: unknown) => notify('error', describeError(caught)))
      .finally(() => setIsHintPending(false));
  }, [room, isCompleted, connectionLost, isHintPending, roomCode, hintLevel, gameId, loadHintHistory, notify]);

  const handleHintHistory = useCallback(() => {
    if (gameId != null) loadHintHistory(gameId);
    setIsHintHistoryOpen(true);
  }, [gameId, loadHintHistory]);

  const handleAnalysis = useCallback(() => {
    if (gameId == null) return;
    GameApi.getPerformanceAnalysis(gameId)
      .then((result) => {
        setAnalysis(result);
        setIsAnalysisOpen(true);
      })
      .catch((caught: unknown) => {
        notify('error', caught instanceof Error ? caught.message : 'Failed to load performance analysis.');
      });
  }, [gameId, notify]);

  /** Solo-style "Submit Solution": validate the shared board or finish the room. */
  const handleSubmit = useCallback(() => {
    if (isSubmitting || connectionLost || room?.status !== 'IN_PROGRESS') return;
    setIsSubmitting(true);
    RoomApi.submit(roomCode)
      .then((result) => {
        if (result.completed && result.valid) {
          notify('success', 'Sudoku solved successfully!');
          void fetchOnce(); // refresh room status + awarded points
        } else {
          setErrorCells(result.incorrectCells.map((c) => ({ row: c.row, col: c.column })));
          notify('error', result.message || 'The puzzle is incomplete or contains errors.');
        }
      })
      .catch((caught: unknown) => {
        if (caught instanceof RoomApiError && caught.status === 409) {
          notify('info', 'The room state changed — refreshing.');
          void fetchOnce();
        } else {
          notify('error', describeError(caught));
        }
      })
      .finally(() => setIsSubmitting(false));
  }, [isSubmitting, connectionLost, room?.status, roomCode, notify, fetchOnce]);

  const handleLeave = async () => {
    setIsLeaving(true);
    try {
      await RoomApi.leaveRoom(roomCode);
    } catch {
      // Best-effort leave; we navigate home regardless.
    } finally {
      onHome();
    }
  };

  // ---- Loading / error / dead-room states ------------------------------------
  if (!room && !loadError) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="flex flex-col items-center gap-3" role="status" aria-live="polite">
          <Loader2 className="h-9 w-9 animate-spin text-emerald-400" aria-hidden="true" />
          <p className="text-sm font-semibold tracking-wider text-emerald-200">Joining the shared board...</p>
        </div>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="w-full max-w-md rounded-3xl border border-rose-500/30 bg-slate-900/85 p-6 text-center backdrop-blur-xl">
          <AlertCircle size={26} className="mx-auto mb-3 text-rose-300" aria-hidden="true" />
          <p className="text-sm text-rose-200">{loadError ?? 'Unable to load the room.'}</p>
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
              onClick={onHome}
              className="rounded-xl border border-slate-700 bg-slate-950/70 px-4 py-2 text-xs font-bold text-slate-300 transition hover:border-slate-500 cursor-pointer"
            >
              Home
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!game) {
    // Room exists but the shared game has not been created (or the room died).
    const dead = room.status === 'ABANDONED' || room.status === 'EXPIRED';
    return (
      <div className="relative z-10 flex min-h-screen items-center justify-center p-4">
        <div className="w-full max-w-md rounded-3xl border border-slate-700 bg-slate-900/85 p-6 text-center backdrop-blur-xl">
          <PlugZap size={26} className="mx-auto mb-3 text-slate-400" aria-hidden="true" />
          <h1 className="text-xl font-extrabold text-white">{dead ? 'Room closed' : 'Game not started yet'}</h1>
          <p className="mt-2 text-sm text-slate-400">
            {dead ? 'This room is no longer active.' : 'Waiting for the host to start the game.'}
          </p>
          <button
            type="button"
            onClick={onHome}
            className="mt-5 rounded-xl bg-gradient-to-r from-emerald-400 to-teal-400 px-5 py-2.5 text-sm font-bold text-slate-950 transition hover:brightness-110 cursor-pointer"
          >
            Back to multiplayer
          </button>
        </div>
      </div>
    );
  }

  const activePlayers = room.players.filter((player) => player.active);
  const hostPlayer = activePlayers.find((player) => player.role === 'HOST') ?? null;
  const guestPlayer = activePlayers.find((player) => player.role === 'GUEST') ?? null;

  return (
    <div className="relative z-10 mx-auto flex w-full max-w-[560px] flex-col gap-3 px-3 py-4 pb-10">
      {/* Top bar */}
      <header className="flex items-center justify-between gap-2 rounded-2xl bg-slate-900/70 border border-emerald-500/20 backdrop-blur-md p-2.5">
        <button
          type="button"
          onClick={handleLeave}
          disabled={isLeaving}
          title="Leave room"
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-slate-300 hover:text-white border border-slate-700/50 transition-all active:scale-95 text-xs font-medium cursor-pointer disabled:opacity-60"
        >
          <ArrowLeft className="w-3.5 h-3.5 text-emerald-400" aria-hidden="true" />
          <span className="hidden sm:inline">{isLeaving ? 'Leaving...' : 'Leave'}</span>
        </button>

        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-emerald-950/60 border border-emerald-500/30 text-emerald-300 text-xs font-mono font-semibold tracking-widest">
            {room.roomCode}
          </span>
          <span className="px-2.5 py-1 rounded-lg bg-cyan-950/60 border border-cyan-500/30 text-cyan-300 text-xs font-medium">
            {room.difficulty}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs font-medium">
            Mistakes: {game.mistakes}
          </span>
          <span className="flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-950/80 border border-slate-700/60 font-mono text-cyan-200 text-xs font-semibold">
            <Clock className="w-3.5 h-3.5" aria-hidden="true" />
            {formattedTime}
          </span>
        </div>
      </header>

      {/* Both players + shared status */}
      <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-slate-700/70 bg-slate-950/50 px-3 py-2">
        <div className="flex flex-wrap items-center gap-2">
          <PlayerPill player={hostPlayer} isYou={room.you.role === 'HOST'} />
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">shares the board with</span>
          <PlayerPill player={guestPlayer} isYou={room.you.role === 'GUEST'} />
        </div>
        <span
          className={`rounded-lg px-2 py-1 text-[10px] font-bold uppercase tracking-wider ${
            room.status === 'IN_PROGRESS'
              ? 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-300'
              : 'bg-slate-800 border border-slate-600 text-slate-300'
          }`}
        >
          {room.status === 'IN_PROGRESS' ? 'Game in progress' : room.status}
        </span>
      </div>

      {/* Connection banner */}
      {connectionLost && (
        <div role="alert" className="flex items-center gap-2 rounded-xl border border-amber-500/40 bg-amber-950/40 px-3 py-2 text-xs text-amber-200">
          <WifiOff size={14} aria-hidden="true" />
          Connection lost — retrying. Moves are paused until the link recovers.
        </div>
      )}

      {/* Notification */}
      {notification && (
        <div
          role={notification.type === 'error' ? 'alert' : 'status'}
          className={`rounded-xl border px-3 py-2 text-xs font-medium ${
            notification.type === 'error'
              ? 'border-rose-500/40 bg-rose-950/40 text-rose-200'
              : notification.type === 'success'
                ? 'border-emerald-500/40 bg-emerald-950/40 text-emerald-200'
                : 'border-cyan-500/40 bg-cyan-950/40 text-cyan-200'
          }`}
        >
          {notification.message}
        </div>
      )}

      {/* Shared board */}
      <SudokuBoard
        board={game.board}
        initialBoard={game.initialBoard}
        selectedCell={selectedCell}
        errorCells={errorCells}
        onCellClick={handleCellClick}
        isPaused={false}
      />

      {/* Room controls — solo-page parity (Hint / History / Analysis / Submit).
          Undo, Redo, Pause and Restart are intentionally omitted: they have no
          safe meaning on a board two players share. */}
      <div className="flex w-full flex-col gap-3 rounded-2xl border border-cyan-500/20 bg-slate-900/50 p-3 backdrop-blur-md sm:p-4">
        <div className="flex flex-row gap-2">
          {/* Hint (shared; penalizes both scores) */}
          <button
            type="button"
            disabled={isCompleted || connectionLost || isHintPending || room.status !== 'IN_PROGRESS'}
            onClick={handleHint}
            title="Get a shared hint for the board"
            className="flex-1 flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/50 shadow-[0_0_15px_rgba(245,158,11,0.2)] hover:shadow-[0_0_20px_rgba(245,158,11,0.3)] transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer tracking-wider"
          >
            <span>{isHintPending ? 'Getting hint...' : '💡 Hint'}</span>
          </button>

          {/* Shared hint history */}
          <button
            type="button"
            onClick={handleHintHistory}
            title="View hints used in this game"
            className="flex-1 flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-bold bg-slate-800/80 hover:bg-slate-700/80 text-cyan-300 border border-cyan-500/30 shadow-[0_0_15px_rgba(6,182,212,0.15)] transition-all active:scale-95 cursor-pointer tracking-wider"
          >
            <History className="w-4 h-4 shrink-0" />
            <span>History</span>
          </button>

          {/* Performance analysis */}
          <button
            type="button"
            onClick={handleAnalysis}
            title="View performance analysis"
            className="flex items-center justify-center gap-1.5 rounded-xl border border-emerald-500/30 bg-slate-800/80 px-3 py-2.5 text-xs font-bold tracking-wider text-emerald-300 transition-all hover:bg-slate-700/80 cursor-pointer"
          >
            <BarChart3 className="h-4 w-4" />
            <span className="hidden sm:inline">Analysis</span>
          </button>
        </div>

        {/* Active hint */}
        {activeHint && activeHint.available && (
          <div className="bg-amber-950/40 border border-amber-500/40 p-3 rounded-xl flex flex-col gap-1 text-xs">
            <div className="flex justify-between items-center mb-1">
              <span className="font-bold text-amber-400">💡 Hint Level {activeHint.level || 3}</span>
              <button
                type="button"
                onClick={() => setActiveHint(null)}
                className="text-amber-200/60 hover:text-amber-200 cursor-pointer"
              >
                Close
              </button>
            </div>
            <p className="text-amber-100">
              <span className="text-amber-300 font-semibold">{activeHint.technique?.replace('_', ' ')}:</span>{' '}
              {activeHint.hintText || activeHint.explanation}
            </p>
            <p className="text-amber-200/80 mt-1">
              Cell R{activeHint.row! + 1}C{activeHint.column! + 1} &rarr; Value: {activeHint.value}
            </p>
          </div>
        )}

        {/* Submit Solution */}
        <button
          type="button"
          disabled={isSubmitting || connectionLost || isCompleted || room.status !== 'IN_PROGRESS'}
          onClick={handleSubmit}
          className="w-full flex items-center justify-center gap-2 py-2.5 sm:py-3 px-4 rounded-xl text-xs sm:text-sm font-bold bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 shadow-[0_0_18px_rgba(6,182,212,0.4)] hover:shadow-[0_0_24px_rgba(6,182,212,0.6)] transition-all active:scale-95 disabled:opacity-35 disabled:cursor-not-allowed cursor-pointer uppercase tracking-wider"
        >
          <CheckCircle2 className="w-4 h-4 sm:w-5 sm:h-5 text-slate-950 shrink-0" />
          <span>{isSubmitting ? 'Checking...' : 'Submit Solution'}</span>
        </button>
      </div>

      <CompletionPredictionCard {...completionPrediction} />

      {/* Results or number pad */}
      {isCompleted ? (
        <div className="rounded-2xl border border-amber-400/40 bg-slate-900/80 p-5 text-center animate-fadeIn">
          <Trophy size={26} className="mx-auto mb-2 text-amber-300" aria-hidden="true" />
          <h2 className="text-xl font-extrabold tracking-wider text-white">Puzzle solved together!</h2>
          <p className="mt-1 text-xs text-slate-400">
            Shared result: {game.mistakes} mistake{game.mistakes === 1 ? '' : 's'} in {formattedTime}
          </p>
          <div className="mt-4 grid grid-cols-2 gap-3">
            {room.players.map((player) => (
              <div
                key={player.userId}
                className="rounded-xl border border-slate-700 bg-slate-950/60 px-3 py-2.5 text-left"
              >
                <p className="truncate text-sm font-bold text-white">
                  {player.displayName}
                  {player.userId === room.you.userId && <span className="ml-1 text-[10px] text-cyan-300">(you)</span>}
                </p>
                <p className="font-mono text-xs text-amber-300">
                  {player.pointsAwarded != null ? `${player.pointsAwarded} pts` : 'Awarding points...'}
                </p>
                <p className="text-[10px] text-slate-500">
                  {player.finishedAt ? 'Finished' : player.active ? 'Did not finish' : 'Left before the finish'}
                </p>
              </div>
            ))}
          </div>
          <button
            type="button"
            onClick={onHome}
            className="mt-4 w-full rounded-xl bg-gradient-to-r from-amber-300 to-yellow-500 px-5 py-3 text-sm font-extrabold text-slate-950 transition hover:brightness-110 cursor-pointer"
          >
            Back to mission control
          </button>
        </div>
      ) : (
        <NumberPad onNumberSelect={handleInputNumber} onErase={handleErase} disabled={!selectedCell || isSending || connectionLost} numberCounts={numberCounts} />
      )}

      {/* Modals (parity with the solo game page) */}
      <HintHistoryModal
        isOpen={isHintHistoryOpen}
        onClose={() => setIsHintHistoryOpen(false)}
        hints={hintHistory}
      />

      <PerformanceAnalysisModal
        isOpen={isAnalysisOpen}
        analysis={analysis}
        onClose={() => setIsAnalysisOpen(false)}
      />
    </div>
  );
};

export default MultiplayerGamePage;
