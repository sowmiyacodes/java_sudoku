# SmartSudoku Machine Learning Benchmark Report

## Executive Summary

This report documents the rigorous evaluation, methodology, and performance benchmarks for the 5 machine learning models operating in the SmartSudoku platform.

The machine learning suite provides intelligent gameplay features including player skill assessment, puzzle difficulty estimation, real-time completion forecasting, pedagogical hint recommendation, and personalized puzzle recommendation.

---

## Model Evaluation Summary

| # | ML Task | Primary Algorithm | Validation Split | Test Split | Primary Metric | Baseline Score | Model Score | Status |
|---|---|---|---|---|---|---|---|---|
| **1** | **Player Skill Classifier** | Logistic Regression | 15% Stratified | 15% Stratified | Weighted F1 | 0.25 (Random) | **0.9554** | Production Active |
| **2** | **Puzzle Difficulty Estimator** | Multi-class Logistic | 15% Stratified | 15% Stratified | Weighted F1 | 0.25 (Random) | **0.3701** | Production Active |
| **3** | **Game Completion Predictor** | Gradient Boosting | 15% Stratified | 15% Stratified | ROC-AUC | 0.50 (Random) | **0.8183** | Production Active |
| **4** | **Pedagogical Hint Engine** | Gradient Boosting | 15% Stratified | 15% Stratified | F1 Score | 0.20 (Random) | **0.6076** | Production Active |
| **5** | **Personalized Recommender** | Gradient Boosting | 15% Stratified | 15% Stratified | F1 Score | 0.25 (Random) | **1.0000** | Production Active |

---

## Detailed Model Cards & Benchmark Analysis

### Model 1: Player Skill Classification

* **Problem Statement**: Predict player skill tier (`BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`) based on telemetry features.
* **Input Features**: 12 engineered features (`games_played`, `win_rate`, `avg_duration_seconds`, `avg_mistakes_per_game`, `avg_hints_per_game`, `avg_score`, `accuracy_rate`, `completion_rate`, `undo_frequency`, `avg_think_time_per_move`, `hard_puzzle_success_rate`, `streak_count`).
* **Dataset Size**: 600 unique player profiles.
* **Train / Val / Test Split**: 70% (420) / 15% (90) / 15% (90) stratified.
* **Algorithms Tested**:
  * Logistic Regression (Validation F1: 0.9664)
  * Decision Tree (Validation F1: 0.9445)
  * Random Forest (Validation F1: 0.9442)
  * Gradient Boosting (Validation F1: 0.9664)
* **Winning Model**: **Logistic Regression** (L2 penalty, C=1.0)
* **Test Performance**:
  * Accuracy: **95.56%**
  * Precision: **95.68%**
  * Recall: **95.56%**
  * F1 (Weighted): **0.9554**
  * F1 (Macro): **0.9534**
* **Limitations**: High accuracy on synthetic/grounded distribution; requires periodic recalibration as novel human playstyles emerge.

---

### Model 2: Puzzle Difficulty Classification

* **Problem Statement**: Estimate puzzle difficulty tier (`EASY`, `MEDIUM`, `HARD`, `EXPERT`) directly from structural givens and grid complexity.
* **Input Features**: 11 grid features (`givens_count`, `empty_cells`, `min_givens_in_row`, `min_givens_in_col`, `min_givens_in_box`, `singles_count`, `hidden_singles_count`, `naked_pairs_count`, `pointing_pairs_count`, `backtrack_depth`, `symmetry_score`).
* **Dataset Size**: 2,744 puzzles (2,400 benchmark puzzles + 344 empirical human puzzles).
* **Train / Val / Test Split**: 70% / 15% / 15% stratified.
* **Winning Model**: **Logistic Regression**
* **Test Performance**:
  * Accuracy: **37.62%**
  * Weighted F1: **0.3701**
  * Macro F1: **0.3671**
* **Limitations**: Multi-class boundary ambiguity between Medium and Hard puzzles. Future enhancements will integrate exact constraint satisfaction propagation depth.

---

### Model 3: Game Completion Probability Prediction

* **Problem Statement**: Predict whether an active gameplay session will be completed successfully or abandoned.
* **Input Features**: Session snapshot features (`elapsed_seconds`, `mistakes_count`, `hints_used`, `current_progress_pct`, `player_skill_encoded`, `puzzle_difficulty_encoded`, `time_per_filled_cell`).
* **Dataset Size**: 16,385 session snapshots.
* **Train / Val / Test Split**: 70% / 15% / 15% stratified.
* **Winning Model**: **Gradient Boosting Classifier**
* **Test Performance**:
  * Accuracy: **84.04%**
  * F1 Score: **0.8169**
  * ROC-AUC: **0.8183**
* **Limitations**: Feature extraction enforces strict anti-leakage guards so future outcome variables (e.g. final mistake count) are excluded from real-time prediction inputs.

---

### Model 4: Pedagogical Hint Recommendation Engine

* **Problem Statement**: Recommend the optimal hint type (`Naked Single`, `Hidden Single`, `Direct Reveal`, `Naked Pair`) tailored to player skill and board state.
* **Dataset Size**: Session hint telemetry.
* **Winning Model**: **Gradient Boosting Classifier**
* **Test Performance**:
  * Accuracy: **60.27%**
  * F1 Score: **0.6076**

---

### Model 5: Personalized Puzzle Recommendation Model

* **Problem Statement**: Recommend the optimal next puzzle difficulty level to maximize player engagement and skill progression.
* **Dataset Size**: Player recommendation logs.
* **Winning Model**: **Gradient Boosting Classifier**
* **Test Performance**:
  * Accuracy: **100.00%**
  * F1 Score: **1.0000**
