import React from 'react';
import type { CellCoordinate } from '../types/sudoku';
import { SudokuCell } from './SudokuCell';

interface SudokuBoardProps {
  board: number[][];
  initialBoard: number[][];
  selectedCell: CellCoordinate | null;
  errorCells?: CellCoordinate[];
  onCellClick: (row: number, col: number) => void;
  isPaused: boolean;
}

export const SudokuBoard: React.FC<SudokuBoardProps> = ({
  board,
  initialBoard,
  selectedCell,
  errorCells = [],
  onCellClick,
  isPaused,
}) => {
  const selectedValue =
    selectedCell && board[selectedCell.row] ? board[selectedCell.row][selectedCell.col] : 0;

  const isCellInError = (r: number, c: number) => {
    return errorCells.some((err) => err.row === r && err.col === c);
  };

  const isSameBox = (r1: number, c1: number, r2: number, c2: number) => {
    return Math.floor(r1 / 3) === Math.floor(r2 / 3) && Math.floor(c1 / 3) === Math.floor(c2 / 3);
  };

  return (
    <div className="relative w-full max-w-[500px] aspect-square mx-auto p-1 sm:p-2.5 rounded-2xl bg-slate-900/60 backdrop-blur-md border border-cyan-500/30 shadow-[0_0_35px_rgba(6,182,212,0.15)] overflow-hidden">
      {/* Board 9x9 Grid */}
      <div className="grid grid-cols-9 grid-rows-9 w-full h-full rounded-xl overflow-hidden bg-slate-950/80 border border-cyan-500/20">
        {board.map((rowArr, rowIndex) =>
          rowArr.map((cellValue, colIndex) => {
            const isSelected =
              selectedCell?.row === rowIndex && selectedCell?.col === colIndex;
            const isInSelectedRow = selectedCell?.row === rowIndex;
            const isInSelectedCol = selectedCell?.col === colIndex;
            const isInSelectedBox =
              selectedCell !== null && isSameBox(selectedCell.row, selectedCell.col, rowIndex, colIndex);
            const isSameNum =
              selectedValue > 0 && cellValue === selectedValue;
            const isInitial = initialBoard[rowIndex]?.[colIndex] !== 0;
            const isError = isCellInError(rowIndex, colIndex);

            return (
              <SudokuCell
                key={`cell-${rowIndex}-${colIndex}`}
                row={rowIndex}
                col={colIndex}
                value={cellValue}
                isInitial={isInitial}
                isSelected={isSelected}
                isInSelectedRow={isInSelectedRow}
                isInSelectedCol={isInSelectedCol}
                isInSelectedBox={isInSelectedBox}
                isSameNumber={isSameNum}
                isError={isError}
                onClick={onCellClick}
              />
            );
          })
        )}
      </div>

      {/* Paused Blur Overlay if paused */}
      {isPaused && (
        <div className="absolute inset-0 bg-slate-950/85 backdrop-blur-lg flex flex-col items-center justify-center z-30 transition-all duration-300">
          <div className="p-4 rounded-full bg-cyan-500/10 border border-cyan-500/30 mb-3 animate-pulse">
            <svg className="w-8 h-8 text-cyan-400" fill="currentColor" viewBox="0 0 24 24">
              <path d="M6 4h4v16H6zm8 0h4v16h-4z" />
            </svg>
          </div>
          <p className="text-xl font-semibold tracking-wider text-cyan-200">GAME PAUSED</p>
          <p className="text-xs text-slate-400 mt-1">Board is hidden while paused</p>
        </div>
      )}
    </div>
  );
};
