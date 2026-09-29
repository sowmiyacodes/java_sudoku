"""
Feature engineering and derived skill labeling for Sudoku Player Intelligence.
Transforms session-level game records into aggregated player-level feature vectors.
"""

import pandas as pd
import numpy as np

FEATURE_COLUMNS = [
    "games_played",
    "completion_rate",
    "average_time",
    "average_score",
    "average_accuracy",
    "average_mistakes",
    "average_hints",
    "average_undos",
    "average_moves",
    "hard_completion_rate",
    "medium_completion_rate",
    "easy_completion_rate",
    "recent_accuracy",
    "recent_error_rate",
    "average_moves_per_minute"
]

def derive_skill_label(row: dict) -> str:
    """
    Computes a multi-criteria Behavioral Evaluation Scoring Index (BESI)
    combining completion rate, accuracy, error restraint, independence,
    difficulty handling, and speed efficiency.
    Returns: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT'
    """
    completion_factor = float(row.get("completion_rate", 0.0))
    accuracy_factor = float(row.get("average_accuracy", 0.0))
    
    # Error restraint: 0 mistakes -> 1.0, 5+ mistakes -> 0.0
    mistakes = float(row.get("average_mistakes", 0.0))
    error_factor = max(0.0, 1.0 - min(1.0, mistakes / 5.0))
    
    # Hint independence: 0 hints -> 1.0, 4+ hints -> 0.0
    hints = float(row.get("average_hints", 0.0))
    hint_factor = max(0.0, 1.0 - min(1.0, hints / 4.0))
    
    # Difficulty handling
    hard_rate = float(row.get("hard_completion_rate", 0.0))
    med_rate = float(row.get("medium_completion_rate", 0.0))
    diff_factor = 0.60 * hard_rate + 0.40 * med_rate
    
    # Speed efficiency
    avg_time = float(row.get("average_time", 600.0))
    speed_factor = float(np.clip(1.0 - ((avg_time - 250.0) / 900.0), 0.0, 1.0))

    # Composite mastery index (0.0 to 1.0)
    mastery = (
        0.25 * completion_factor +
        0.25 * accuracy_factor +
        0.15 * error_factor +
        0.15 * hint_factor +
        0.10 * diff_factor +
        0.10 * speed_factor
    )

    if mastery >= 0.85:
        return "EXPERT"
    elif mastery >= 0.70:
        return "ADVANCED"
    elif mastery >= 0.52:
        return "INTERMEDIATE"
    else:
        return "BEGINNER"

def build_player_features(df_sessions: pd.DataFrame) -> pd.DataFrame:
    """
    Aggregates per-game session records into player-level features.
    """
    player_rows = []

    for player_id, group in df_sessions.groupby("player_id"):
        # Sort by timestamp if available
        if "timestamp" in group.columns:
            group = group.sort_values("timestamp")

        games_played = len(group)
        completed_games = group[group["completion_status"] == "COMPLETED"]
        games_completed = len(completed_games)
        completion_rate = round(games_completed / games_played, 4) if games_played > 0 else 0.0

        avg_time = round(float(group["duration"].mean()), 2)
        best_time = int(completed_games["duration"].min()) if not completed_games.empty else int(group["duration"].min())
        avg_score = round(float(group["score"].mean()), 2)
        avg_acc = round(float(group["accuracy"].mean()), 4)
        avg_mistakes = round(float(group["mistakes"].mean()), 2)
        avg_hints = round(float(group["hints"].mean()), 2)
        avg_undos = round(float(group["undos"].mean()), 2)
        avg_moves = round(float(group["moves"].mean()), 2)

        # Difficulty breakdowns
        easy_games = group[group["difficulty"].str.lower() == "easy"]
        med_games = group[group["difficulty"].str.lower() == "medium"]
        hard_games = group[group["difficulty"].str.lower() == "hard"]

        easy_comp = round(len(easy_games[easy_games["completion_status"] == "COMPLETED"]) / len(easy_games), 4) if len(easy_games) > 0 else 0.0
        med_comp = round(len(med_games[med_games["completion_status"] == "COMPLETED"]) / len(med_games), 4) if len(med_games) > 0 else 0.0
        hard_comp = round(len(hard_games[hard_games["completion_status"] == "COMPLETED"]) / len(hard_games), 4) if len(hard_games) > 0 else 0.0

        # Recent performance (last 5 games)
        recent_group = group.tail(5)
        recent_acc = round(float(recent_group["accuracy"].mean()), 4)
        recent_moves = recent_group["moves"].sum()
        recent_errs = recent_group["mistakes"].sum()
        recent_err_rate = round(recent_errs / (recent_moves + recent_errs), 4) if (recent_moves + recent_errs) > 0 else 0.0

        # Moves per minute
        total_duration_mins = group["duration"].sum() / 60.0
        moves_per_min = round(float(group["moves"].sum() / total_duration_mins), 2) if total_duration_mins > 0 else 0.0

        row = {
            "player_id": player_id,
            "games_played": games_played,
            "games_completed": games_completed,
            "completion_rate": completion_rate,
            "average_time": avg_time,
            "best_time": best_time,
            "average_score": avg_score,
            "average_accuracy": avg_acc,
            "average_mistakes": avg_mistakes,
            "average_hints": avg_hints,
            "average_undos": avg_undos,
            "average_moves": avg_moves,
            "hard_completion_rate": hard_comp,
            "medium_completion_rate": med_comp,
            "easy_completion_rate": easy_comp,
            "recent_accuracy": recent_acc,
            "recent_error_rate": recent_err_rate,
            "average_moves_per_minute": moves_per_min
        }
        
        row["skill_level"] = derive_skill_label(row)
        player_rows.append(row)

    return pd.DataFrame(player_rows)
