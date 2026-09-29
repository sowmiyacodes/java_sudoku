"""
Unit and integration tests for Member 1 ML pipeline and FastAPI service.
Tests all 5 models and registry endpoints.
"""

import pytest
import os
import pandas as pd
from fastapi.testclient import TestClient

from ml.features.feature_engineer import derive_skill_label, build_player_features, FEATURE_COLUMNS
from ml.features.puzzle_features import extract_puzzle_features, PUZZLE_FEATURE_COLUMNS
from ml.preprocessing.pipeline import clean_session_data
from ml.api.main import app

client = TestClient(app)

def test_derive_skill_label():
    expert_row = {
        "completion_rate": 0.98,
        "average_accuracy": 0.98,
        "average_mistakes": 0.2,
        "average_hints": 0.1,
        "hard_completion_rate": 0.95,
        "medium_completion_rate": 1.0,
        "average_time": 280
    }
    assert derive_skill_label(expert_row) == "EXPERT"

    beginner_row = {
        "completion_rate": 0.40,
        "average_accuracy": 0.70,
        "average_mistakes": 4.5,
        "average_hints": 4.0,
        "hard_completion_rate": 0.0,
        "medium_completion_rate": 0.30,
        "average_time": 950
    }
    assert derive_skill_label(beginner_row) == "BEGINNER"

def test_clean_session_data():
    raw_df = pd.DataFrame([
        {"game_id": 1, "player_id": 10, "duration": 300, "moves": 40, "mistakes": 1, "completion_status": "COMPLETED"},
        {"game_id": 1, "player_id": 10, "duration": 300, "moves": 40, "mistakes": 1, "completion_status": "COMPLETED"}, # duplicate
        {"game_id": 2, "player_id": 10, "duration": -10, "moves": 10, "mistakes": 0, "completion_status": "ABANDONED"} # invalid duration
    ])
    cleaned, metrics = clean_session_data(raw_df)
    assert len(cleaned) == 1
    assert cleaned.iloc[0]["game_id"] == 1
    assert metrics["duplicate_rows"] == 1
    assert metrics["invalid_rows"] == 1

def test_puzzle_feature_extractor():
    sample_puzzle = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"
    feats = extract_puzzle_features(sample_puzzle)
    for col in PUZZLE_FEATURE_COLUMNS:
        assert col in feats
    assert feats["clue_count"] > 20
    assert feats["empty_cell_count"] < 60

def test_fastapi_health():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"

def test_fastapi_predict_skill():
    payload = {
        "games_played": 15,
        "completion_rate": 0.95,
        "average_time": 250.0,
        "average_score": 850.0,
        "average_accuracy": 0.96,
        "average_mistakes": 0.4,
        "average_hints": 0.1,
        "average_undos": 0.2,
        "average_moves": 45.0,
        "hard_completion_rate": 0.90,
        "medium_completion_rate": 1.0,
        "easy_completion_rate": 1.0,
        "recent_accuracy": 0.98,
        "recent_error_rate": 0.02,
        "average_moves_per_minute": 10.5
    }
    response = client.post("/predict/skill", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["skill_level"] in ["BEGINNER", "INTERMEDIATE", "ADVANCED", "EXPERT"]
    assert 0.0 <= data["confidence"] <= 1.0
    assert len(data["explanation"]["top_factors"]) > 0

def test_fastapi_predict_difficulty():
    payload = {"puzzle": "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"}
    response = client.post("/predict/difficulty", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["difficulty"] in ["EASY", "MEDIUM", "HARD", "EXPERT"]
    assert "clue_count" in data["extracted_features"]
    assert len(data["top_factors"]) > 0

def test_fastapi_predict_completion():
    payload = {
        "skill_level": "ADVANCED",
        "difficulty": "MEDIUM",
        "historical_completion_rate": 0.88,
        "average_solving_time": 320.0,
        "recent_accuracy": 0.94,
        "hints_used": 0,
        "mistakes_made": 1,
        "current_streak": 4,
        "current_progress": 0.65,
        "elapsed_time": 180.0
    }
    response = client.post("/predict/completion", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["completion_probability"] <= 1.0
    assert isinstance(data["predicted_completion"], bool)

def test_fastapi_predict_hint():
    payload = {
        "player_skill": "INTERMEDIATE",
        "difficulty": "HARD",
        "current_progress": 0.40,
        "mistakes_count": 2,
        "elapsed_time": 300.0,
        "previous_hints_used": 1,
        "stuck_duration_seconds": 120.0
    }
    response = client.post("/predict/hint", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["hint_type"] in ["CELL", "ROW", "COLUMN", "REGION", "TECHNIQUE", "NEXT_MOVE"]
    assert len(data["reason"]) > 0

def test_fastapi_predict_recommendation():
    payload = {
        "completion_rate": 0.85,
        "average_accuracy": 0.90,
        "average_mistakes": 1.2,
        "average_hints": 0.4,
        "easy_completion_rate": 1.0,
        "medium_completion_rate": 0.90,
        "hard_completion_rate": 0.65,
        "recent_accuracy": 0.92,
        "recent_error_rate": 0.06,
        "average_moves_per_minute": 8.0
    }
    response = client.post("/predict/recommendation", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["recommended_difficulty"] in ["EASY", "MEDIUM", "HARD", "EXPERT"]
    assert len(data["recommended_reason"]) > 0

def test_fastapi_model_registry_and_experiments():
    res_models = client.get("/ml/models")
    assert res_models.status_code == 200
    assert isinstance(res_models.json(), list)

    res_exps = client.get("/ml/experiments")
    assert res_exps.status_code == 200
    assert isinstance(res_exps.json(), list)

    res_ds = client.get("/ml/datasets")
    assert res_ds.status_code == 200
    data = res_ds.json()
    assert "manifest" in data
