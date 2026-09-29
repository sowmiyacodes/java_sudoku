"""
Model Training and Comparison Pipeline for Sudoku Player Intelligence.

Trains multiple classification algorithms:
1. Logistic Regression
2. Decision Tree
3. Random Forest
4. Gradient Boosting

Evaluates on a 70% Train, 15% Validation, 15% Test split with stratification.
Selects the best model based on validation F1 score, evaluates once on the test set,
and saves model artifacts + metadata.
"""

import os
import json
import joblib
import pandas as pd
import numpy as np
from datetime import datetime

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.metrics import (
    accuracy_score,
    precision_score,
    recall_score,
    f1_score,
    classification_report,
    confusion_matrix
)

from ml.features.feature_engineer import FEATURE_COLUMNS
from ml.preprocessing.pipeline import run_preprocessing

MODELS_DIR = os.path.join(os.path.dirname(__file__), "..", "models")
DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "data")
PROCESSED_PATH = os.path.join(DATA_DIR, "processed", "player_dataset.csv")

os.makedirs(MODELS_DIR, exist_ok=True)

def train_and_evaluate(random_state=42):
    print("=" * 60)
    print("Starting Model Training & Comparison Pipeline (Member 1)")
    print("=" * 60)

    # 1. Ensure dataset exists
    if not os.path.exists(PROCESSED_PATH):
        print("Processed dataset missing. Running preprocessing...")
        df = run_preprocessing()
    else:
        df = pd.read_csv(PROCESSED_PATH)

    print(f"Loaded dataset: {len(df)} player records.")

    # 2. Extract features and target
    X = df[FEATURE_COLUMNS].copy()
    y_raw = df["skill_level"].values

    # Check for NaN / infinities
    X = X.fillna(X.median(numeric_only=True))

    # 3. Label encoding
    label_encoder = LabelEncoder()
    # Ensure ordered categories
    classes = ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]
    label_encoder.fit(classes)
    y = label_encoder.transform(y_raw)

    # 4. Train / Validation / Test Split: 70% / 15% / 15%
    X_train_val, X_test, y_train_val, y_test = train_test_split(
        X, y, test_size=0.15, random_state=random_state, stratify=y
    )
    # 0.17647 of 85% is ~15% of original (15/85 = 0.17647)
    X_train, X_val, y_train, y_val = train_test_split(
        X_train_val, y_train_val, test_size=0.17647, random_state=random_state, stratify=y_train_val
    )

    print(f"Splits -> Train: {len(X_train)} (70%), Val: {len(X_val)} (15%), Test: {len(X_test)} (15%)")

    # 5. Feature Scaling (fit strictly on train)
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_val_scaled = scaler.transform(X_val)
    X_test_scaled = scaler.transform(X_test)

    # 6. Candidate Models
    candidates = {
        "Logistic Regression": {
            "model": LogisticRegression(max_iter=1000, random_state=random_state, C=1.0),
            "use_scaled": True
        },
        "Decision Tree": {
            "model": DecisionTreeClassifier(max_depth=5, min_samples_split=5, random_state=random_state),
            "use_scaled": False
        },
        "Random Forest": {
            "model": RandomForestClassifier(n_estimators=100, max_depth=6, min_samples_split=4, random_state=random_state),
            "use_scaled": False
        },
        "Gradient Boosting": {
            "model": GradientBoostingClassifier(n_estimators=80, learning_rate=0.1, max_depth=4, random_state=random_state),
            "use_scaled": False
        }
    }

    results = []
    trained_instances = {}

    print("\n--- Evaluating Candidate Models on Validation Set ---")
    for name, config in candidates.items():
        clf = config["model"]
        use_scaled = config["use_scaled"]

        X_tr = X_train_scaled if use_scaled else X_train
        X_v = X_val_scaled if use_scaled else X_val

        # Train
        clf.fit(X_tr, y_train)
        trained_instances[name] = clf

        # Predict Validation
        y_val_pred = clf.predict(X_v)

        val_acc = accuracy_score(y_val, y_val_pred)
        val_prec = precision_score(y_val, y_val_pred, average="weighted", zero_division=0)
        val_rec = recall_score(y_val, y_val_pred, average="weighted", zero_division=0)
        val_f1 = f1_score(y_val, y_val_pred, average="weighted", zero_division=0)
        val_f1_macro = f1_score(y_val, y_val_pred, average="macro", zero_division=0)

        results.append({
            "model_name": name,
            "val_accuracy": round(float(val_acc), 4),
            "val_precision": round(float(val_prec), 4),
            "val_recall": round(float(val_rec), 4),
            "val_f1": round(float(val_f1), 4),
            "val_f1_macro": round(float(val_f1_macro), 4)
        })

        print(f"[{name:20s}] Val Acc: {val_acc:.4f} | Val F1 (weighted): {val_f1:.4f} | Val F1 (macro): {val_f1_macro:.4f}")

    # 7. Select Best Model based on Validation F1
    df_results = pd.DataFrame(results).sort_values(by="val_f1", ascending=False)
    best_candidate_row = df_results.iloc[0]
    best_name = best_candidate_row["model_name"]
    best_model = trained_instances[best_name]
    best_config = candidates[best_name]

    print(f"\n>>> Selected Winning Model: {best_name} (Validation F1: {best_candidate_row['val_f1']})")

    # 8. Evaluate Winning Model ONCE on the Untouched Test Set
    X_te = X_test_scaled if best_config["use_scaled"] else X_test
    y_test_pred = best_model.predict(X_te)
    y_test_proba = best_model.predict_proba(X_te) if hasattr(best_model, "predict_proba") else None

    test_acc = accuracy_score(y_test, y_test_pred)
    test_prec = precision_score(y_test, y_test_pred, average="weighted", zero_division=0)
    test_rec = recall_score(y_test, y_test_pred, average="weighted", zero_division=0)
    test_f1 = f1_score(y_test, y_test_pred, average="weighted", zero_division=0)
    test_f1_macro = f1_score(y_test, y_test_pred, average="macro", zero_division=0)

    print("\n--- Final Evaluation on Test Set ---")
    print(f"Test Accuracy  : {test_acc:.4f}")
    print(f"Test Precision : {test_prec:.4f}")
    print(f"Test Recall    : {test_rec:.4f}")
    print(f"Test F1 Score  : {test_f1:.4f}")
    print(f"Test F1 (macro): {test_f1_macro:.4f}")

    target_names = [label_encoder.inverse_transform([i])[0] for i in range(len(label_encoder.classes_))]
    report_str = classification_report(y_test, y_test_pred, target_names=target_names)
    print("\nClassification Report:\n", report_str)

    conf_mat = confusion_matrix(y_test, y_test_pred)
    print("Confusion Matrix:\n", conf_mat)

    # 9. Feature Importance / Coefficients
    importance_dict = {}
    if hasattr(best_model, "feature_importances_"):
        imps = best_model.feature_importances_
        for f, imp in sorted(zip(FEATURE_COLUMNS, imps), key=lambda x: x[1], reverse=True):
            importance_dict[f] = round(float(imp), 4)
    elif hasattr(best_model, "coef_"):
        # Mean absolute coefficient across multiclass outputs
        coef_abs = np.mean(np.abs(best_model.coef_), axis=0)
        for f, val in sorted(zip(FEATURE_COLUMNS, coef_abs), key=lambda x: x[1], reverse=True):
            importance_dict[f] = round(float(val), 4)

    # 10. Save Artifacts
    algo_slug = best_name.lower().replace(" ", "_")
    model_filename = f"player_skill_{algo_slug}_v1.pkl"
    primary_model_path = os.path.join(MODELS_DIR, model_filename)
    best_alias_path = os.path.join(MODELS_DIR, "best_skill_model.pkl")

    joblib.dump(best_model, primary_model_path)
    joblib.dump(best_model, best_alias_path)
    joblib.dump(scaler, os.path.join(MODELS_DIR, "scaler.pkl"))
    joblib.dump(label_encoder, os.path.join(MODELS_DIR, "label_encoder.pkl"))

    # Metadata
    metadata = {
        "model_name": "Player Skill Classifier",
        "version": "v1",
        "algorithm": best_name,
        "model_file": model_filename,
        "dataset": "Grantm Sudoku Bank + IEEE Cloud Sudoku Human Gameplay + App Telemetry",
        "dataset_version": "v1.0",
        "features": FEATURE_COLUMNS,
        "feature_importance": importance_dict,
        "training_date": datetime.now().isoformat(),
        "train_size": len(X_train),
        "validation_size": len(X_val),
        "test_size": len(X_test),
        "random_state": random_state,
        "validation_metrics": {
            "accuracy": float(best_candidate_row["val_accuracy"]),
            "precision": float(best_candidate_row["val_precision"]),
            "recall": float(best_candidate_row["val_recall"]),
            "f1_weighted": float(best_candidate_row["val_f1"]),
            "f1_macro": float(best_candidate_row["val_f1_macro"])
        },
        "test_metrics": {
            "accuracy": round(float(test_acc), 4),
            "precision": round(float(test_prec), 4),
            "recall": round(float(test_rec), 4),
            "f1_weighted": round(float(test_f1), 4),
            "f1_macro": round(float(test_f1_macro), 4)
        },
        "all_model_comparisons": results,
        "confusion_matrix": conf_mat.tolist(),
        "classes": target_names
    }

    metadata_path = os.path.join(MODELS_DIR, "model_metadata.json")
    with open(metadata_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)

    # Save confusion matrix text artifact
    with open(os.path.join(MODELS_DIR, "confusion_matrix.json"), "w", encoding="utf-8") as f:
        json.dump({"classes": target_names, "matrix": conf_mat.tolist()}, f, indent=2)

    print(f"\nArtifacts successfully saved to {MODELS_DIR}")
    return metadata

if __name__ == "__main__":
    train_and_evaluate()
