import React from 'react';
import { Eraser } from 'lucide-react';

interface NumberPadProps {
  onNumberSelect: (num: number) => void;
  onErase: () => void;
  disabled: boolean;
  numberCounts?: { [num: number]: number };
}

export const NumberPad: React.FC<NumberPadProps> = ({
  onNumberSelect,
  onErase,
  disabled,
  numberCounts = {},
}) => {
  const numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9];

  return (
    <div className="w-full">
      {/* 1-9 Numeric Buttons */}
      <div className="grid grid-cols-5 sm:grid-cols-9 lg:grid-cols-3 gap-1.5 sm:gap-2 lg:gap-2.5">
        {numbers.map((num) => {
          const count = numberCounts[num] || 0;
          const isAllPlaced = count >= 9;

          return (
            <button
              key={num}
              id={`numpad-btn-${num}`}
              type="button"
              disabled={disabled}
              onClick={() => onNumberSelect(num)}
              className={`relative flex flex-col items-center justify-center h-12 sm:h-13 lg:h-14 rounded-xl font-mono text-xl sm:text-2xl font-bold transition-all duration-150 active:scale-95 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed ${
                isAllPlaced
                  ? 'bg-slate-800/30 text-slate-500 border border-slate-700/30'
                  : 'bg-slate-800/60 hover:bg-cyan-500/20 text-cyan-200 hover:text-white border border-cyan-500/25 hover:border-cyan-400 hover:shadow-[0_0_15px_rgba(6,182,212,0.3)]'
              }`}
            >
              <span>{num}</span>
              {count > 0 && (
                <span className="text-[10px] font-sans font-normal text-slate-400 -mt-1">
                  {count}/9
                </span>
              )}
            </button>
          );
        })}

        {/* Erase button for mobile numpad row */}
        <button
          id="numpad-btn-erase"
          type="button"
          disabled={disabled}
          onClick={onErase}
          title="Erase cell (Backspace / Del)"
          className="col-span-1 sm:hidden flex flex-col items-center justify-center h-12 rounded-xl font-mono text-xs font-medium bg-rose-950/40 hover:bg-rose-900/50 text-rose-300 border border-rose-500/30 active:scale-95 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <Eraser className="w-4 h-4 mb-0.5" />
          <span>Erase</span>
        </button>
      </div>

      {/* Keyboard Shortcuts Hint */}
      <div className="hidden sm:flex flex-col gap-1 mt-3 px-1 text-[11px] text-slate-400 border-t border-slate-800/60 pt-2.5">
        <div className="flex items-center justify-between">
          <span>Input numbers:</span>
          <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300 font-mono">1-9</kbd>
        </div>
        <div className="flex items-center justify-between">
          <span>Erase number:</span>
          <span>
            <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300 font-mono">Backspace</kbd> / <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300 font-mono">Del</kbd>
          </span>
        </div>
        <div className="flex items-center justify-between">
          <span>Move selection:</span>
          <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300 font-mono">Arrow Keys</kbd>
        </div>
      </div>
    </div>
  );
};
