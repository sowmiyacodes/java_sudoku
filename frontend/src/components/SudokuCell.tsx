import React from 'react';

interface SudokuCellProps {
  row: number;
  col: number;
  value: number;
  isInitial: boolean;
  isSelected: boolean;
  isInSelectedRow: boolean;
  isInSelectedCol: boolean;
  isInSelectedBox: boolean;
  isSameNumber: boolean;
  isError: boolean;
  onClick: (row: number, col: number) => void;
}

export const SudokuCell: React.FC<SudokuCellProps> = ({
  row,
  col,
  value,
  isInitial,
  isSelected,
  isInSelectedRow,
  isInSelectedCol,
  isInSelectedBox,
  isSameNumber,
  isError,
  onClick,
}) => {
  // Border logic for 3x3 grid separation
  const borderRight = (col + 1) % 3 === 0 && col !== 8 ? 'border-r-2 border-r-cyan-400/40' : 'border-r border-r-white/10';
  const borderBottom = (row + 1) % 3 === 0 && row !== 8 ? 'border-b-2 border-b-cyan-400/40' : 'border-b border-b-white/10';

  // Determine cell background styling based on state
  let bgClass = 'bg-slate-900/40 hover:bg-slate-800/60';
  
  if (isSelected) {
    bgClass = 'bg-cyan-500/30 ring-2 ring-cyan-400 shadow-[0_0_15px_rgba(6,182,212,0.5)] z-20';
  } else if (isError) {
    bgClass = 'bg-rose-500/30 ring-2 ring-rose-500 shadow-[0_0_12px_rgba(244,63,94,0.4)] animate-shake z-10';
  } else if (isSameNumber && value !== 0) {
    bgClass = 'bg-cyan-400/20 text-cyan-200 ring-1 ring-cyan-400/50';
  } else if (isInSelectedRow || isInSelectedCol || isInSelectedBox) {
    bgClass = 'bg-blue-950/40';
  }

  // Text color & styling
  let textClass = 'text-white/30';
  if (value !== 0) {
    if (isError) {
      textClass = 'text-rose-400 font-bold';
    } else if (isInitial) {
      textClass = 'text-sky-100 font-bold';
    } else {
      textClass = 'text-cyan-300 font-medium';
    }
  }

  return (
    <button
      type="button"
      id={`cell-${row}-${col}`}
      data-row={row}
      data-col={col}
      aria-label={`Row ${row + 1}, Column ${col + 1}, Value ${value === 0 ? 'empty' : value}`}
      onClick={() => onClick(row, col)}
      className={`relative w-full aspect-square flex items-center justify-center text-lg sm:text-2xl md:text-3xl font-mono select-none transition-all duration-150 cursor-pointer focus:outline-none ${borderRight} ${borderBottom} ${bgClass} ${textClass}`}
    >
      {value !== 0 ? (
        <span className={`transform transition-transform duration-150 ${isSelected ? 'scale-110' : 'scale-100'}`}>
          {value}
        </span>
      ) : null}
      {isInitial && value !== 0 && (
        <span className="absolute bottom-1 right-1 w-1 h-1 rounded-full bg-cyan-400/40" />
      )}
    </button>
  );
};
