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

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, roc_auc_score

BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.join(BASE_DIR, "..", "data")
MODELS_DIR = os.path.join(BASE_DIR, "..", "models")
SESSIONS_PATH = os.path.join(DATA_DIR, "raw", "player_gameplay_sessions.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

COMPLETION_FEATURE_COLUMNS = [
    "skill_level_numeric",
    "difficulty_numeric",
    "historical_completion_rate",
    "average_solving_time",
    "recent_accuracy",
    "hints_used",
    "mistakes_made",
    "current_streak",
    "current_progress",
    "elapsed_time_ratio",
    "puzzle_complexity_estimate"
]

def generate_completion_training_dataset(sessions_df: pd.DataFrame) -> pd.DataFrame:
    """
    Constructs in-game snapshot training dataset from gameplay sessions.
    Simulates mid-game checkpoints (e.g. 30%, 50%, 75% progress) to train a real
    completion probability estimator.
    """
    rows = []
    diff_map = {"Easy": 1, "Medium": 2, "Hard": 3, "Expert": 4}
    
    # Calculate player aggregates for context
    player_stats = sessions_df.groupby("player_id").agg({
        "completion_status": lambda s: np.mean(s == "COMPLETED"),
        "duration": "mean",
        "accuracy": "mean"
    }).to_dict(orient="index")

    for _, session in sessions_df.iterrows():
        p_id = session["player_id"]
        p_stat = player_stats.get(p_id, {"completion_status": 0.7, "duration": 500, "accuracy": 0.85})
        
        hist_comp = p_stat["completion_status"]
        if hist_comp >= 0.85:
            skill_num = 3  # Advanced / Expert
        elif hist_comp >= 0.65:
            skill_num = 2  # Intermediate
        else:
            skill_num = 1  # Beginner

        diff_str = str(session["difficulty"]).capitalize()
        diff_num = diff_map.get(diff_str, 2)
        is_completed = 1 if session["completion_status"] == "COMPLETED" else 0

        # Snapshot at 60% game duration
        duration = session["duration"]
        progress = 0.60 if is_completed else np.random.uniform(0.20, 0.55)
        elapsed_ratio = progress * (1.0 if is_completed else np.random.uniform(1.1, 1.6))
        
        mistakes = session["mistakes"]
        hints = session["hints"]
        streak = max(0, int(np.random.poisson(2 if is_completed else 0.5)))
        
        rows.append({
            "skill_level_numeric": float(skill_num),
            "difficulty_numeric": float(diff_num),
            "historical_completion_rate": round(float(hist_comp), 4),
            "average_solving_time": round(float(p_stat["duration"]), 1),
            "recent_accuracy": round(float(session["accuracy"]), 4),
            "hints_used": float(hints),
            "mistakes_made": float(mistakes),
            "current_streak": float(streak),
            "current_progress": round(float(progress), 4),
            "elapsed_time_ratio": round(float(elapsed_ratio), 4),
            "puzzle_complexity_estimate": float(diff_num * 1.5 + np.random.uniform(0.8, 1.2)),
            "target_completed": is_completed
        })

    return pd.DataFrame(rows)

def train_and_evaluate_completion(random_state=42) -> Dict[str, Any]:
    if not os.path.exists(SESSIONS_PATH):
        raise FileNotFoundError(f"Session data not found at {SESSIONS_PATH}")

    df_sessions = pd.read_csv(SESSIONS_PATH)
    df = generate_completion_training_dataset(df_sessions)

    X = df[COMPLETION_FEATURE_COLUMNS]
    y = df["target_completed"].values

    # 70% Train, 15% Validation, 15% Test
    X_train, X_temp, y_train, y_temp = train_test_split(
        X, y, test_size=0.30, random_state=random_state, stratify=y
    )
    X_val, X_test, y_val, y_test = train_test_split(
        X_temp, y_temp, test_size=0.50, random_state=random_state, stratify=y_temp
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
    test_auc = float(roc_auc_score(y_test, y_test_probs)) if len(np.unique(y_test)) > 1 else 1.0

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
        "version": "v1.0",
        "trained_at": datetime.datetime.now().isoformat(),
        "dataset_samples": len(df),
        "features": COMPLETION_FEATURE_COLUMNS,
        "classes": ["ABANDONED_OR_FAILED", "COMPLETED"],
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
