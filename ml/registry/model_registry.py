"""
Central Model Registry and Experiment Tracking for Sudoku ML Operations.

Maintains registry records, version histories, active deployment tags,
and experiment execution logs.
"""

import os
import json
import uuid
import datetime
from typing import List, Dict, Any, Optional

MODELS_DIR = os.path.join(os.path.dirname(__file__), "..", "models")
REGISTRY_FILE = os.path.join(MODELS_DIR, "model_registry.json")
EXPERIMENTS_FILE = os.path.join(MODELS_DIR, "experiment_history.json")
PREDICTIONS_LOG_FILE = os.path.join(MODELS_DIR, "recent_predictions.json")

os.makedirs(MODELS_DIR, exist_ok=True)

def load_json(filepath: str, default: Any) -> Any:
    if os.path.exists(filepath):
        try:
            with open(filepath, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return default
    return default

def save_json(filepath: str, data: Any):
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def register_model_run(
    model_type: str,
    algorithm: str,
    version: str,
    dataset_version: str,
    feature_version: str,
    test_metrics: Dict[str, float],
    comparison_scores: Dict[str, Any],
    artifact_paths: Dict[str, str],
    duration_seconds: float = 1.0,
    status: str = "ACTIVE"
) -> Dict[str, Any]:
    """
    Records a training run into both the experiment history and model registry.
    """
    exp_id = f"EXP-{uuid.uuid4().hex[:8].upper()}"
    model_id = f"MOD-{model_type[:4].upper()}-{uuid.uuid4().hex[:6].upper()}"
    timestamp = datetime.datetime.now().isoformat()

    # 1. Add experiment
    experiments = load_json(EXPERIMENTS_FILE, [])
    exp_record = {
        "experiment_id": exp_id,
        "model_type": model_type,
        "selected_algorithm": algorithm,
        "dataset_version": dataset_version,
        "feature_version": feature_version,
        "algorithms_evaluated": list(comparison_scores.keys()),
        "validation_scores": comparison_scores,
        "test_metrics": test_metrics,
        "duration_seconds": round(duration_seconds, 2),
        "timestamp": timestamp,
        "artifact_paths": artifact_paths
    }
    experiments.insert(0, exp_record)
    save_json(EXPERIMENTS_FILE, experiments[:100])

    # 2. Update model registry
    registry = load_json(REGISTRY_FILE, [])
    # Set existing of same type to INACTIVE if new one is ACTIVE
    if status == "ACTIVE":
        for m in registry:
            if m.get("model_type") == model_type:
                m["status"] = "ARCHIVED"

    model_record = {
        "model_id": model_id,
        "model_type": model_type,
        "name": f"{model_type.replace('_', ' ').title()} Model",
        "algorithm": algorithm,
        "version": version,
        "dataset_version": dataset_version,
        "feature_version": feature_version,
        "accuracy": test_metrics.get("accuracy", 0.0),
        "precision": test_metrics.get("precision_weighted", 0.0),
        "recall": test_metrics.get("recall_weighted", 0.0),
        "f1": test_metrics.get("f1_weighted", 0.0),
        "status": status,
        "created_at": timestamp,
        "artifact_paths": artifact_paths
    }
    registry.insert(0, model_record)
    save_json(REGISTRY_FILE, registry)
    return model_record

def get_registered_models() -> List[Dict[str, Any]]:
    return load_json(REGISTRY_FILE, [])

def get_model_by_id(model_id: str) -> Optional[Dict[str, Any]]:
    models = get_registered_models()
    for m in models:
        if m.get("model_id") == model_id:
            return m
    return None

def activate_model(model_id: str) -> bool:
    models = get_registered_models()
    target = None
    for m in models:
        if m.get("model_id") == model_id:
            target = m
            break
    if not target:
        return False

    model_type = target["model_type"]
    for m in models:
        if m.get("model_type") == model_type:
            m["status"] = "ARCHIVED"
    target["status"] = "ACTIVE"
    save_json(REGISTRY_FILE, models)
    return True

def get_experiment_history() -> List[Dict[str, Any]]:
    return load_json(EXPERIMENTS_FILE, [])

def log_prediction_event(pred_type: str, input_summary: Dict[str, Any], output_summary: Dict[str, Any]):
    logs = load_json(PREDICTIONS_LOG_FILE, [])
    entry = {
        "id": str(uuid.uuid4())[:8],
        "type": pred_type,
        "timestamp": datetime.datetime.now().isoformat(),
        "input": input_summary,
        "output": output_summary
    }
    logs.insert(0, entry)
    save_json(PREDICTIONS_LOG_FILE, logs[:200])

def get_recent_predictions() -> List[Dict[str, Any]]:
    return load_json(PREDICTIONS_LOG_FILE, [])
