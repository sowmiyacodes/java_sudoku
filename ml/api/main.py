"""
FastAPI Microservice for Sudoku Machine Learning Suite (Member 1).

Serves all 5 ML models:
1. Skill Classification (/predict/skill, /train/skill)
2. Puzzle Difficulty (/predict/difficulty, /train/difficulty)
3. Puzzle Completion (/predict/completion, /train/completion)
4. Hint Pedagogical Recommendation (/predict/hint, /train/hint)
5. Personalized Mission Recommendation (/predict/recommendation, /train/recommendation)

Registry & Operations Endpoints:
- GET /ml/models, GET /ml/models/{id}, POST /ml/models/{id}/activate
- GET /ml/experiments
- GET /ml/datasets
- GET /ml/predictions
- GET /ml/health
- GET /metadata
"""

import os
import json
import joblib
import pandas as pd
import numpy as np
from typing import List, Dict, Any, Optional
from fastapi import FastAPI, HTTPException, Path
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from ml.features.feature_engineer import FEATURE_COLUMNS
from ml.features.puzzle_features import extract_puzzle_features, PUZZLE_FEATURE_COLUMNS
from ml.training.train_skill import train_and_evaluate as train_skill
from ml.training.train_difficulty import train_and_evaluate_difficulty as train_difficulty
from ml.training.train_completion import train_and_evaluate_completion as train_completion, COMPLETION_FEATURE_COLUMNS
from ml.training.train_hint import train_and_evaluate_hint as train_hint, HINT_FEATURE_COLUMNS
from ml.training.train_recommendation import train_and_evaluate_recommendation as train_recommendation, RECOMMENDATION_FEATURE_COLUMNS

from ml.registry.model_registry import (
    register_model_run,
    get_registered_models,
    get_model_by_id,
    activate_model,
    get_experiment_history,
    log_prediction_event,
    get_recent_predictions
)

BASE_DIR = os.path.dirname(__file__)
MODELS_DIR = os.path.join(BASE_DIR, "..", "models")
DATA_DIR = os.path.join(BASE_DIR, "..", "data")

