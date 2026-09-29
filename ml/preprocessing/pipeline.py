"""
Preprocessing Pipeline for Sudoku Player Intelligence.
Cleans raw gameplay session data, checks schema, handles missing values,
and produces the clean player-level feature dataset with a comprehensive report.
"""

import os
import json
import datetime
import pandas as pd
import numpy as np
from ml.features.feature_engineer import build_player_features, FEATURE_COLUMNS

DATA_DIR = os.path.join(os.path.dirname(__file__), "..", "data")
RAW_PATH = os.path.join(DATA_DIR, "raw", "player_gameplay_sessions.csv")
APP_PATH = os.path.join(DATA_DIR, "application", "player_gameplay.csv")
PROCESSED_PATH = os.path.join(DATA_DIR, "processed", "player_dataset.csv")
REPORT_PATH = os.path.join(DATA_DIR, "preprocessing_report.json")

def clean_session_data(df: pd.DataFrame) -> tuple[pd.DataFrame, dict]:
    """
    Cleans raw gameplay records:
    1. Records duplicate rows
    2. Identifies and filters invalid records
    3. Imputes missing accuracy/mistakes/hints
    4. Enforces proper types and value ranges
    """
    raw_rows = len(df)
    duplicate_rows = int(df.duplicated(subset=["game_id"]).sum())
    df = df.drop_duplicates(subset=["game_id"])
    
    # Required columns check
    req_cols = ["player_id", "duration", "moves", "mistakes", "completion_status"]
    missing_required_mask = df[req_cols].isnull().any(axis=1)
    invalid_range_mask = (df["duration"] <= 0) | (df["moves"] < 0) | (df["mistakes"] < 0)
    
    invalid_mask = missing_required_mask | invalid_range_mask
    invalid_rows = int(invalid_mask.sum())
    
    df = df[~invalid_mask].copy()

    # Fill defaults for optional fields
    if "hints" not in df.columns:
        df["hints"] = 0
    else:
        df["hints"] = df["hints"].fillna(0).astype(int)

    if "undos" not in df.columns:
        df["undos"] = 0
    else:
        df["undos"] = df["undos"].fillna(0).astype(int)

    if "accuracy" not in df.columns:
        attempts = df["moves"] + df["mistakes"]
        df["accuracy"] = np.where(attempts > 0, df["moves"] / attempts, 1.0)
    else:
        df["accuracy"] = df["accuracy"].fillna(1.0).clip(0.0, 1.0)

    if "score" not in df.columns:
        df["score"] = 0
    else:
        df["score"] = df["score"].fillna(0).clip(lower=0)

    if "difficulty" not in df.columns:
        df["difficulty"] = "Medium"
    else:
        df["difficulty"] = df["difficulty"].fillna("Medium").astype(str).str.capitalize()

    valid_rows = len(df)
    metrics = {
        "raw_rows": raw_rows,
        "duplicate_rows": duplicate_rows,
        "invalid_rows": invalid_rows,
        "valid_rows": valid_rows
    }
    return df, metrics

def run_preprocessing(hybrid=True) -> pd.DataFrame:
    """
    Loads raw session dataset, merges application telemetry if available,
    cleans the data, runs feature engineering, saves the processed dataset,
    and writes a detailed preprocessing report.
    """
    print("Executing Preprocessing Pipeline...")
    if not os.path.exists(RAW_PATH):
        raise FileNotFoundError(f"Raw data not found at {RAW_PATH}. Run download_dataset.py first.")

    df_raw = pd.read_csv(RAW_PATH)
    initial_raw_count = len(df_raw)
    
    if hybrid and os.path.exists(APP_PATH):
        try:
            df_app = pd.read_csv(APP_PATH)
            existing_ids = set(df_raw["game_id"])
            df_app_new = df_app[~df_app["game_id"].isin(existing_ids)]
            if not df_app_new.empty:
                print(f"Merging {len(df_app_new)} application gameplay records into pipeline.")
                df_raw = pd.concat([df_raw, df_app_new], ignore_index=True)
        except Exception as e:
            print(f"Note: Could not merge application gameplay: {e}")

    cleaned_sessions, clean_metrics = clean_session_data(df_raw)
    player_features_df = build_player_features(cleaned_sessions)
    
    os.makedirs(os.path.dirname(PROCESSED_PATH), exist_ok=True)
    player_features_df.to_csv(PROCESSED_PATH, index=False)
    
    class_dist = player_features_df["skill_level"].value_counts().to_dict()
    
    report = {
        "pipeline_version": "v1.1",
        "dataset_version": "1.1.0",
        "feature_version": "v1.1",
        "timestamp": datetime.datetime.now().isoformat(),
        "raw_rows": initial_raw_count,
        "total_merged_rows": len(df_raw),
        "duplicate_rows": clean_metrics["duplicate_rows"],
        "invalid_rows": clean_metrics["invalid_rows"],
        "valid_session_rows": clean_metrics["valid_rows"],
        "final_player_profiles": len(player_features_df),
        "features": FEATURE_COLUMNS,
        "class_distribution": class_dist
    }
    
    with open(REPORT_PATH, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)
        
    print(f"Successfully processed {len(player_features_df)} player profiles saved to {PROCESSED_PATH}")
    print(f"Preprocessing report written to {REPORT_PATH}")
    print("Class distribution:\n", class_dist)
    return player_features_df

if __name__ == "__main__":
    run_preprocessing()
