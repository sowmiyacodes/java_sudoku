import { useEffect, useState, useRef } from 'react';
import type { GameStatus } from '../types/sudoku';

interface UseGameTimerProps {
  initialSeconds: number;
  status: GameStatus;
  onTick?: (seconds: number) => void;
}

export function useGameTimer({ initialSeconds, status, onTick }: UseGameTimerProps) {
  const [seconds, setSeconds] = useState(initialSeconds);
  const onTickRef = useRef(onTick);
  onTickRef.current = onTick;

  // Resynchronize state with backend when initialSeconds changes
  useEffect(() => {
    setSeconds(initialSeconds);
  }, [initialSeconds]);

  useEffect(() => {
    if (status !== 'IN_PROGRESS') {
      return;
    }

    const interval = setInterval(() => {
      setSeconds((prev) => {
        const next = prev + 1;
        if (onTickRef.current) {
          onTickRef.current(next);
        }
        return next;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [status]);

  const formatTime = (totalSeconds: number): string => {
    const hrs = Math.floor(totalSeconds / 3600);
    const mins = Math.floor((totalSeconds % 3600) / 60);
    const secs = totalSeconds % 60;

    const pad = (n: number) => n.toString().padStart(2, '0');

    if (hrs > 0) {
      return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
    }
    return `${pad(mins)}:${pad(secs)}`;
  };

  return {
    seconds,
    formattedTime: formatTime(seconds),
    formatTime,
    setSeconds,
  };
}
