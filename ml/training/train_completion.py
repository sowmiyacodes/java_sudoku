"""
Player Puzzle Completion Probability Prediction Model.

Predicts whether a player is likely to complete a Sudoku puzzle successfully
given their profile, historical performance, and real-time in-game state.

Outputs:
- completion_probability (0.0 to 1.0)
- predicted_completion (bool)
- confidence (0.0 to 1.0)
- top_factors (explainable ML factors)
"""

import os
import json
import joblib
import datetime
import numpy as np
import pandas as pd
from typing import Dict, Any

from sklearn.model_selection import GroupShuffleSplit
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, roc_auc_score

BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.join(BASE_DIR, "..", "data")
MODELS_DIR = os.path.join(BASE_DIR, "..", "models")
SESSIONS_PATH = os.path.join(DATA_DIR, "raw", "player_gameplay_sessions.csv")
APP_SESSIONS_PATH = os.path.join(DATA_DIR, "application", "player_gameplay.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

COMPLETION_FEATURE_COLUMNS = [
    "skill_level_numeric",
    "difficulty_numeric",
    "historical_completion_rate",
    "average_solving_time",
    "recent_accuracy",
    "current_streak",
    "current_progress",
    "elapsed_time_ratio",
    "puzzle_complexity_estimate"
]

def generate_completion_training_dataset(sessions_df: pd.DataFrame) -> pd.DataFrame:
    """
    Expands each completed/abandoned session into standardized progress
    checkpoints. Checkpoint features use only difficulty and player history
    available before the session; final-session duration, accuracy, hints, and
    mistakes are deliberately excluded to prevent target leakage.

    The source CSV contains session summaries rather than move-by-move
    telemetry, so the progress checkpoints are standardized training stages,
    not reconstructed timestamps from an individual playthrough.
    """
    required_columns = {
        "game_id", "player_id", "difficulty", "completion_status",
        "duration", "accuracy", "timestamp"
    }
    missing = required_columns.difference(sessions_df.columns)
    if missing:
        raise ValueError(f"Gameplay sessions are missing required columns: {sorted(missing)}")

    sessions = sessions_df.copy()
    sessions["completion_status"] = sessions["completion_status"].astype(str).str.upper()
    sessions = sessions[sessions["completion_status"].isin({"COMPLETED", "ABANDONED", "FAILED"})]
    sessions["timestamp"] = pd.to_datetime(sessions["timestamp"], errors="coerce")
    sessions = sessions.sort_values(["player_id", "timestamp", "game_id"], kind="stable")
    if sessions.empty:
        raise ValueError("No completed or abandoned gameplay sessions are available for training.")

    rows = []
    difficulty_map = {"EASY": 1, "MEDIUM": 2, "HARD": 3, "EXPERT": 4}
    history_by_player: Dict[Any, list[dict[str, Any]]] = {}

    for _, session in sessions.iterrows():
        player_id = session["player_id"]
        history = history_by_player.setdefault(player_id, [])
        prior_completed = [item for item in history if item["completed"]]
        historical_completion_rate = (
            len(prior_completed) / len(history) if history else 0.5
        )
        average_solving_time = (
            sum(item["duration"] for item in history) / len(history) if history else 450.0
        )
        recent_sessions = history[-5:]
        recent_accuracy = (
            sum(item["accuracy"] for item in recent_sessions) / len(recent_sessions)
            if recent_sessions else 0.88
        )
        current_streak = 0
        for item in reversed(history):
            if not item["completed"]:
                break
            current_streak += 1

        difficulty_numeric = difficulty_map.get(
            str(session["difficulty"]).strip().upper(), 2
        )
        target_completed = int(session["completion_status"] == "COMPLETED")
        skill_numeric = (
            4 if historical_completion_rate >= 0.90 and recent_accuracy >= 0.95
            else 3 if historical_completion_rate >= 0.75 and recent_accuracy >= 0.88
            else 2 if historical_completion_rate >= 0.50 and recent_accuracy >= 0.75
            else 1
        )

        for progress in (0.25, 0.50, 0.75):
            rows.append({
                "game_id": session["game_id"],
                "player_id": player_id,
                "skill_level_numeric": float(skill_numeric),
                "difficulty_numeric": float(difficulty_numeric),
                "historical_completion_rate": round(historical_completion_rate, 4),
                "average_solving_time": round(float(average_solving_time), 1),
                "recent_accuracy": round(float(recent_accuracy), 4),
                "current_streak": float(current_streak),
                "current_progress": progress,
                "elapsed_time_ratio": round(
                    progress * average_solving_time / max(60.0, 300.0 * difficulty_numeric),
                    4,
                ),
                "puzzle_complexity_estimate": float(difficulty_numeric * 1.5),
                "target_completed": target_completed,
            })

        history.append({
            "completed": target_completed == 1,
            "duration": max(0.0, float(session["duration"])),
            "accuracy": min(1.0, max(0.0, float(session["accuracy"]))),
        })

    return pd.DataFrame(rows)

def train_and_evaluate_completion(random_state=42) -> Dict[str, Any]:
    if not os.path.exists(SESSIONS_PATH):
        raise FileNotFoundError(f"Session data not found at {SESSIONS_PATH}")

    df_sessions = pd.read_csv(SESSIONS_PATH)
    df_sessions["game_id"] = "bootstrap:" + df_sessions["game_id"].astype(str)
    df_sessions["player_id"] = "bootstrap:" + df_sessions["player_id"].astype(str)
    application_sessions = 0

    if os.path.exists(APP_SESSIONS_PATH):
        df_application = pd.read_csv(APP_SESSIONS_PATH)
        required_columns = set(df_sessions.columns)
        missing = required_columns.difference(df_application.columns)
        if missing:
            raise ValueError(f"Application gameplay data is missing columns: {sorted(missing)}")

        df_application["game_id"] = df_application["game_id"].astype(str)
        bootstrap_ids = set(df_sessions["game_id"].str.removeprefix("bootstrap:"))
        df_application = df_application[
            ~df_application["game_id"].isin(bootstrap_ids)
        ].copy()
        application_sessions = len(df_application)
        if application_sessions:
            df_application["game_id"] = "application:" + df_application["game_id"]
            df_application["player_id"] = "application:" + df_application["player_id"].astype(str)
            df_sessions = pd.concat([df_sessions, df_application], ignore_index=True)

    df = generate_completion_training_dataset(df_sessions)

    X = df[COMPLETION_FEATURE_COLUMNS]
    y = df["target_completed"].values
    groups = df["player_id"].values
    if len(set(y)) < 2 or len(set(groups)) < 3:
        raise ValueError("Completion training requires both outcomes and at least three distinct players.")

    # Keep every session and checkpoint for a player in exactly one split.
    first_split = GroupShuffleSplit(n_splits=1, test_size=0.15, random_state=random_state)
    train_val_idx, test_idx = next(first_split.split(X, y, groups))
    remaining_groups = groups[train_val_idx]
    second_split = GroupShuffleSplit(n_splits=1, test_size=0.1765, random_state=random_state)
    train_idx, val_idx = next(
        second_split.split(X.iloc[train_val_idx], y[train_val_idx], remaining_groups)
    )
    train_idx = train_val_idx[train_idx]
    val_idx = train_val_idx[val_idx]

    X_train, X_val, X_test = X.iloc[train_idx], X.iloc[val_idx], X.iloc[test_idx]
    y_train, y_val, y_test = y[train_idx], y[val_idx], y[test_idx]
    if any(len(set(labels)) < 2 for labels in (y_train, y_val, y_test)):
        raise ValueError(
            "Player-grouped train/validation/test split must contain both completion outcomes. "
            "Collect more gameplay sessions across players and retry."
        )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_val_scaled = scaler.transform(X_val)
    X_test_scaled = scaler.transform(X_test)

    candidates = {
        "Logistic Regression": {
            "model": LogisticRegression(max_iter=1000, random_state=random_state),
            "use_scaled": True
        },
        "Decision Tree": {
            "model": DecisionTreeClassifier(max_depth=6, min_samples_split=5, random_state=random_state),
            "use_scaled": False
        },
        "Random Forest": {
            "model": RandomForestClassifier(n_estimators=100, max_depth=8, random_state=random_state),
            "use_scaled": False
        },
        "Gradient Boosting": {
            "model": GradientBoostingClassifier(n_estimators=100, max_depth=4, random_state=random_state),
            "use_scaled": False
        }
    }

    comparison_results = {}
    best_name = None
    best_val_f1 = -1.0
    best_candidate_obj = None

    print("\n--- Training Completion Prediction Models ---")
    for name, c in candidates.items():
        clf = c["model"]
        X_tr = X_train_scaled if c["use_scaled"] else X_train
        X_v = X_val_scaled if c["use_scaled"] else X_val

        clf.fit(X_tr, y_train)
        y_val_pred = clf.predict(X_v)

        val_acc = float(accuracy_score(y_val, y_val_pred))
        val_f1 = float(f1_score(y_val, y_val_pred, average="weighted", zero_division=0))
        val_prec = float(precision_score(y_val, y_val_pred, average="weighted", zero_division=0))
        val_rec = float(recall_score(y_val, y_val_pred, average="weighted", zero_division=0))

        comparison_results[name] = {
            "val_accuracy": round(val_acc, 4),
            "val_f1_weighted": round(val_f1, 4),
            "val_precision": round(val_prec, 4),
            "val_recall": round(val_rec, 4)
        }
        print(f"[{name:20}] Val Acc: {val_acc:.4f} | Val F1: {val_f1:.4f}")

        if val_f1 > best_val_f1:
            best_val_f1 = val_f1
            best_name = name
            best_candidate_obj = c

    print(f"\nWinner: {best_name} (Val F1: {best_val_f1:.4f})")
    winner_model = best_candidate_obj["model"]
    winner_scaled = best_candidate_obj["use_scaled"]

    # Final Test Set Evaluation
    X_te = X_test_scaled if winner_scaled else X_test
    y_test_pred = winner_model.predict(X_te)
    y_test_probs = winner_model.predict_proba(X_te)[:, 1] if hasattr(winner_model, "predict_proba") else y_test_pred

    test_acc = float(accuracy_score(y_test, y_test_pred))
    test_prec = float(precision_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_rec = float(recall_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_f1 = float(f1_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_auc = float(roc_auc_score(y_test, y_test_probs))

    cm = confusion_matrix(y_test, y_test_pred)

    feature_importances = {}
    if hasattr(winner_model, "feature_importances_"):
        for col, imp in zip(COMPLETION_FEATURE_COLUMNS, winner_model.feature_importances_):
            feature_importances[col] = round(float(imp), 4)
    elif hasattr(winner_model, "coef_"):
        for col, imp in zip(COMPLETION_FEATURE_COLUMNS, np.abs(winner_model.coef_[0])):
            feature_importances[col] = round(float(imp), 4)

    # Save artifacts
    model_path = os.path.join(MODELS_DIR, "completion_model.pkl")
    scaler_path = os.path.join(MODELS_DIR, "completion_scaler.pkl")
    meta_path = os.path.join(MODELS_DIR, "completion_metadata.json")
    cm_path = os.path.join(MODELS_DIR, "completion_confusion_matrix.json")

    joblib.dump(winner_model, model_path)
    joblib.dump(scaler, scaler_path)

    cm_data = {
        "labels": ["Abandoned/Failed", "Completed"],
        "matrix": cm.tolist()
    }
    with open(cm_path, "w", encoding="utf-8") as f:
        json.dump(cm_data, f, indent=2)

    metadata = {
        "model_type": "puzzle_completion",
        "algorithm": best_name,
        "version": "v2.0",
        "trained_at": datetime.datetime.now().isoformat(),
        "uses_scaler": bool(winner_scaled),
        "dataset_samples": len(df),
        "dataset_sessions": int(df["game_id"].nunique()),
        "dataset_players": int(df["player_id"].nunique()),
        "application_sessions": application_sessions,
        "training_sources": ["ml/data/raw/player_gameplay_sessions.csv"]
        + (["ml/data/application/player_gameplay.csv"] if application_sessions else []),
        "features": COMPLETION_FEATURE_COLUMNS,
        "classes": ["ABANDONED_OR_FAILED", "COMPLETED"],
        "evaluation_split": "70/15/15 player-grouped",
        "checkpoint_method": "standardized progress stages from session summaries",
        "model_comparison": comparison_results,
        "validation_f1_weighted": round(best_val_f1, 4),
        "test_metrics": {
            "accuracy": round(test_acc, 4),
            "precision_weighted": round(test_prec, 4),
            "recall_weighted": round(test_rec, 4),
            "f1_weighted": round(test_f1, 4),
            "roc_auc": round(test_auc, 4)
        },
        "feature_importances": dict(sorted(feature_importances.items(), key=lambda x: x[1], reverse=True)),
        "artifacts": {
            "model_path": "ml/models/completion_model.pkl",
            "scaler_path": "ml/models/completion_scaler.pkl"
        }
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print("\n--- Final Completion Test Set Results ---")
    print(f"Accuracy: {test_acc:.4f} | F1: {test_f1:.4f} | ROC-AUC: {test_auc:.4f}")
    return metadata

if __name__ == "__main__":
    train_and_evaluate_completion()
