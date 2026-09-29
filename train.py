"""
Master Orchestrator Training Script for Sudoku Machine Learning Suite.
Trains all 5 production models:
1. Player Skill Classification Model
2. Puzzle Difficulty Estimation Model
3. Puzzle Completion Probability Model
4. Hint Pedagogical Recommendation Model
5. Personalized Difficulty Recommender Model
"""

import time
import datetime
from ml.data.download_dataset import download_external_datasets, generate_player_gameplay_sessions
from ml.preprocessing.pipeline import run_preprocessing
from ml.training.train_skill import train_and_evaluate as train_skill
from ml.training.train_difficulty import train_and_evaluate_difficulty as train_difficulty
from ml.training.train_completion import train_and_evaluate_completion as train_completion
from ml.training.train_hint import train_and_evaluate_hint as train_hint
from ml.training.train_recommendation import train_and_evaluate_recommendation as train_recommendation
from ml.registry.model_registry import register_model_run

def main():
    print("================================================================")
    print("      SMARTSUDOKU MACHINE LEARNING SUITE REPRODUCIBILITY        ")
    print("================================================================")

    # 1. Dataset Acquisition & Session Generation
    print("\n>>> STEP 1: Ingesting & Generating Data...")
    download_external_datasets()
    generate_player_gameplay_sessions()

    # 2. Preprocessing & Feature Engineering
    print("\n>>> STEP 2: Executing Preprocessing Pipeline...")
    run_preprocessing()

    # 3. Model 1: Player Skill Classification
    print("\n>>> STEP 3: Training Skill Classification Model...")
    t0 = time.time()
    m_skill = train_skill()
    register_model_run(
        model_type="player_skill",
        algorithm=m_skill["algorithm"],
        version="v1.1",
        dataset_version="1.1.0",
        feature_version="v1.1",
        test_metrics=m_skill["test_metrics"],
        comparison_scores=m_skill.get("model_comparison", {}),
        artifact_paths=m_skill.get("artifacts", {}),
        duration_seconds=time.time() - t0
    )

    # 4. Model 2: Puzzle Difficulty Model
    print("\n>>> STEP 4: Training Puzzle Difficulty Model...")
    t0 = time.time()
    m_diff = train_difficulty()
    register_model_run(
        model_type="puzzle_difficulty",
        algorithm=m_diff["algorithm"],
        version="v1.0",
        dataset_version="1.1.0",
        feature_version="v1.0",
        test_metrics=m_diff["test_metrics"],
        comparison_scores=m_diff.get("model_comparison", {}),
        artifact_paths=m_diff.get("artifacts", {}),
        duration_seconds=time.time() - t0
    )

    # 5. Model 3: Puzzle Completion Probability Model
    print("\n>>> STEP 5: Training Puzzle Completion Probability Model...")
    t0 = time.time()
    m_comp = train_completion()
    register_model_run(
        model_type="puzzle_completion",
        algorithm=m_comp["algorithm"],
        version="v1.0",
        dataset_version="1.1.0",
        feature_version="v1.0",
        test_metrics=m_comp["test_metrics"],
        comparison_scores=m_comp.get("model_comparison", {}),
        artifact_paths=m_comp.get("artifacts", {}),
        duration_seconds=time.time() - t0
    )

    # 6. Model 4: Hint Recommendation Model
    print("\n>>> STEP 6: Training Hint Pedagogical Recommendation Model...")
    t0 = time.time()
    m_hint = train_hint()
    register_model_run(
        model_type="hint_recommendation",
        algorithm=m_hint["algorithm"],
        version="v1.0",
        dataset_version="1.1.0",
        feature_version="v1.0",
        test_metrics=m_hint["test_metrics"],
        comparison_scores=m_hint.get("model_comparison", {}),
        artifact_paths=m_hint.get("artifacts", {}),
        duration_seconds=time.time() - t0
    )

    # 7. Model 5: Personalized Recommendation Model
    print("\n>>> STEP 7: Training Personalized Recommendation Model...")
    t0 = time.time()
    m_rec = train_recommendation()
    register_model_run(
        model_type="personalized_recommendation",
        algorithm=m_rec["algorithm"],
        version="v1.0",
        dataset_version="1.1.0",
        feature_version="v1.0",
        test_metrics=m_rec["test_metrics"],
        comparison_scores=m_rec.get("model_comparison", {}),
        artifact_paths=m_rec.get("artifacts", {}),
        duration_seconds=time.time() - t0
    )

    print("\n================================================================")
    print("  ALL 5 ML MODELS TRAINED, EVALUATED, AND REGISTERED IN REGISTRY")
    print("================================================================")

if __name__ == "__main__":
    main()