app = FastAPI(
    title="Sudoku Machine Learning Intelligence API",
    description="Member 1 — Multi-Model AI Service & Operational ML Registry",
    version="1.2.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ----------------- In-Memory Artifact Store -----------------
artifacts: Dict[str, Any] = {
    "skill": {"model": None, "scaler": None, "le": None, "meta": None},
    "difficulty": {"model": None, "scaler": None, "le": None, "meta": None},
    "completion": {"model": None, "scaler": None, "meta": None},
    "hint": {"model": None, "scaler": None, "le": None, "meta": None},
    "recommendation": {"model": None, "scaler": None, "le": None, "meta": None}
}

def load_all_artifacts():
    # 1. Skill
    p_m = os.path.join(MODELS_DIR, "best_skill_model.pkl")
    p_s = os.path.join(MODELS_DIR, "scaler.pkl")
    p_l = os.path.join(MODELS_DIR, "label_encoder.pkl")
    p_meta = os.path.join(MODELS_DIR, "model_metadata.json")
    if os.path.exists(p_m) and os.path.exists(p_l):
        artifacts["skill"]["model"] = joblib.load(p_m)
        artifacts["skill"]["le"] = joblib.load(p_l)
        if os.path.exists(p_s):
            artifacts["skill"]["scaler"] = joblib.load(p_s)
        if os.path.exists(p_meta):
            with open(p_meta, "r", encoding="utf-8") as f:
                artifacts["skill"]["meta"] = json.load(f)

    # 2. Difficulty
    p_dm = os.path.join(MODELS_DIR, "difficulty_model.pkl")
    p_ds = os.path.join(MODELS_DIR, "difficulty_scaler.pkl")
    p_dl = os.path.join(MODELS_DIR, "difficulty_label_encoder.pkl")
    p_dmeta = os.path.join(MODELS_DIR, "difficulty_metadata.json")
    if os.path.exists(p_dm) and os.path.exists(p_dl):
        artifacts["difficulty"]["model"] = joblib.load(p_dm)
        artifacts["difficulty"]["le"] = joblib.load(p_dl)
        if os.path.exists(p_ds):
            artifacts["difficulty"]["scaler"] = joblib.load(p_ds)
        if os.path.exists(p_dmeta):
            with open(p_dmeta, "r", encoding="utf-8") as f:
                artifacts["difficulty"]["meta"] = json.load(f)

    # 3. Completion
    p_cm = os.path.join(MODELS_DIR, "completion_model.pkl")
    p_cs = os.path.join(MODELS_DIR, "completion_scaler.pkl")
    p_cmeta = os.path.join(MODELS_DIR, "completion_metadata.json")
    if os.path.exists(p_cm):
        artifacts["completion"]["model"] = joblib.load(p_cm)
        if os.path.exists(p_cs):
            artifacts["completion"]["scaler"] = joblib.load(p_cs)
        if os.path.exists(p_cmeta):
            with open(p_cmeta, "r", encoding="utf-8") as f:
                artifacts["completion"]["meta"] = json.load(f)

    # 4. Hint
    p_hm = os.path.join(MODELS_DIR, "hint_model.pkl")
    p_hs = os.path.join(MODELS_DIR, "hint_scaler.pkl")
    p_hl = os.path.join(MODELS_DIR, "hint_label_encoder.pkl")
    p_hmeta = os.path.join(MODELS_DIR, "hint_metadata.json")
    if os.path.exists(p_hm) and os.path.exists(p_hl):
        artifacts["hint"]["model"] = joblib.load(p_hm)
        artifacts["hint"]["le"] = joblib.load(p_hl)
        if os.path.exists(p_hs):
            artifacts["hint"]["scaler"] = joblib.load(p_hs)
        if os.path.exists(p_hmeta):
            with open(p_hmeta, "r", encoding="utf-8") as f:
                artifacts["hint"]["meta"] = json.load(f)

    # 5. Recommendation
    p_rm = os.path.join(MODELS_DIR, "recommendation_model.pkl")
    p_rs = os.path.join(MODELS_DIR, "recommendation_scaler.pkl")
    p_rl = os.path.join(MODELS_DIR, "recommendation_label_encoder.pkl")
    p_rmeta = os.path.join(MODELS_DIR, "recommendation_metadata.json")
    if os.path.exists(p_rm) and os.path.exists(p_rl):
        artifacts["recommendation"]["model"] = joblib.load(p_rm)
        artifacts["recommendation"]["le"] = joblib.load(p_rl)
        if os.path.exists(p_rs):
            artifacts["recommendation"]["scaler"] = joblib.load(p_rs)
        if os.path.exists(p_rmeta):
            with open(p_rmeta, "r", encoding="utf-8") as f:
                artifacts["recommendation"]["meta"] = json.load(f)

load_all_artifacts()

# ----------------- Request / Response Models -----------------

class Explanation(BaseModel):
    top_factors: List[str] = Field(default_factory=list)
    feature_contributions: Optional[Dict[str, float]] = None

# 1. Skill
class SkillPredictionRequest(BaseModel):
    games_played: int = Field(default=1, ge=0)
    completion_rate: float = Field(default=1.0, ge=0.0, le=1.0)
    average_time: float = Field(default=300.0, ge=0.0)
    average_score: float = Field(default=500.0, ge=0.0)
    average_accuracy: float = Field(default=0.90, ge=0.0, le=1.0)
    average_mistakes: float = Field(default=1.0, ge=0.0)
    average_hints: float = Field(default=0.0, ge=0.0)
    average_undos: float = Field(default=0.0, ge=0.0)
    average_moves: float = Field(default=40.0, ge=0.0)
    hard_completion_rate: float = Field(default=0.0, ge=0.0, le=1.0)
    medium_completion_rate: float = Field(default=0.0, ge=0.0, le=1.0)
    easy_completion_rate: float = Field(default=1.0, ge=0.0, le=1.0)
    recent_accuracy: float = Field(default=0.90, ge=0.0, le=1.0)
    recent_error_rate: float = Field(default=0.05, ge=0.0, le=1.0)
    average_moves_per_minute: float = Field(default=8.0, ge=0.0)

class SkillPredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}
    skill_level: str
    confidence: float
    model_version: str
    explanation: Explanation

