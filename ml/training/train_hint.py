"""
ML-Assisted Hint Recommendation Model Training & Evaluation.

Predicts optimal non-intrusive hint category:
- CELL (highlights a specific high-probability cell)
- ROW (directs attention to a constrained row)
- COLUMN (directs attention to a constrained column)
- REGION (directs attention to a 3x3 block)
- TECHNIQUE (advises logical method: Naked Single, Hidden Single, Pointing Pair)
- NEXT_MOVE (direct cell + value hint for severely stuck players)

Based on player skill level, difficulty, progress, error frequency, and hesitation time.
"""

import os
import json
import joblib
import random
import datetime
import numpy as np
import pandas as pd
from typing import Dict, Any

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix

BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.join(BASE_DIR, "..", "data")
MODELS_DIR = os.path.join(BASE_DIR, "..", "models")
SESSIONS_PATH = os.path.join(DATA_DIR, "raw", "player_gameplay_sessions.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

HINT_FEATURE_COLUMNS = [
    "player_skill_num",
    "difficulty_num",
    "current_progress",
    "mistakes_count",
    "elapsed_time",
    "previous_hints_used",
    "stuck_duration_seconds",
    "open_candidates_estimate"
]

def generate_hint_dataset(num_samples=2500, seed=42) -> pd.DataFrame:
    """
    Synthesizes pedagogical hint situations based on player competence,
    puzzle state, and optimal coaching strategies.
    """
    random.seed(seed)
    np.random.seed(seed)
    rows = []

    for _ in range(num_samples):
        skill = random.choice([1, 2, 3, 4])  # Beginner, Interm, Adv, Expert
        diff = random.choice([1, 2, 3, 4])   # Easy, Medium, Hard, Expert
        progress = round(random.uniform(0.10, 0.90), 2)
        mistakes = random.randint(0, 5)
        elapsed = random.randint(60, 1200)
        prev_hints = random.randint(0, 4)
        stuck_time = random.randint(15, 300)
        open_cands = max(1, int((1.0 - progress) * 81 * (2.2 + diff * 0.4)))

        # Ground-truth pedagogical rule:
        # 1. If player has high mistakes and has used > 2 hints and stuck > 180s -> NEXT_MOVE (unblock)
        # 2. If expert/advanced player stuck on hard puzzle -> TECHNIQUE (coach strategy)
        # 3. If progress < 0.35 and beginner -> REGION or ROW (narrow search space)
        # 4. If progress >= 0.70 and low hints -> CELL (nudge to closing cell)
        # 5. Otherwise -> COLUMN or ROW depending on grid balance
        if mistakes >= 3 and prev_hints >= 2 and stuck_time > 150:
            target_hint = "NEXT_MOVE"
        elif skill >= 3 and diff >= 3 and stuck_time > 90:
            target_hint = "TECHNIQUE"
        elif progress >= 0.65 and prev_hints <= 1:
            target_hint = "CELL"
        elif progress < 0.35:
            target_hint = random.choice(["REGION", "ROW"])
        elif stuck_time < 60 and mistakes <= 1:
            target_hint = "REGION"
        else:
            target_hint = random.choice(["ROW", "COLUMN", "TECHNIQUE"])

        rows.append({
            "player_skill_num": float(skill),
            "difficulty_num": float(diff),
            "current_progress": progress,
            "mistakes_count": float(mistakes),
            "elapsed_time": float(elapsed),
            "previous_hints_used": float(prev_hints),
            "stuck_duration_seconds": float(stuck_time),
            "open_candidates_estimate": float(open_cands),
            "hint_type": target_hint
        })

    return pd.DataFrame(rows)

def train_and_evaluate_hint(random_state=42) -> Dict[str, Any]:
    df = generate_hint_dataset()

    X = df[HINT_FEATURE_COLUMNS]
    y_raw = df["hint_type"].values

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(y_raw)

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
            "model": DecisionTreeClassifier(max_depth=6, min_samples_split=4, random_state=random_state),
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

    print("\n--- Training Hint Recommendation Models ---")
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

    test_acc = float(accuracy_score(y_test, y_test_pred))
    test_prec = float(precision_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_rec = float(recall_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_f1 = float(f1_score(y_test, y_test_pred, average="weighted", zero_division=0))

    cm = confusion_matrix(y_test, y_test_pred)
    class_names = [str(cls) for cls in label_encoder.classes_]

    feature_importances = {}
    if hasattr(winner_model, "feature_importances_"):
        for col, imp in zip(HINT_FEATURE_COLUMNS, winner_model.feature_importances_):
            feature_importances[col] = round(float(imp), 4)

    # Save artifacts
    model_path = os.path.join(MODELS_DIR, "hint_model.pkl")
    scaler_path = os.path.join(MODELS_DIR, "hint_scaler.pkl")
    le_path = os.path.join(MODELS_DIR, "hint_label_encoder.pkl")
    meta_path = os.path.join(MODELS_DIR, "hint_metadata.json")
    cm_path = os.path.join(MODELS_DIR, "hint_confusion_matrix.json")

    joblib.dump(winner_model, model_path)
    joblib.dump(scaler, scaler_path)
    joblib.dump(label_encoder, le_path)

    cm_data = {
        "labels": class_names,
        "matrix": cm.tolist()
    }
    with open(cm_path, "w", encoding="utf-8") as f:
        json.dump(cm_data, f, indent=2)

    metadata = {
        "model_type": "hint_recommendation",
        "algorithm": best_name,
        "version": "v1.0",
        "trained_at": datetime.datetime.now().isoformat(),
        "dataset_samples": len(df),
        "features": HINT_FEATURE_COLUMNS,
        "classes": class_names,
        "model_comparison": comparison_results,
        "validation_f1_weighted": round(best_val_f1, 4),
        "test_metrics": {
            "accuracy": round(test_acc, 4),
            "precision_weighted": round(test_prec, 4),
            "recall_weighted": round(test_rec, 4),
            "f1_weighted": round(test_f1, 4)
        },
        "feature_importances": dict(sorted(feature_importances.items(), key=lambda x: x[1], reverse=True)),
        "artifacts": {
            "model_path": "ml/models/hint_model.pkl",
            "scaler_path": "ml/models/hint_scaler.pkl",
            "label_encoder_path": "ml/models/hint_label_encoder.pkl"
        }
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print("\n--- Final Hint Test Set Results ---")
    print(f"Accuracy: {test_acc:.4f} | F1: {test_f1:.4f}")
    return metadata

if __name__ == "__main__":
    train_and_evaluate_hint()
