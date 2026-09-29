"""
Sudoku Puzzle Feature Extractor.
Extracts structural, candidate, and complexity features from 81-character puzzle strings.
"""

import numpy as np
from typing import Dict, List, Any

PUZZLE_FEATURE_COLUMNS = [
    "clue_count",
    "empty_cell_count",
    "clue_density",
    "candidate_count",
    "min_candidates_per_cell",
    "max_candidates_per_cell",
    "avg_candidates_per_cell",
    "single_candidate_cells",
    "box_clue_variance",
    "row_clue_variance",
    "col_clue_variance",
    "symmetry_score",
    "branching_factor_estimate"
]

def parse_puzzle_grid(puzzle_str: str) -> np.ndarray:
    """Parses an 81-character string into a 9x9 integer numpy array."""
    clean_str = normalize_puzzle_string(puzzle_str)
    grid = np.zeros((9, 9), dtype=int)
    for i, char in enumerate(clean_str):
        row, col = divmod(i, 9)
        grid[row, col] = int(char) if char.isdigit() else 0
    return grid

def normalize_puzzle_string(puzzle_str: str) -> str:
    """Validate and normalize a puzzle into an 81-character dot-empty string."""
    if not isinstance(puzzle_str, str):
        raise ValueError("Puzzle must be a string.")
    clean_str = puzzle_str.strip().replace("0", ".")
    if len(clean_str) != 81 or any(char not in ".123456789" for char in clean_str):
        raise ValueError("Puzzle must contain exactly 81 digits, dots, or zeroes.")

    grid = np.array([int(char) if char != "." else 0 for char in clean_str]).reshape(9, 9)
    for index in range(9):
        units = [grid[index, :], grid[:, index], grid[(index // 3) * 3:(index // 3) * 3 + 3,
                                                     (index % 3) * 3:(index % 3) * 3 + 3].flatten()]
        if any(len(values := [int(value) for value in unit if value]) != len(set(values)) for unit in units):
            raise ValueError("Puzzle contains conflicting givens.")
    return clean_str

def get_candidates(grid: np.ndarray, row: int, col: int) -> set[int]:
    """Returns valid candidate digits (1-9) for a given cell in grid."""
    if grid[row, col] != 0:
        return set()
    used = set(grid[row, :]) | set(grid[:, col])
    box_r, box_c = (row // 3) * 3, (col // 3) * 3
    used |= set(grid[box_r:box_r + 3, box_c:box_c + 3].flatten())
    used.discard(0)
    return set(range(1, 10)) - used

def extract_puzzle_features(puzzle_str: str) -> Dict[str, float]:
    """
    Extracts 13 mathematical and logical features from a Sudoku puzzle string.
    """
    grid = parse_puzzle_grid(puzzle_str)
    
    clue_mask = grid > 0
    clue_count = int(np.sum(clue_mask))
    empty_cell_count = 81 - clue_count
    clue_density = round(clue_count / 81.0, 4)
    
    # Candidate analysis
    candidate_counts = []
    single_candidate_cells = 0
    
    for r in range(9):
        for c in range(9):
            if grid[r, c] == 0:
                cands = get_candidates(grid, r, c)
                cand_len = len(cands)
                candidate_counts.append(cand_len)
                if cand_len == 1:
                    single_candidate_cells += 1

    if candidate_counts:
        candidate_count = int(sum(candidate_counts))
        min_candidates_per_cell = int(min(candidate_counts))
        max_candidates_per_cell = int(max(candidate_counts))
        avg_candidates_per_cell = round(float(np.mean(candidate_counts)), 4)
    else:
        candidate_count = 0
        min_candidates_per_cell = 0
        max_candidates_per_cell = 0
        avg_candidates_per_cell = 0.0

    # Clue distributions across rows, columns, and 3x3 boxes
    row_clues = [int(np.sum(clue_mask[r, :])) for r in range(9)]
    col_clues = [int(np.sum(clue_mask[:, c])) for c in range(9)]
    box_clues = []
    for br in range(3):
        for bc in range(3):
            box_clues.append(int(np.sum(clue_mask[br * 3:(br + 1) * 3, bc * 3:(bc + 1) * 3])))

    row_clue_variance = round(float(np.var(row_clues)), 4)
    col_clue_variance = round(float(np.var(col_clues)), 4)
    box_clue_variance = round(float(np.var(box_clues)), 4)

    # 180-degree rotational symmetry score
    matches = 0
    for r in range(9):
        for c in range(9):
            opp_r, opp_c = 8 - r, 8 - c
            if (grid[r, c] > 0 and grid[opp_r, opp_c] > 0) or (grid[r, c] == 0 and grid[opp_r, opp_c] == 0):
                matches += 1
    symmetry_score = round(matches / 81.0, 4)

    # Branching factor estimate
    # Higher average candidates and fewer single-candidate cells lead to combinatorial branching
    branching_factor_estimate = round(
        (avg_candidates_per_cell * empty_cell_count) / max(1, single_candidate_cells + 5), 4
    )

    return {
        "clue_count": float(clue_count),
        "empty_cell_count": float(empty_cell_count),
        "clue_density": clue_density,
        "candidate_count": float(candidate_count),
        "min_candidates_per_cell": float(min_candidates_per_cell),
        "max_candidates_per_cell": float(max_candidates_per_cell),
        "avg_candidates_per_cell": avg_candidates_per_cell,
        "single_candidate_cells": float(single_candidate_cells),
        "box_clue_variance": box_clue_variance,
        "row_clue_variance": row_clue_variance,
        "col_clue_variance": col_clue_variance,
        "symmetry_score": symmetry_score,
        "branching_factor_estimate": branching_factor_estimate
    }