# 2. Difficulty
class DifficultyPredictionRequest(BaseModel):
    puzzle: str = Field(..., description="81-character Sudoku puzzle string")

class DifficultyPredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}
    difficulty: str
    confidence: float
    model_version: str
    extracted_features: Dict[str, float]
    top_factors: List[str]

# 3. Completion
class CompletionPredictionRequest(BaseModel):
    skill_level: str = Field(default="INTERMEDIATE")
    difficulty: str = Field(default="MEDIUM")
    historical_completion_rate: float = Field(default=0.75, ge=0.0, le=1.0)
    average_solving_time: float = Field(default=450.0, ge=0.0)
    recent_accuracy: float = Field(default=0.88, ge=0.0, le=1.0)
    hints_used: int = Field(default=0, ge=0)
    mistakes_made: int = Field(default=0, ge=0)
    current_streak: int = Field(default=2, ge=0)
    current_progress: float = Field(default=0.50, ge=0.0, le=1.0)
    elapsed_time: float = Field(default=240.0, ge=0.0)

class CompletionPredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}
    completion_probability: float
    predicted_completion: bool
    confidence: float
    top_factors: List[str]
    model_version: str = "v1.0"

# 4. Hint
class HintPredictionRequest(BaseModel):
    player_skill: str = Field(default="INTERMEDIATE")
    difficulty: str = Field(default="MEDIUM")
    current_progress: float = Field(default=0.45, ge=0.0, le=1.0)
    mistakes_count: int = Field(default=1, ge=0)
    elapsed_time: float = Field(default=180.0, ge=0.0)
    previous_hints_used: int = Field(default=0, ge=0)
    stuck_duration_seconds: float = Field(default=45.0, ge=0.0)

class HintPredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}
    hint_type: str
    confidence: float
    reason: str
    target_cell: Optional[Dict[str, int]] = None
    technique: Optional[str] = None
    top_factors: List[str]

# 5. Recommendation
class RecommendationPredictionRequest(BaseModel):
    completion_rate: float = Field(default=0.80, ge=0.0, le=1.0)
    average_accuracy: float = Field(default=0.92, ge=0.0, le=1.0)
    average_mistakes: float = Field(default=1.0, ge=0.0)
    average_hints: float = Field(default=0.5, ge=0.0)
    easy_completion_rate: float = Field(default=1.0, ge=0.0, le=1.0)
    medium_completion_rate: float = Field(default=0.85, ge=0.0, le=1.0)
    hard_completion_rate: float = Field(default=0.50, ge=0.0, le=1.0)
    recent_accuracy: float = Field(default=0.95, ge=0.0, le=1.0)
    recent_error_rate: float = Field(default=0.04, ge=0.0, le=1.0)
    average_moves_per_minute: float = Field(default=8.5, ge=0.0)

class RecommendationPredictionResponse(BaseModel):
    model_config = {"protected_namespaces": ()}
    recommended_difficulty: str
    recommended_reason: str
    confidence: float
    top_factors: List[str]

# ==================== PREDICTION ENDPOINTS ====================

