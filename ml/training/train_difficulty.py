"""
Sudoku Puzzle Difficulty Classification Model Training & Evaluation.

Trains models to classify Sudoku puzzles into:
- EASY
- MEDIUM
- HARD
- EXPERT

Using genuine structural, candidate, and branching features extracted from puzzles.
Evaluates: Logistic Regression, Decision Tree, Random Forest, Gradient Boosting.
Selects best model via Validation Weighted F1, and performs final test evaluation.
"""

import os
import json
import joblib
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

from ml.features.puzzle_features import extract_puzzle_features, normalize_puzzle_string, PUZZLE_FEATURE_COLUMNS

BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.join(BASE_DIR, "..", "data")
MODELS_DIR = os.path.join(BASE_DIR, "..", "models")
BENCHMARK_PATH = os.path.join(DATA_DIR, "raw", "puzzles_benchmark.csv")
HUMAN_PATH = os.path.join(DATA_DIR, "raw", "cloud_sudoku_human.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

def build_puzzle_dataset() -> pd.DataFrame:
    """
    Assembles puzzle dataset from benchmark bank and human empirical dataset.
    Extracts 13 mathematical puzzle features per puzzle.
    """
    rows = []
    seen_puzzles = set()
    
    if os.path.exists(BENCHMARK_PATH):
        df_bench = pd.read_csv(BENCHMARK_PATH)
        print(f"Loading {len(df_bench)} benchmark puzzles...")
        for _, row in df_bench.iterrows():
            diff = str(row["difficulty"]).upper()
            if diff in ["EASY", "MEDIUM", "HARD", "EXPERT"]:
                try:
                    puzzle_str = normalize_puzzle_string(str(row["puzzle"]))
                except ValueError:
                    continue
                if puzzle_str in seen_puzzles:
                    continue
                seen_puzzles.add(puzzle_str)
                feats = extract_puzzle_features(puzzle_str)
                feats["difficulty"] = diff
                feats["source"] = "SudokuExchange"
                rows.append(feats)

    if os.path.exists(HUMAN_PATH):
        df_human = pd.read_csv(HUMAN_PATH)
        print(f"Loading {len(df_human)} empirical human puzzles...")
        for _, row in df_human.iterrows():
            try:
                puzzle_str = normalize_puzzle_string(str(row["Sudoku Puzzle"]))
            except ValueError:
                continue
            if puzzle_str in seen_puzzles:
                continue
            seen_puzzles.add(puzzle_str)
            d_tr = float(row["D_TR"]) if pd.notnull(row["D_TR"]) else 1.0
            # Map empirical human difficulty metric D_TR to category
            if d_tr < 1.10:
                diff = "EASY"
            elif d_tr < 1.45:
                diff = "MEDIUM"
            elif d_tr < 2.0:
                diff = "HARD"
            else:
                diff = "EXPERT"
            feats = extract_puzzle_features(puzzle_str)
            feats["difficulty"] = diff
            feats["source"] = "IEEE_Human"
            rows.append(feats)

    df_full = pd.DataFrame(rows)
    print(f"Total puzzle dataset assembled: {len(df_full)} samples.")
    if not df_full.empty:
        print("Class distribution:\n", df_full["difficulty"].value_counts())
    return df_full

def train_and_evaluate_difficulty(random_state=42) -> Dict[str, Any]:
    df = build_puzzle_dataset()
    if df.empty:
        raise ValueError("No puzzle data available for difficulty training.")

    X = df[PUZZLE_FEATURE_COLUMNS]
    y_raw = df["difficulty"].values

    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(y_raw)

    # 70% Train, 15% Validation, 15% Test (Stratified)
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

    # Model candidates
    candidates = {
        "Logistic Regression": {
            "model": LogisticRegression(max_iter=1000, random_state=random_state, C=1.0),
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

    print("\n--- Training Difficulty Classification Models ---")
    for name, c in candidates.items():
        clf = c["model"]
        X_tr = X_train_scaled if c["use_scaled"] else X_train
        X_v = X_val_scaled if c["use_scaled"] else X_val

        clf.fit(X_tr, y_train)
        y_val_pred = clf.predict(X_v)

        val_acc = float(accuracy_score(y_val, y_val_pred))
        val_f1_w = float(f1_score(y_val, y_val_pred, average="weighted", zero_division=0))
        val_f1_m = float(f1_score(y_val, y_val_pred, average="macro", zero_division=0))
        val_prec = float(precision_score(y_val, y_val_pred, average="weighted", zero_division=0))
        val_rec = float(recall_score(y_val, y_val_pred, average="weighted", zero_division=0))

        comparison_results[name] = {
            "val_accuracy": round(val_acc, 4),
            "val_f1_weighted": round(val_f1_w, 4),
            "val_f1_macro": round(val_f1_m, 4),
            "val_precision": round(val_prec, 4),
            "val_recall": round(val_rec, 4)
        }
        print(f"[{name:20}] Val Acc: {val_acc:.4f} | Val F1 (Weighted): {val_f1_w:.4f} | Val F1 (Macro): {val_f1_m:.4f}")

        if val_f1_w > best_val_f1:
            best_val_f1 = val_f1_w
            best_name = name
            best_candidate_obj = c

    print(f"\nWinner: {best_name} (Val F1: {best_val_f1:.4f})")
    winner_model = best_candidate_obj["model"]
    winner_scaled = best_candidate_obj["use_scaled"]

    # Final Test Set Evaluation (Evaluated ONLY ONCE)
    X_te = X_test_scaled if winner_scaled else X_test
    y_test_pred = winner_model.predict(X_te)

    test_acc = float(accuracy_score(y_test, y_test_pred))
    test_prec = float(precision_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_rec = float(recall_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_f1_w = float(f1_score(y_test, y_test_pred, average="weighted", zero_division=0))
    test_f1_m = float(f1_score(y_test, y_test_pred, average="macro", zero_division=0))

    cm = confusion_matrix(y_test, y_test_pred)
    class_names = [str(cls) for cls in label_encoder.classes_]

    # Feature importances
    feature_importances = {}
    if hasattr(winner_model, "feature_importances_"):
        for col, imp in zip(PUZZLE_FEATURE_COLUMNS, winner_model.feature_importances_):
            feature_importances[col] = round(float(imp), 4)
    elif hasattr(winner_model, "coef_"):
        mean_coef = np.mean(np.abs(winner_model.coef_), axis=0)
        for col, imp in zip(PUZZLE_FEATURE_COLUMNS, mean_coef):
            feature_importances[col] = round(float(imp), 4)

    # Save artifacts
    model_path = os.path.join(MODELS_DIR, "difficulty_model.pkl")
    scaler_path = os.path.join(MODELS_DIR, "difficulty_scaler.pkl")
    le_path = os.path.join(MODELS_DIR, "difficulty_label_encoder.pkl")
    meta_path = os.path.join(MODELS_DIR, "difficulty_metadata.json")
    cm_path = os.path.join(MODELS_DIR, "difficulty_confusion_matrix.json")

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
        "model_type": "puzzle_difficulty",
        "algorithm": best_name,
        "version": "v1.0",
        "trained_at": datetime.datetime.now().isoformat(),
        "dataset_samples": len(df),
        "features": PUZZLE_FEATURE_COLUMNS,
        "classes": class_names,
        "hyperparameters": winner_model.get_params(),
        "model_comparison": comparison_results,
        "validation_f1_weighted": round(best_val_f1, 4),
        "test_metrics": {
            "accuracy": round(test_acc, 4),
            "precision_weighted": round(test_prec, 4),
            "recall_weighted": round(test_rec, 4),
            "f1_weighted": round(test_f1_w, 4),
            "f1_macro": round(test_f1_m, 4)
        },
        "feature_importances": dict(sorted(feature_importances.items(), key=lambda x: x[1], reverse=True)),
        "artifacts": {
            "model_path": "ml/models/difficulty_model.pkl",
            "scaler_path": "ml/models/difficulty_scaler.pkl",
            "label_encoder_path": "ml/models/difficulty_label_encoder.pkl"
        }
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print("\n--- Final Test Set Results ---")
    print(f"Accuracy:  {test_acc:.4f}")
    print(f"Precision: {test_prec:.4f}")
    print(f"Recall:    {test_rec:.4f}")
    print(f"F1 (W):    {test_f1_w:.4f}")
    print(f"F1 (M):    {test_f1_m:.4f}")
    print("Saved difficulty model artifacts successfully.")
    return metadata

if __name__ == "__main__":
    train_and_evaluate_difficulty()
