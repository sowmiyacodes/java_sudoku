import React, { useState } from 'react';
import type { Difficulty, GameState } from './types/sudoku';
import { GameApi } from './services/gameApi';
import { StarBackground } from './components/StarBackground';
import { LandingPage } from './pages/LandingPage';
import { GamePage } from './pages/GamePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ProfilePage } from './pages/ProfilePage';
import { LeaderboardPage } from './pages/LeaderboardPage';
import { PublicProfilePage } from './pages/PublicProfilePage';
import { MultiplayerLandingPage } from './pages/MultiplayerLandingPage';
import { RoomWaitingPage } from './pages/RoomWaitingPage';
import { MultiplayerGamePage } from './pages/MultiplayerGamePage';
import { PlayerDashboardPage } from './pages/PlayerDashboardPage';
import { GameHistoryPage } from './pages/GameHistoryPage';
import { StatisticsPage } from './pages/StatisticsPage';
import { AdminMLDashboardPage } from './pages/AdminMLDashboardPage';
import { PuzzleManagementPage } from './pages/PuzzleManagementPage';
import { PlayerSidebar, type DashboardView } from './components/PlayerSidebar';
import { useAuth } from './context/useAuth';
import { Loader2, LogIn, Trophy, UserRound, Users, LayoutDashboard } from 'lucide-react';

type AppView =
  | 'LANDING'
  | 'GAME'
  | 'REGISTER'
  | 'LOGIN'
  | 'PROFILE'
  | 'LEADERBOARD'
  | 'PLAYER_PROFILE'
  | 'MULTIPLAYER'
  | 'ROOM_WAITING'
  | 'ROOM_GAME'
  | 'DASHBOARD'
  | 'ADMIN_ML'
  | 'GAME_HISTORY'
  | 'STATISTICS'
  | 'PUZZLE_MANAGEMENT';