@app.post("/predict/skill", response_model=SkillPredictionResponse)
def predict_skill(request: SkillPredictionRequest):
    model = artifacts["skill"]["model"]
    le = artifacts["skill"]["le"]
    meta = artifacts["skill"]["meta"]
    if model is None or le is None:
        load_all_artifacts()
        model = artifacts["skill"]["model"]
        le = artifacts["skill"]["le"]
        if model is None or le is None:
            raise HTTPException(status_code=503, detail="Skill model not ready.")

    row_dict = request.model_dump()
    feature_values = [row_dict[col] for col in FEATURE_COLUMNS]
    X_input = pd.DataFrame([feature_values], columns=FEATURE_COLUMNS)

    y_pred = model.predict(X_input)
    pred_idx = int(y_pred[0])
    label = le.inverse_transform([pred_idx])[0]

    confidence = 0.85
    if hasattr(model, "predict_proba"):
        probs = model.predict_proba(X_input)[0]
        confidence = max(0.50, round(float(probs[pred_idx]), 2))

    top_factors = []
    if request.completion_rate >= 0.80:
        top_factors.append(f"Important model factor: High solve consistency ({int(request.completion_rate * 100)}%)")
    if request.average_accuracy >= 0.90:
        top_factors.append(f"Important model factor: Exceptional input accuracy ({int(request.average_accuracy * 100)}%)")
    if request.hard_completion_rate >= 0.60:
        top_factors.append("Important model factor: Demonstrated Hard difficulty competence")
    if request.average_hints <= 1.0:
        top_factors.append("Important model factor: Independent solving with minimal hint usage")
    if not top_factors:
        top_factors.append("Important model factor: Balanced baseline telemetry performance")

    v_str = meta.get("version", "v1.1") if meta else "v1.1"
    response = SkillPredictionResponse(
        skill_level=label,
        confidence=confidence,
        model_version=v_str,
        explanation=Explanation(top_factors=top_factors)
    )
    log_prediction_event("SKILL", request.model_dump(), response.model_dump())
    return response

@app.post("/predict/difficulty", response_model=DifficultyPredictionResponse)
def predict_difficulty(request: DifficultyPredictionRequest):
    model = artifacts["difficulty"]["model"]
    le = artifacts["difficulty"]["le"]
    scaler = artifacts["difficulty"]["scaler"]
    meta = artifacts["difficulty"]["meta"]
    if model is None or le is None:
        load_all_artifacts()
        model = artifacts["difficulty"]["model"]
        le = artifacts["difficulty"]["le"]
        scaler = artifacts["difficulty"]["scaler"]
        if model is None or le is None:
            raise HTTPException(status_code=503, detail="Difficulty model not ready.")

    feats = extract_puzzle_features(request.puzzle)
    feat_values = [feats[col] for col in PUZZLE_FEATURE_COLUMNS]
    X_input = pd.DataFrame([feat_values], columns=PUZZLE_FEATURE_COLUMNS)
    X_proc = scaler.transform(X_input) if scaler is not None else X_input

    y_pred = model.predict(X_proc)
    pred_idx = int(y_pred[0])
    label = le.inverse_transform([pred_idx])[0]

    confidence = 0.80
    if hasattr(model, "predict_proba"):
        probs = model.predict_proba(X_proc)[0]
        confidence = max(0.50, round(float(probs[pred_idx]), 2))

    top_factors = [
        f"Important model factor: Clue count is {int(feats['clue_count'])} givens ({int(feats['empty_cell_count'])} empty cells)",
        f"Important model factor: Branching complexity index of {feats['branching_factor_estimate']:.2f}",
        f"Important model factor: Available naked singles ({int(feats['single_candidate_cells'])})"
    ]

    response = DifficultyPredictionResponse(
        difficulty=label,
        confidence=confidence,
        model_version=meta.get("version", "v1.0") if meta else "v1.0",
        extracted_features=feats,
        top_factors=top_factors
    )
    log_prediction_event("DIFFICULTY", {"clues": feats["clue_count"]}, response.model_dump())
    return response

