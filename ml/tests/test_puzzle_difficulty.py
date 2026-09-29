import pandas as pd
import pytest

from ml.features.puzzle_features import normalize_puzzle_string
from ml.training import train_difficulty


def test_puzzle_validation_rejects_malformed_and_conflicting_givens():
    with pytest.raises(ValueError, match="exactly 81"):
        normalize_puzzle_string("123")
    with pytest.raises(ValueError, match="conflicting givens"):
        normalize_puzzle_string("11" + "." * 79)


def test_difficulty_dataset_skips_invalid_and_deduplicates_sources(tmp_path, monkeypatch):
    puzzle = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
    benchmark_path = tmp_path / "benchmark.csv"
    human_path = tmp_path / "human.csv"
    pd.DataFrame([
        {"puzzle": puzzle, "difficulty": "Easy"},
        {"puzzle": "invalid", "difficulty": "Hard"},
    ]).to_csv(benchmark_path, index=False)
    pd.DataFrame([
        {"Sudoku Puzzle": puzzle.replace(".", "0"), "D_TR": 2.2},
        {"Sudoku Puzzle": "x" * 81, "D_TR": 1.0},
    ]).to_csv(human_path, index=False)
    monkeypatch.setattr(train_difficulty, "BENCHMARK_PATH", str(benchmark_path))
    monkeypatch.setattr(train_difficulty, "HUMAN_PATH", str(human_path))

    dataset = train_difficulty.build_puzzle_dataset()

    assert len(dataset) == 1
    assert dataset.iloc[0]["difficulty"] == "EASY"
    assert dataset.iloc[0]["source"] == "SudokuExchange"