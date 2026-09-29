import React, { useState, useEffect, useCallback, useMemo } from 'react';
import type { CellCoordinate, Difficulty, GameState, HintHistoryItem, PerformanceAnalysis } from '../types/sudoku';
import { GameApi } from '../services/gameApi';
import { useGameTimer } from '../hooks/useGameTimer';
import { GameHeader } from '../components/GameHeader';
import { SudokuBoard } from '../components/SudokuBoard';
import { NumberPad } from '../components/NumberPad';
import { GameControls } from '../components/GameControls';
import { PauseOverlay } from '../components/PauseOverlay';
import { CompletionModal } from '../components/CompletionModal';
import { HintHistoryModal } from '../components/HintHistoryModal';
import { PerformanceAnalysisModal } from '../components/PerformanceAnalysisModal';
import { AlertTriangle, CheckCircle, Info } from 'lucide-react';

interface GamePageProps {
  initialGame: GameState;
  onHome: () => void;
  onNewGameRequest: (difficulty: Difficulty) => void;
}

export const GamePage: React.FC<GamePageProps> = ({
  initialGame,
  onHome,
  onNewGameRequest,
}) => {
  const [game, setGame] = useState<GameState>(initialGame);
  const [selectedCell, setSelectedCell] = useState<CellCoordinate | null>(null);
  const [errorCells, setErrorCells] = useState<CellCoordinate[]>([]);
  const [notification, setNotification] = useState<{ type: 'error' | 'success' | 'info'; message: string } | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [activeHint, setActiveHint] = useState<import('../types/sudoku').HintResponse | null>(null);
  const [isHintHistoryOpen, setIsHintHistoryOpen] = useState<boolean>(false);
  const [hintHistory, setHintHistory] = useState<HintHistoryItem[]>([]);
  const [hintLevel, setHintLevel] = useState(1);
  const [analysis, setAnalysis] = useState<PerformanceAnalysis | null>(null);
  const [isAnalysisOpen, setIsAnalysisOpen] = useState(false);

  const loadHintHistory = useCallback(async (gameId: number) => {
    try {
      const items = await GameApi.getHintHistory(gameId);
      setHintHistory(items);
    } catch (e) {
      console.warn('Could not load hint history:', e);
    }
  }, []);

  useEffect(() => {
    loadHintHistory(game.id);
  }, [game.id, loadHintHistory]);

  const { formattedTime } = useGameTimer({
    initialSeconds: game.elapsedSeconds,
    status: game.status,
  });

  // Calculate number counts (how many times each number 1-9 is placed on the board)
  const numberCounts = useMemo(() => {
    const counts: { [num: number]: number } = {};
    for (let r = 0; r < 9; r++) {
      for (let c = 0; c < 9; c++) {
        const val = game.board[r]?.[c];
        if (val && val >= 1 && val <= 9) {
          counts[val] = (counts[val] || 0) + 1;
        }
      }
    }
    return counts;
  }, [game.board]);

  // Set notification with auto-clear
  const showNotification = (type: 'error' | 'success' | 'info', message: string, duration = 3500) => {
    setNotification({ type, message });
    setTimeout(() => {
      setNotification((prev) => (prev?.message === message ? null : prev));
    }, duration);
  };

  // Handle cell click
  const handleCellClick = (row: number, col: number) => {
    if (game.status === 'PAUSED' || game.status === 'COMPLETED') return;
    setSelectedCell({ row, col });
    // Clear targeted error cell highlight on interaction
    setErrorCells((prev) => prev.filter((err) => !(err.row === row && err.col === col)));
    setActiveHint(null);
  };

  // Submit a number move to backend
  const handleInputNumber = useCallback(
    async (value: number) => {
      if (!selectedCell || game.status === 'PAUSED' || game.status === 'COMPLETED') return;

      const { row, col } = selectedCell;

      // Check if original fixed cell
      if (game.initialBoard[row]?.[col] !== 0) {
        showNotification('info', 'Original clue cells cannot be altered.');
        return;
      }

      try {
        const response = await GameApi.makeMove(game.id, row, col, value);

        if (!response.valid) {
          showNotification('error', response.reason || 'Invalid move for this position.');
          setErrorCells((prev) => [...prev.filter((e) => !(e.row === row && e.col === col)), { row, col }]);
          setGame((prev) => ({
            ...prev,
            mistakes: response.mistakes,
            canUndo: response.canUndo,
            canRedo: response.canRedo,
          }));
        } else {
          setGame((prev) => ({
            ...prev,
            board: response.board,
            mistakes: response.mistakes,
            status: response.completed ? 'COMPLETED' : prev.status,
            completed: response.completed,
            canUndo: response.canUndo,
            canRedo: response.canRedo,
          }));
          setErrorCells((prev) => prev.filter((e) => !(e.row === row && e.col === col)));

          if (response.completed) {
            showNotification('success', '🎉 Puzzle successfully completed!');
          }
        }
        
        setActiveHint(null);
      } catch (err: any) {
        showNotification('error', err.message || 'Error processing move.');
      }
    },
    [selectedCell, game.id, game.status, game.initialBoard]
  );

  const handleErase = useCallback(() => {
    handleInputNumber(0);
  }, [handleInputNumber]);

  // Undo Move
  const handleUndo = async () => {
    if (!game.canUndo || game.status === 'PAUSED' || game.status === 'COMPLETED') return;
    try {
      const updatedGame = await GameApi.undo(game.id);
      setGame(updatedGame);
      setErrorCells([]);
      setActiveHint(null);
      showNotification('info', 'Move undone.');
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to undo move.');
    }
  };

  // Redo Move
  const handleRedo = async () => {
    if (!game.canRedo || game.status === 'PAUSED' || game.status === 'COMPLETED') return;
    try {
      const updatedGame = await GameApi.redo(game.id);
      setGame(updatedGame);
      setErrorCells([]);
      setActiveHint(null);
      showNotification('info', 'Move redone.');
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to redo move.');
    }
  };

  // Pause / Resume Toggle
  const handlePauseToggle = async () => {
    if (game.status === 'COMPLETED') return;

    try {
      if (game.status === 'PAUSED') {
        const resumed = await GameApi.resume(game.id);
        setGame(resumed);
      } else {
        const paused = await GameApi.pause(game.id);
        setGame(paused);
      }
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to update pause state.');
    }
  };

  // Restart Puzzle
  const handleRestart = async () => {
    try {
      const restartedGame = await GameApi.restart(game.id);
      setGame(restartedGame);
      setSelectedCell(null);
      setErrorCells([]);
      setActiveHint(null);
      showNotification('info', 'Puzzle restarted.');
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to restart puzzle.');
    }
  };

  // Hint Request
  const handleHint = async () => {
    if (game.status === 'PAUSED' || game.status === 'COMPLETED') return;
    try {
      const hint = await GameApi.getHint(game.id, hintLevel);
      setActiveHint(hint);
      setHintLevel((current) => current === 3 ? 1 : current + 1);
      if (hint.available && hint.row !== undefined && hint.column !== undefined) {
        setSelectedCell({ row: hint.row, col: hint.column });
        loadHintHistory(game.id);
      } else if (!hint.available) {
        showNotification('info', hint.message || 'No basic hint available.');
      }
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to get hint.');
    }
  };

  const handleAnalysis = async () => {
    try {
      setAnalysis(await GameApi.getPerformanceAnalysis(game.id));
      setIsAnalysisOpen(true);
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to load performance analysis.');
    }
  };

  // Submit Entire Board
  const handleSubmit = async () => {
    if (game.status === 'PAUSED' || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const res = await GameApi.submit(game.id);
      if (res.completed && res.valid) {
        setGame((prev) => ({ ...prev, status: 'COMPLETED', completed: true }));
        showNotification('success', 'Sudoku solved successfully!');
      } else {
        setErrorCells(res.incorrectCells.map((c) => ({ row: c.row, col: c.column })));
        showNotification('error', res.message || 'The puzzle is incomplete or contains errors.');
      }
    } catch (err: any) {
      showNotification('error', err.message || 'Failed to submit solution.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Keyboard navigation & number input listener
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if inside an input field
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement).tagName)) return;

      if (e.key >= '1' && e.key <= '9') {
        e.preventDefault();
        handleInputNumber(parseInt(e.key, 10));
      } else if (e.key === 'Backspace' || e.key === 'Delete' || e.key === '0') {
        e.preventDefault();
        handleErase();
      } else if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'z') {
        e.preventDefault();
        if (e.shiftKey) {
          handleRedo();
        } else {
          handleUndo();
        }
      } else if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'y') {
        e.preventDefault();
        handleRedo();
      } else if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) {
        e.preventDefault();
        setSelectedCell((prev) => {
          if (!prev) return { row: 4, col: 4 };
          let newRow = prev.row;
          let newCol = prev.col;
          if (e.key === 'ArrowUp') newRow = Math.max(0, prev.row - 1);
          if (e.key === 'ArrowDown') newRow = Math.min(8, prev.row + 1);
          if (e.key === 'ArrowLeft') newCol = Math.max(0, prev.col - 1);
          if (e.key === 'ArrowRight') newCol = Math.min(8, prev.col + 1);
          return { row: newRow, col: newCol };
        });
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleInputNumber, handleErase, handleUndo, handleRedo]);

  return (
    <div className="relative min-h-screen flex flex-col justify-between items-center py-4 px-3 sm:px-4 z-10 select-none">
      {/* Top Header */}
      <div className="w-full flex flex-col items-center">
        <GameHeader
          difficulty={game.difficulty}
          mistakes={game.mistakes}
          formattedTime={formattedTime}
          status={game.status}
          onPauseToggle={handlePauseToggle}
          onHomeClick={onHome}
        />

        {/* Live Notification Bar */}
        {notification && (
          <div
            className={`w-full max-w-[500px] mb-3 px-3.5 py-2 rounded-xl flex items-center gap-2 text-xs font-medium backdrop-blur-md transition-all animate-fadeIn ${
              notification.type === 'error'
                ? 'bg-rose-950/80 text-rose-200 border border-rose-500/40 shadow-[0_0_15px_rgba(244,63,94,0.3)]'
                : notification.type === 'success'
                ? 'bg-emerald-950/80 text-emerald-200 border border-emerald-500/40 shadow-[0_0_15px_rgba(16,185,129,0.3)]'
                : 'bg-cyan-950/80 text-cyan-200 border border-cyan-500/40'
            }`}
          >
            {notification.type === 'error' && <AlertTriangle className="w-4 h-4 text-rose-400 shrink-0" />}
            {notification.type === 'success' && <CheckCircle className="w-4 h-4 text-emerald-400 shrink-0" />}
            {notification.type === 'info' && <Info className="w-4 h-4 text-cyan-400 shrink-0" />}
            <span>{notification.message}</span>
          </div>
        )}
      </div>

      {/* Main Gameplay Container */}
      <main className="w-full flex flex-col lg:flex-row items-center lg:items-center justify-center gap-6 lg:gap-10 max-w-6xl mx-auto my-auto py-2">
        {/* Left Side: Sudoku Board */}
        <div className="w-full max-w-[500px] shrink-0">
          <SudokuBoard
            board={game.board}
            initialBoard={game.initialBoard}
            selectedCell={selectedCell}
            errorCells={errorCells}
            onCellClick={handleCellClick}
            isPaused={game.status === 'PAUSED'}
          />
        </div>

        {/* Right Side Panel: Controls & Number Pad (Wider Screens) / Below Grid on Mobile */}
        <div className="w-full max-w-[500px] lg:max-w-[340px] flex flex-col gap-4 bg-slate-900/40 lg:bg-slate-900/60 backdrop-blur-md p-4 sm:p-5 rounded-2xl border border-cyan-500/20 shadow-[0_0_30px_rgba(6,182,212,0.1)]">
          {/* Action Controls (Undo, Redo, Erase, Restart & Submit) */}
          <GameControls
            canUndo={game.canUndo}
            canRedo={game.canRedo}
            onUndo={handleUndo}
            onRedo={handleRedo}
            onErase={handleErase}
            onRestart={handleRestart}
            onSubmit={handleSubmit}
            onHint={handleHint}
            onHintHistory={() => setIsHintHistoryOpen(true)}
            onAnalysis={handleAnalysis}
            isPaused={game.status === 'PAUSED'}
            isCompleted={game.status === 'COMPLETED'}
          />
          
          {/* Active Hint UI */}
          {activeHint && activeHint.available && (
            <div className="bg-amber-950/40 border border-amber-500/40 p-3 rounded-xl flex flex-col gap-1 text-xs">
              <div className="flex justify-between items-center mb-1">
                <span className="font-bold text-amber-400">💡 Hint Level {activeHint.level || 3}</span>
                <button 
                  onClick={() => setActiveHint(null)}
                  className="text-amber-200/60 hover:text-amber-200"
                >
                  Close
                </button>
              </div>
              <p className="text-amber-100">
                <span className="text-amber-300 font-semibold">{activeHint.technique?.replace('_', ' ')}:</span> {activeHint.hintText || activeHint.explanation}
              </p>
              <p className="text-amber-200/80 mt-1">
                Cell R{activeHint.row! + 1}C{activeHint.column! + 1} &rarr; Value: {activeHint.value}
              </p>
            </div>
          )}

          {/* 1-9 Number Pad */}
          <NumberPad
            onNumberSelect={handleInputNumber}
            onErase={handleErase}
            disabled={game.status === 'PAUSED' || game.status === 'COMPLETED'}
            numberCounts={numberCounts}
          />
        </div>
      </main>

      {/* Modals */}
      <PauseOverlay
        isOpen={game.status === 'PAUSED'}
        formattedTime={formattedTime}
        onResume={handlePauseToggle}
      />

      <CompletionModal
        isOpen={game.status === 'COMPLETED'}
        formattedTime={formattedTime}
        mistakes={game.mistakes}
        difficulty={game.difficulty}
        onNewGame={() => onNewGameRequest(game.difficulty)}
        onHome={onHome}
      />

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