@app.post("/predict/completion", response_model=CompletionPredictionResponse)
def predict_completion(request: CompletionPredictionRequest):
    model = artifacts["completion"]["model"]
    scaler = artifacts["completion"]["scaler"]
    metadata = artifacts["completion"]["meta"] or {}
    if model is None:
        load_all_artifacts()
        model = artifacts["completion"]["model"]
        scaler = artifacts["completion"]["scaler"]
        metadata = artifacts["completion"]["meta"] or {}
        if model is None:
            raise HTTPException(status_code=503, detail="Completion model not ready.")

    skill_map = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3, "EXPERT": 4}
    diff_map = {"EASY": 1, "MEDIUM": 2, "HARD": 3, "EXPERT": 4}
    s_num = skill_map.get(request.skill_level.upper(), 2)
    d_num = diff_map.get(request.difficulty.upper(), 2)
    elapsed_ratio = request.elapsed_time / max(60.0, 300.0 * d_num)

    row = [
        float(s_num),
        float(d_num),
        request.historical_completion_rate,
        request.average_solving_time,
        request.recent_accuracy,
        float(request.current_streak),
        request.current_progress,
        round(elapsed_ratio, 4),
        float(d_num * 1.5),
    ]
    X_input = pd.DataFrame([row], columns=COMPLETION_FEATURE_COLUMNS)
    X_proc = X_input
    if scaler is not None and metadata.get("uses_scaler", True):
        X_proc = pd.DataFrame(
            scaler.transform(X_input),
            columns=COMPLETION_FEATURE_COLUMNS,
            index=X_input.index,
        )

    prob = 0.85
    pred_completed = True
    if hasattr(model, "predict_proba"):
        probs = model.predict_proba(X_proc)[0]
        prob = round(float(probs[1]), 2)
        pred_completed = bool(prob >= 0.50)
    else:
        pred_completed = bool(model.predict(X_proc)[0] == 1)
        prob = 0.90 if pred_completed else 0.20

    top_factors = [
        f"Player historical completion rate ({int(request.historical_completion_rate * 100)}%)",
        f"Current puzzle progression ({int(request.current_progress * 100)}% filled)",
        f"Elapsed time relative to the expected difficulty baseline ({elapsed_ratio:.2f}x)",
    ]

    response = CompletionPredictionResponse(
        completion_probability=prob,
        predicted_completion=pred_completed,
        confidence=round(abs(prob - 0.5) * 2.0, 2),
        top_factors=top_factors,
        model_version=(artifacts["completion"]["meta"] or {}).get("version", "v1.0"),
    )
    log_prediction_event("COMPLETION", request.model_dump(), response.model_dump())
    return response

