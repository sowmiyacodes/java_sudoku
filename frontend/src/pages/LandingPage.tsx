import React, { useEffect, useState } from 'react';
import { Play, RotateCw, Sparkles, Compass } from 'lucide-react';
import type { Difficulty, GameState } from '../types/sudoku';
import { GameApi } from '../services/gameApi';

interface LandingPageProps {
  onStartNewGame: (difficulty: Difficulty) => void;
  onContinueGame: (game: GameState) => void;
}

export const LandingPage: React.FC<LandingPageProps> = ({ onStartNewGame, onContinueGame }) => {
  const [selectedDifficulty, setSelectedDifficulty] = useState<Difficulty>('Medium');
  const [activeGame, setActiveGame] = useState<GameState | null>(null);
  const [isLoadingActive, setIsLoadingActive] = useState<boolean>(true);
  const [isStarting, setIsStarting] = useState<boolean>(false);

  useEffect(() => {
    // Check if there is an active unfinished game in the database
    let isMounted = true;
    GameApi.getActiveGame()
      .then((game) => {
        if (isMounted) {
          setActiveGame(game);
          setIsLoadingActive(false);
        }
      })
      .catch((err) => {
        console.warn('Could not check for active game:', err);
        if (isMounted) {
          setIsLoadingActive(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const handleNewGame = () => {
    setIsStarting(true);
    onStartNewGame(selectedDifficulty);
  };

  const handleContinue = () => {
    if (activeGame) {
      onContinueGame(activeGame);
    }
  };

  const formatElapsed = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const s = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="relative min-h-screen flex flex-col items-center justify-center p-4 z-10 select-none">
      {/* Cosmic Center Container */}
      <div className="w-full max-w-md p-6 sm:p-10 rounded-3xl bg-slate-900/80 backdrop-blur-xl border border-cyan-500/30 shadow-[0_0_60px_rgba(6,182,212,0.2)] text-center animate-fadeIn">
        {/* Constellation Star Emblem */}
        <div className="relative inline-flex items-center justify-center w-20 h-20 mb-6 rounded-3xl bg-cyan-950/60 border border-cyan-400/40 shadow-[0_0_30px_rgba(6,182,212,0.3)]">
          <Compass className="w-10 h-10 text-cyan-400 animate-spin-slow" />
          <div className="absolute -top-1.5 -right-1.5 w-4 h-4 rounded-full bg-cyan-400 shadow-[0_0_10px_#22d3ee] animate-ping" />
        </div>

        {/* Title */}
        <div className="mb-2">
          <span className="text-xs font-mono tracking-[0.3em] uppercase text-cyan-400/80">
            ✦ DEEP SPACE ✦
          </span>
          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-[0.25em] text-transparent bg-clip-text bg-gradient-to-b from-white via-cyan-100 to-sky-300 drop-shadow-[0_0_20px_rgba(56,189,248,0.4)] mt-1">
            S U D O K U
          </h1>
        </div>

        {/* Difficulty Selection Buttons */}
        <div className="mb-6 text-left">
          <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
            Select Orbit Difficulty
          </label>
          <div className="grid grid-cols-4 gap-1.5 sm:gap-2">
            {(['Easy', 'Medium', 'Hard', 'Expert'] as Difficulty[]).map((diff) => {
              const isSelected = selectedDifficulty === diff;
              return (
                <button
                  key={diff}
                  type="button"
                  onClick={() => setSelectedDifficulty(diff)}
                  className={`py-2 px-1.5 sm:px-2 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer ${
                    isSelected
                      ? 'bg-cyan-500 text-slate-950 shadow-[0_0_20px_rgba(6,182,212,0.6)] scale-102 border-transparent'
                      : 'bg-slate-800/70 text-slate-300 hover:text-white border border-slate-700/60 hover:border-cyan-500/30'
                  }`}
                >
                  {diff}
                </button>
              );
            })}
          </div>
          <div className="mt-2.5 text-xs text-cyan-300/80 h-5 text-center transition-all">
            {selectedDifficulty === 'Easy' && 'Beginner-friendly • Lower logical complexity'}
            {selectedDifficulty === 'Medium' && 'Balanced challenge • Requires candidate reasoning'}
            {selectedDifficulty === 'Hard' && 'Advanced challenge • Requires deeper deduction'}
            {selectedDifficulty === 'Expert' && 'Master level • Complex branching & minimal clues'}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col gap-3.5">
          {/* New Game Button */}
          <button
            type="button"
            disabled={isStarting}
            onClick={handleNewGame}
            className="w-full flex items-center justify-center gap-2.5 py-4 px-6 rounded-2xl font-extrabold text-base tracking-wider text-slate-950 bg-gradient-to-r from-cyan-400 via-sky-300 to-blue-500 hover:from-cyan-300 hover:to-blue-400 shadow-[0_0_30px_rgba(6,182,212,0.45)] hover:shadow-[0_0_40px_rgba(6,182,212,0.65)] transform hover:scale-[1.02] active:scale-[0.98] transition-all cursor-pointer disabled:opacity-75 disabled:scale-100"
          >
            <Play className="w-5 h-5 fill-slate-950" />
            <span>{isStarting ? 'GENERATING PUZZLE...' : 'START NEW MISSION'}</span>
          </button>

          {/* Continue Game Section */}
          {activeGame ? (
            <div className="p-3.5 rounded-2xl bg-cyan-950/40 border border-cyan-500/40 text-left animate-fadeIn">
              <div className="flex items-center justify-between text-xs text-cyan-300 mb-2">
                <span className="font-semibold flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
                  Unfinished Game Found
                </span>
                <span className="font-mono text-slate-300">
                  {formatElapsed(activeGame.elapsedSeconds)}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-[11px] text-slate-400">
                  {activeGame.difficulty} • Mistakes: {activeGame.mistakes}
                </span>
                <button
                  type="button"
                  onClick={handleContinue}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-200 border border-cyan-400/50 transition-all active:scale-95 cursor-pointer"
                >
                  <RotateCw className="w-3.5 h-3.5" />
                  <span>Resume</span>
                </button>
              </div>
            </div>
          ) : (
            <button
              type="button"
              disabled={true}
              className="w-full flex items-center justify-center gap-2 py-3.5 px-6 rounded-2xl font-semibold text-xs text-slate-500 bg-slate-800/30 border border-slate-800 cursor-not-allowed opacity-60"
            >
              <RotateCw className="w-4 h-4 text-slate-600" />
              <span>{isLoadingActive ? 'Scanning orbit...' : "There's no game to continue"}</span>
            </button>
          )}
        </div>

        {/* Footer info */}
        <div className="mt-8 pt-4 border-t border-white/5 flex items-center justify-center gap-4 text-[11px] text-slate-500">
          <span>Spring Boot + React</span>
        </div>
      </div>
    </div>
  );
};
