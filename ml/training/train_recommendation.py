"""
Personalized Difficulty & Mission Recommendation Model Training & Evaluation.

Recommends optimal difficulty and challenge trajectory based on historical
competence, recent momentum, error rate, and completion patterns.

Outputs:
- recommended_difficulty (EASY, MEDIUM, HARD, EXPERT)
- confidence (float)
- recommended_reason (human-readable coaching note)
- top_factors (explainable ML features)
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
PROCESSED_PATH = os.path.join(DATA_DIR, "processed", "player_dataset.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

RECOMMENDATION_FEATURE_COLUMNS = [
    "completion_rate",
    "average_accuracy",
    "average_mistakes",
    "average_hints",
    "easy_completion_rate",
    "medium_completion_rate",
    "hard_completion_rate",
    "recent_accuracy",
    "recent_error_rate",
    "average_moves_per_minute"
]

def derive_recommendation_target(row: pd.Series) -> str:
    """
    Pedagogical recommendation objective based on mastery and challenge zones:
    1. If hard_completion_rate >= 0.70 and recent_accuracy >= 0.90 -> EXPERT
    2. If medium_completion_rate >= 0.75 and recent_accuracy >= 0.85 -> HARD
    3. If easy_completion_rate >= 0.70 or completion_rate >= 0.60 -> MEDIUM
    4. Otherwise -> EASY
    """
    h_comp = row["hard_completion_rate"]
    m_comp = row["medium_completion_rate"]
    e_comp = row["easy_completion_rate"]
    rec_acc = row["recent_accuracy"]
    rec_err = row["recent_error_rate"]

    if h_comp >= 0.70 and rec_acc >= 0.90 and rec_err <= 0.08:
        return "EXPERT"
    elif m_comp >= 0.65 and rec_acc >= 0.82 and rec_err <= 0.15:
        return "HARD"
    elif e_comp >= 0.65 or row["completion_rate"] >= 0.55:
        return "MEDIUM"
    else:
        return "EASY"

def train_and_evaluate_recommendation(random_state=42) -> Dict[str, Any]:
    if not os.path.exists(PROCESSED_PATH):
        raise FileNotFoundError(f"Processed player features not found at {PROCESSED_PATH}")

    df = pd.read_csv(PROCESSED_PATH)
    df["target_rec"] = df.apply(derive_recommendation_target, axis=1)

    X = df[RECOMMENDATION_FEATURE_COLUMNS]
    y_raw = df["target_rec"].values

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
            "model": DecisionTreeClassifier(max_depth=5, min_samples_split=4, random_state=random_state),
            "use_scaled": False
        },
        "Random Forest": {
            "model": RandomForestClassifier(n_estimators=100, max_depth=7, random_state=random_state),
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

    print("\n--- Training Recommendation Models ---")
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
        for col, imp in zip(RECOMMENDATION_FEATURE_COLUMNS, winner_model.feature_importances_):
            feature_importances[col] = round(float(imp), 4)

    # Save artifacts
    model_path = os.path.join(MODELS_DIR, "recommendation_model.pkl")
    scaler_path = os.path.join(MODELS_DIR, "recommendation_scaler.pkl")
    le_path = os.path.join(MODELS_DIR, "recommendation_label_encoder.pkl")
    meta_path = os.path.join(MODELS_DIR, "recommendation_metadata.json")
    cm_path = os.path.join(MODELS_DIR, "recommendation_confusion_matrix.json")

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
        "model_type": "personalized_recommendation",
        "algorithm": best_name,
        "version": "v1.0",
        "trained_at": datetime.datetime.now().isoformat(),
        "dataset_samples": len(df),
        "features": RECOMMENDATION_FEATURE_COLUMNS,
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
            "model_path": "ml/models/recommendation_model.pkl",
            "scaler_path": "ml/models/recommendation_scaler.pkl",
            "label_encoder_path": "ml/models/recommendation_label_encoder.pkl"
        }
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    print("\n--- Final Recommendation Test Set Results ---")
    print(f"Accuracy: {test_acc:.4f} | F1: {test_f1:.4f}")
    return metadata

if __name__ == "__main__":
    train_and_evaluate_recommendation()