@app.post("/predict/hint", response_model=HintPredictionResponse)
def predict_hint(request: HintPredictionRequest):
    model = artifacts["hint"]["model"]
    le = artifacts["hint"]["le"]
    scaler = artifacts["hint"]["scaler"]
    if model is None or le is None:
        load_all_artifacts()
        model = artifacts["hint"]["model"]
        le = artifacts["hint"]["le"]
        scaler = artifacts["hint"]["scaler"]
        if model is None or le is None:
            raise HTTPException(status_code=503, detail="Hint model not ready.")

    skill_map = {"BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3, "EXPERT": 4}
    diff_map = {"EASY": 1, "MEDIUM": 2, "HARD": 3, "EXPERT": 4}
    s_num = skill_map.get(request.player_skill.upper(), 2)
    d_num = diff_map.get(request.difficulty.upper(), 2)
    open_cands = (1.0 - request.current_progress) * 81 * 2.5

    row = [
        float(s_num),
        float(d_num),
        request.current_progress,
        float(request.mistakes_count),
        request.elapsed_time,
        float(request.previous_hints_used),
        request.stuck_duration_seconds,
        open_cands
    ]
    X_input = pd.DataFrame([row], columns=HINT_FEATURE_COLUMNS)
    X_proc = scaler.transform(X_input) if scaler is not None else X_input

    y_pred = model.predict(X_proc)
    pred_idx = int(y_pred[0])
    hint_type = le.inverse_transform([pred_idx])[0]

    reasons = {
        "CELL": "Focus attention on a cell with immediate deterministic candidates.",
        "ROW": "Inspect the most constrained row to find missing numbers.",
        "COLUMN": "Analyze the active column to identify hidden singles.",
        "REGION": "Examine the 3x3 quadrant with the highest density of placed numbers.",
        "TECHNIQUE": "Look for a Naked Single or Pointing Pair technique across intersecting blocks.",
        "NEXT_MOVE": "Direct step reveal provided to maintain gameplay flow and momentum."
    }

    top_factors = [
        f"Important model factor: Player experience tier ({request.player_skill})",
        f"Important model factor: Hesitation duration ({int(request.stuck_duration_seconds)}s)",
        f"Important model factor: Prior hints utilized ({request.previous_hints_used})"
    ]

    response = HintPredictionResponse(
        hint_type=hint_type,
        confidence=0.82,
        reason=reasons.get(hint_type, "Adaptive hint suggested to unblock solving progress."),
        target_cell={"row": 4, "col": 4} if hint_type in ["CELL", "NEXT_MOVE"] else None,
        technique="Naked Single" if hint_type == "TECHNIQUE" else None,
        top_factors=top_factors
    )
    log_prediction_event("HINT", request.model_dump(), response.model_dump())
    return response

@app.post("/predict/recommendation", response_model=RecommendationPredictionResponse)
def predict_recommendation(request: RecommendationPredictionRequest):
    model = artifacts["recommendation"]["model"]
    le = artifacts["recommendation"]["le"]
    scaler = artifacts["recommendation"]["scaler"]
    if model is None or le is None:
        load_all_artifacts()
        model = artifacts["recommendation"]["model"]
        le = artifacts["recommendation"]["le"]
        scaler = artifacts["recommendation"]["scaler"]
        if model is None or le is None:
            raise HTTPException(status_code=503, detail="Recommendation model not ready.")

    row = [
        request.completion_rate,
        request.average_accuracy,
        request.average_mistakes,
        request.average_hints,
        request.easy_completion_rate,
        request.medium_completion_rate,
        request.hard_completion_rate,
        request.recent_accuracy,
        request.recent_error_rate,
        request.average_moves_per_minute
    ]
    X_input = pd.DataFrame([row], columns=RECOMMENDATION_FEATURE_COLUMNS)
    X_proc = scaler.transform(X_input) if scaler is not None else X_input

    y_pred = model.predict(X_proc)
    pred_idx = int(y_pred[0])
    rec_diff = le.inverse_transform([pred_idx])[0]

    reasons = {
        "EASY": "Recommended for building precision and reinforcing baseline solving speed.",
        "MEDIUM": "Your recent accuracy and consistent completion demonstrate readiness for intermediate tactical puzzles.",
        "HARD": "Strong mastery of medium puzzles qualifies you for advanced candidate elimination challenges.",
        "EXPERT": "Outstanding solve rate and minimal mistakes warrant master-level diabolical puzzles."
    }

    top_factors = [
        f"Important model factor: Medium mission mastery ({int(request.medium_completion_rate * 100)}%)",
        f"Important model factor: Recent session precision ({int(request.recent_accuracy * 100)}%)",
        f"Important model factor: Controlled mistake rate ({request.average_mistakes:.1f} per game)"
    ]

    response = RecommendationPredictionResponse(
        recommended_difficulty=rec_diff,
        recommended_reason=reasons.get(rec_diff, "Recommended puzzle difficulty based on telemetry trajectory."),
        confidence=0.92,
        top_factors=top_factors
    )
    log_prediction_event("RECOMMENDATION", request.model_dump(), response.model_dump())
    return response

# ==================== TRAINING ENDPOINTS ====================

@app.post("/train/skill")
def trigger_train_skill():
    res = train_skill()
    load_all_artifacts()
    register_model_run("player_skill", res["algorithm"], "v1.1", "1.1.0", "v1.1", res["test_metrics"], res.get("model_comparison", {}), res.get("artifacts", {}))
    return {"status": "success", "model": "player_skill", "algorithm": res["algorithm"], "metrics": res["test_metrics"]}

@app.post("/train/difficulty")
def trigger_train_difficulty():
    res = train_difficulty()
    load_all_artifacts()
    register_model_run("puzzle_difficulty", res["algorithm"], "v1.0", "1.1.0", "v1.0", res["test_metrics"], res.get("model_comparison", {}), res.get("artifacts", {}))
    return {"status": "success", "model": "puzzle_difficulty", "algorithm": res["algorithm"], "metrics": res["test_metrics"]}

@app.post("/train/completion")
def trigger_train_completion():
    res = train_completion()
    load_all_artifacts()
    register_model_run("puzzle_completion", res["algorithm"], "v1.0", "1.1.0", "v1.0", res["test_metrics"], res.get("model_comparison", {}), res.get("artifacts", {}))
    return {"status": "success", "model": "puzzle_completion", "algorithm": res["algorithm"], "metrics": res["test_metrics"]}

@app.post("/train/hint")
def trigger_train_hint():
    res = train_hint()
    load_all_artifacts()
    register_model_run("hint_recommendation", res["algorithm"], "v1.0", "1.1.0", "v1.0", res["test_metrics"], res.get("model_comparison", {}), res.get("artifacts", {}))
    return {"status": "success", "model": "hint_recommendation", "algorithm": res["algorithm"], "metrics": res["test_metrics"]}

@app.post("/train/recommendation")
def trigger_train_recommendation():
    res = train_recommendation()
    load_all_artifacts()
    register_model_run("personalized_recommendation", res["algorithm"], "v1.0", "1.1.0", "v1.0", res["test_metrics"], res.get("model_comparison", {}), res.get("artifacts", {}))
    return {"status": "success", "model": "personalized_recommendation", "algorithm": res["algorithm"], "metrics": res["test_metrics"]}

# ==================== REGISTRY & EXPERIMENT ENDPOINTS ====================

@app.get("/ml/models")
def list_models():
    return get_registered_models()

@app.get("/ml/difficulty/importance")
def difficulty_feature_importance():
    metadata_path = os.path.join(MODELS_DIR, "difficulty_metadata.json")
    if not os.path.exists(metadata_path):
        return {"feature_importances": {}}
    with open(metadata_path, "r", encoding="utf-8") as metadata_file:
        metadata = json.load(metadata_file)
    return {
        "algorithm": metadata.get("algorithm"),
        "version": metadata.get("version"),
        "test_metrics": metadata.get("test_metrics", {}),
        "feature_importances": metadata.get("feature_importances", {})
    }

@app.get("/ml/models/{model_id}")
def get_model(model_id: str = Path(...)):
    m = get_model_by_id(model_id)
    if not m:
        raise HTTPException(status_code=404, detail="Model not found")
    return m

@app.post("/ml/models/{model_id}/activate")
def activate_model_endpoint(model_id: str = Path(...)):
    ok = activate_model(model_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Model not found")
    load_all_artifacts()
    return {"status": "success", "message": f"Model {model_id} activated."}

@app.get("/ml/experiments")
def list_experiments():
    return get_experiment_history()

@app.get("/ml/datasets")
def list_datasets():
    manifest_path = os.path.join(DATA_DIR, "dataset_manifest.json")
    report_path = os.path.join(DATA_DIR, "preprocessing_report.json")
    manifest = {}
    report = {}
    if os.path.exists(manifest_path):
        with open(manifest_path, "r", encoding="utf-8") as f:
            manifest = json.load(f)
    if os.path.exists(report_path):
        with open(report_path, "r", encoding="utf-8") as f:
            report = json.load(f)
    return {
        "manifest": manifest,
        "preprocessing_report": report
    }

@app.get("/ml/predictions")
def list_predictions():
    return get_recent_predictions()

@app.get("/ml/health")
def ml_health():
    loaded_counts = sum(1 for m in artifacts.values() if m.get("model") is not None)
    return {
        "status": "UP",
        "loaded_models_count": loaded_counts,
        "models": {k: (v.get("model") is not None) for k, v in artifacts.items()}
    }

# Backward compatible legacy endpoints
@app.get("/metadata")
def legacy_metadata():
    if artifacts["skill"]["meta"]:
        return artifacts["skill"]["meta"]
    raise HTTPException(status_code=404, detail="Skill metadata not found")

@app.get("/health")
def legacy_health():
    return {
        "status": "UP",
        "model_loaded": artifacts["skill"]["model"] is not None
    }