export const App: React.FC = () => {
  const { user, isLoading: isAuthLoading, logout } = useAuth();
  const [view, setView] = useState<AppView>('LANDING');
  const [currentGame, setCurrentGame] = useState<GameState | null>(null);
  const [profileUsername, setProfileUsername] = useState<string | null>(null);
  const [activeRoomCode, setActiveRoomCode] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleStartNewGame = async (difficulty: Difficulty) => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const newGame = await GameApi.createGame({ difficulty });
      setCurrentGame(newGame);
      setView('GAME');
    } catch (err: any) {
      console.error('Error starting new game:', err);
      setErrorMessage(err.message || 'Unable to connect to game server. Ensure backend is running.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleContinueGame = (game: GameState) => {
    setCurrentGame(game);
    setView('GAME');
  };

  const handleReturnToLanding = () => {
    setCurrentGame(null);
    setView('LANDING');
  };

  const isAnalyticsView = ['DASHBOARD', 'PROFILE', 'GAME_HISTORY', 'STATISTICS', 'ADMIN_ML', 'PUZZLE_MANAGEMENT'].includes(view);

  const mapToDashboardView = (v: AppView): DashboardView => {
    if (v === 'ADMIN_ML') return 'ADMIN_ML';
    if (v === 'PUZZLE_MANAGEMENT') return 'PUZZLES';
    if (v === 'GAME_HISTORY') return 'GAMES';
    if (v === 'STATISTICS') return 'STATISTICS';
    if (v === 'PROFILE') return 'PROFILE';
    return 'DASHBOARD';
  };

  const handleSidebarNavigate = (dest: DashboardView) => {
    if (dest === 'DASHBOARD') setView('DASHBOARD');
    else if (dest === 'ADMIN_ML') setView('ADMIN_ML');
    else if (dest === 'PUZZLES') setView('PUZZLE_MANAGEMENT');
    else if (dest === 'PROFILE') setView('PROFILE');
    else if (dest === 'GAMES') setView('GAME_HISTORY');
    else if (dest === 'STATISTICS') setView('STATISTICS');
    else if (dest === 'RECOMMENDATIONS') setView('DASHBOARD');
    else if (dest === 'LEADERBOARD') setView('LEADERBOARD');
  };

  return (
    <div className="relative min-h-screen bg-[#02040a] text-slate-100 font-sans flex flex-col justify-center selection:bg-cyan-500/30 selection:text-cyan-200">
      {/* Background Subtle Moving Starfield Canvas */}
      <StarBackground />

      {view === 'LANDING' && !isAuthLoading && (
        <nav aria-label="Account" className="absolute right-4 top-4 z-20 flex items-center gap-2 sm:right-7 sm:top-6">
          <button
            type="button"
            onClick={() => setView('MULTIPLAYER')}
            className="inline-flex items-center gap-2 rounded-xl border border-emerald-400/40 bg-slate-950/70 px-3 py-2 text-sm font-semibold text-emerald-100 backdrop-blur transition hover:border-emerald-300/70 hover:bg-emerald-950/50 cursor-pointer"
          >
            <Users size={16} aria-hidden="true" />
            <span className="hidden sm:inline">Multiplayer</span>
          </button>
          <button
            type="button"
            onClick={() => setView('LEADERBOARD')}
            className="inline-flex items-center gap-2 rounded-xl border border-amber-400/40 bg-slate-950/70 px-3 py-2 text-sm font-semibold text-amber-100 backdrop-blur transition hover:border-amber-300/70 hover:bg-amber-950/50 cursor-pointer"
          >
            <Trophy size={16} aria-hidden="true" />
            <span className="hidden sm:inline">Leaderboard</span>
          </button>

          {user ? (
            <>
              <button
                type="button"
                onClick={() => setView('DASHBOARD')}
                className="inline-flex items-center gap-2 rounded-xl border border-cyan-500/40 bg-cyan-950/40 px-3 py-2 text-sm font-bold text-cyan-200 backdrop-blur transition hover:border-cyan-300 hover:bg-cyan-900/50 cursor-pointer shadow-sm"
              >
                <LayoutDashboard size={16} aria-hidden="true" />
                <span>Dashboard</span>
              </button>
              <button
                type="button"
                onClick={() => setView('PROFILE')}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950/70 px-3 py-2 text-sm font-semibold text-slate-200 backdrop-blur transition hover:border-cyan-300/60 hover:text-cyan-100 cursor-pointer"
              >
                <UserRound size={16} aria-hidden="true" />
                <span className="max-w-32 truncate">{user.displayName}</span>
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={() => setView('LOGIN')}
                className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-950/70 px-3 py-2 text-sm font-semibold text-slate-200 transition hover:border-cyan-400/50 hover:text-cyan-100 cursor-pointer"
              >
                <LogIn size={16} aria-hidden="true" /> Log in
              </button>
              <button
                type="button"
                onClick={() => setView('REGISTER')}
                className="rounded-xl bg-cyan-400 px-3 py-2 text-sm font-bold text-slate-950 transition hover:bg-cyan-300 cursor-pointer"
              >
                Register
              </button>
            </>
          )}
        </nav>
      )}

      {/* Global Error Banner */}
      {errorMessage && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 max-w-md w-full px-4">
          <div className="p-3 rounded-2xl bg-rose-950/90 border border-rose-500/50 text-rose-200 text-xs font-medium text-center shadow-lg backdrop-blur-md">
            {errorMessage}
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="ml-2 text-rose-400 hover:text-white underline cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}

      {/* Loading Overlay */}
      {isLoading && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex flex-col items-center justify-center">
          <Loader2 className="w-10 h-10 text-cyan-400 animate-spin mb-3" />
          <p className="text-sm font-semibold tracking-wider text-cyan-200">Initializing Cosmic Grid...</p>
        </div>
      )}

      {/* Analytics Suite Layout (Dashboard, History, Stats) */}
      {isAnalyticsView && user && (
        <div className="relative z-10 flex flex-col md:flex-row min-h-screen w-full">
          <PlayerSidebar
            currentView={mapToDashboardView(view)}
            onNavigate={handleSidebarNavigate}
            onPlaySudoku={() => setView('LANDING')}
            onLogout={async () => {
              await logout();
              setView('LANDING');
            }}
          />

          <main className="flex-1 flex flex-col min-w-0 bg-[#02040a]/90 backdrop-blur-sm overflow-hidden">
            {view === 'DASHBOARD' && (
              <PlayerDashboardPage
                onStartGame={handleStartNewGame}
                onViewHistory={() => setView('GAME_HISTORY')}
                onViewStats={() => setView('STATISTICS')}
              />
            )}

            {view === 'ADMIN_ML' && <AdminMLDashboardPage />}

            {view === 'PUZZLE_MANAGEMENT' && <PuzzleManagementPage onPlayPuzzle={async (puzzleId) => {
              setIsLoading(true);
              setErrorMessage(null);
              try {
                const game = await GameApi.createGame({ puzzleId });
                setCurrentGame(game);
                setView('GAME');
              } catch (err) {
                setErrorMessage(err instanceof Error ? err.message : 'Unable to launch this puzzle.');
              } finally {
                setIsLoading(false);
              }
            }} />}

            {view === 'PROFILE' && (
              <div className="p-4 sm:p-6 lg:p-8 flex-1 overflow-y-auto">
                <ProfilePage onBack={() => setView('DASHBOARD')} onLogout={() => setView('LANDING')} />
              </div>
            )}

            {view === 'GAME_HISTORY' && <GameHistoryPage />}

            {view === 'STATISTICS' && <StatisticsPage />}
          </main>
        </div>
      )}

      {/* Views */}
      {view === 'LANDING' && (
        <LandingPage
          onStartNewGame={handleStartNewGame}
          onContinueGame={handleContinueGame}
        />
      )}

      {view === 'GAME' && currentGame && (
        <GamePage
          initialGame={currentGame}
          onHome={handleReturnToLanding}
          onNewGameRequest={handleStartNewGame}
        />
      )}

      {view === 'REGISTER' && (
        <RegisterPage onBack={() => setView('LANDING')} onLogin={() => setView('LOGIN')} onComplete={() => setView('LANDING')} />
      )}

      {view === 'LOGIN' && (
        <LoginPage onBack={() => setView('LANDING')} onRegister={() => setView('REGISTER')} onComplete={() => setView('LANDING')} />
      )}

      {view === 'PROFILE' && !user && !isAuthLoading && (
        <LoginPage onBack={() => setView('LANDING')} onRegister={() => setView('REGISTER')} onComplete={() => setView('LANDING')} />
      )}

      {view === 'LEADERBOARD' && (
        <LeaderboardPage
          onBack={() => setView(user ? 'DASHBOARD' : 'LANDING')}
          onSelectProfile={(username) => {
            setProfileUsername(username);
            setView('PLAYER_PROFILE');
          }}
        />
      )}

      {view === 'PLAYER_PROFILE' && profileUsername && (
        <PublicProfilePage username={profileUsername} onBack={() => setView('LEADERBOARD')} />
      )}

      {view === 'MULTIPLAYER' && (
        <MultiplayerLandingPage
          onBack={() => setView('LANDING')}
          onRoomJoined={(code) => {
            setActiveRoomCode(code);
            setView('ROOM_WAITING');
          }}
          onLogin={() => setView('LOGIN')}
        />
      )}

      {view === 'ROOM_WAITING' && activeRoomCode && (
        <RoomWaitingPage
          roomCode={activeRoomCode}
          onBack={() => {
            setActiveRoomCode(null);
            setView('LANDING');
          }}
          onGameStarted={() => setView('ROOM_GAME')}
        />
      )}

      {view === 'ROOM_GAME' && activeRoomCode && (
        <MultiplayerGamePage
          roomCode={activeRoomCode}
          onHome={() => {
            setActiveRoomCode(null);
            setView('LANDING');
          }}
        />
      )}
    </div>
  );
};

export default App;
