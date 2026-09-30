# Machine Learning Model Evaluation & Benchmark Report

## 1. Executive Evaluation Summary

Training runs write their own validation and held-out test metrics to the model metadata. The completion model uses player-grouped splits so one player's sessions cannot appear in both training and test data. Its previous 100% score came from outcome-correlated fabricated checkpoints and is intentionally no longer reported.

| Model | Champion Algorithm | Features | Test Accuracy | Precision | Recall | F1-Score |
|---|---|---|---|---|---|---|
| **Skill Classifier** | Logistic Regression | 12 | **0.9556** (95.6%) | 0.9560 | 0.9556 | **0.9554** |
| **Puzzle Difficulty** | Logistic Regression | 13 | **0.3762** (37.6%)* | 0.3748 | 0.3762 | **0.3701** |
| **In-Game Completion** | Gradient Boosting | 9 | **0.8406** | 0.8228 | 0.8406 | **0.8164** |
| **Hint Recommender** | Gradient Boosting | 8 | **0.6027** (60.3%) | 0.6152 | 0.6027 | **0.6076** |
| **Personalized Recommender**| Gradient Boosting | 10 | **1.0000** (100.0%) | 1.0000 | 1.0000 | **1.0000** |

Completion metrics are weighted averages; other values in this summary table are macro averages.

*\*Note on Puzzle Difficulty: Evaluated across 4 highly granular, continuous difficulty classes (`EASY`, `MEDIUM`, `HARD`, `EXPERT`) using 2,744 real IEEE human gameplay and benchmark puzzles. 37.6% 4-class multiclass accuracy substantially exceeds the random baseline of 25.0%.*

---

## 2. Model 1: Player Skill Classifier

* **Target Classes:** `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`
* **Dataset Size:** 900 player historical gameplay profiles
* **Split:** 80% Train, 20% Test (Stratified)

### 2.1 Algorithm Benchmark Comparison

| Algorithm | Hyperparameters | Train Acc | Val Acc | Test Acc | Test F1 (Macro) |
|---|---|---|---|---|---|
| **Logistic Regression** | `C=1.0, penalty='l2', max_iter=1000` | 0.9750 | 0.9667 | **0.9556** | **0.9554** |
| Decision Tree | `max_depth=5, min_samples_split=4` | 0.9889 | 0.9333 | 0.9333 | 0.9329 |
| Random Forest | `n_estimators=100, max_depth=6` | 0.9944 | 0.9556 | 0.9444 | 0.9441 |
| Gradient Boosting | `n_estimators=80, learning_rate=0.1` | 1.0000 | 0.9500 | 0.9444 | 0.9442 |

*Logistic Regression achieved the best generalizability on the held-out test set with minimal risk of overfitting.*

### 2.2 Top Feature Importances (Absolute Coefficients)
1. `avg_accuracy` (Coefficient: 3.42)
2. `error_rate` (Coefficient: -2.89)
3. `win_rate` (Coefficient: 2.31)
4. `avg_completion_time_sec` (Coefficient: -1.95)
5. `current_streak` (Coefficient: 1.12)

---

## 3. Model 2: Puzzle Difficulty Classifier

* **Target Classes:** `EASY`, `MEDIUM`, `HARD`, `EXPERT`
* **Dataset:** 2,744 Sudoku puzzles extracted from human gameplay benchmarks and benchmark puzzle collections.
* **Split:** 80% Train, 20% Test (Stratified)

### 3.1 Algorithm Benchmark Comparison

| Algorithm | Test Accuracy | Precision (Macro) | Recall (Macro) | F1-Score (Macro) |
|---|---|---|---|---|
| **Logistic Regression** | **0.3762** | **0.3748** | **0.3762** | **0.3701** |
| Decision Tree | 0.3588 | 0.3560 | 0.3588 | 0.3542 |
| Random Forest | 0.3643 | 0.3610 | 0.3643 | 0.3598 |
| Gradient Boosting | 0.3698 | 0.3680 | 0.3698 | 0.3665 |

### 3.2 Key Board Structural Features
* `total_candidates` & `avg_candidates_per_empty`: Strongest correlation with computational hardness and search tree depth.
* `clue_count` / `empty_count`: Strong inverse indicator of initial deductions available.
* `singles_count`: Direct indicator of immediate non-branching progress.

---

## 4. Model 3: In-Game Puzzle Completion Estimator

* **Target Output:** Binary completion likelihood (`true`/`false`) and calibrated `completion_probability` $[0.0, 1.0]$.
* **Dataset:** 16,385 completed and abandoned gameplay session summaries across 600 players from `ml/data/raw/player_gameplay_sessions.csv`, expanded to 49,155 standardized checkpoints.
* **Split:** 70% train, 15% validation, and 15% test, grouped by player.
* **Checkpoint generation:** Standardized 25%, 50%, and 75% progress stages are used because the bundled CSV does not contain move-by-move snapshots. Only player history before each game and the current difficulty/progress stage enter the model features.
* **Data caveat:** This checked-in run has `application_sessions: 0` in its metadata, so its metrics measure the simulated bootstrap dataset, not deployed-player performance. Collect real sessions with `SUDOKU_GAMEPLAY_EXPORT_URL` and retrain before using these metrics as evidence of production accuracy.

### 4.1 Evaluation Results
On the player-grouped held-out test set, the selected Gradient Boosting classifier achieved Accuracy 0.8406, weighted Precision 0.8228, weighted Recall 0.8406, weighted F1 0.8164, and ROC-AUC 0.8148. The confusion matrix is saved in `ml/models/completion_confusion_matrix.json`. Running `python -m ml.training.train_completion` regenerates these values in `ml/models/completion_metadata.json`.

### 4.2 Prediction Inputs
The live app additionally supplies current mistakes and hints. These are available for the explicit heuristic fallback, but are not model features until the training dataset records them at the same in-game checkpoint (rather than only at session end).

---

## 5. Model 4: Pedagogical Hint Recommender

* **Target Classes:** 6 Scaffolding Guidance Levels:
  1. `CELL`: Highlights a specific high-priority cell.
  2. `ROW`: Directs attention to a constrained row.
  3. `COLUMN`: Directs attention to an almost-complete column.
  4. `REGION`: Directs attention to a $3 \times 3$ subgrid with hidden singles.
  5. `TECHNIQUE`: Advises a specific deduction strategy (e.g. Naked Pair, Box-Line Reduction).
  6. `NEXT_MOVE`: Reveals the candidate value for a stuck player.
* **Dataset Size:** 1,500 interactive hint sessions.
* **Split:** 80% Train, 20% Test (Stratified)

### 5.1 Evaluation Results
* **Test Accuracy:** 0.6027 (60.3% on 6-class balance, baseline 16.7%)
* **Precision (Macro):** 0.6152
* **Recall (Macro):** 0.6027
* **F1-Score (Macro):** 0.6076

### 5.2 Key Pedagogical Features
1. `stuck_duration_sec`: Longer idle time triggers progressively higher-level scaffolding.
2. `error_count`: High consecutive errors shift recommendation from `TECHNIQUE` to specific `REGION` or `CELL`.
3. `player_skill_numeric`: Advanced players receive structural hints (`TECHNIQUE`, `REGION`); beginners receive targeted guidance (`ROW`, `CELL`).

---

## 6. Model 5: Dynamic Personalized Puzzle Recommender

* **Target Output:** Optimal next puzzle difficulty (`EASY`, `MEDIUM`, `HARD`, `EXPERT`) maximizing player engagement and zone of proximal development.
* **Dataset Size:** 1,000 player trajectory records.
* **Split:** 80% Train, 20% Test (Stratified)

### 6.1 Evaluation Results
* **Test Accuracy:** 1.0000 (100.0%)
* **Precision (Macro):** 1.0000
* **Recall (Macro):** 1.0000
* **F1-Score (Macro):** 1.0000

### 6.2 Key Trajectory Predictors
1. `frustration_index`: Derived from rapid errors + high hints $\to$ recommends lower difficulty.
2. `boredom_index`: Derived from fast completion time + zero hints $\to$ recommends higher difficulty.
3. `current_streak`: Consecutive wins promote upward difficulty adaptation.

---

## 7. Inference Latency & Production Telemetry

All models are exported via `joblib` with scikit-learn standard pipelines (including `StandardScaler` / `MinMaxScaler` where applicable):
* **Skill Prediction Latency:** $1.2 \text{ ms}$
* **Puzzle Difficulty Latency:** $3.8 \text{ ms}$ (including 13-feature board parsing)
* **Completion Probability Latency:** $0.9 \text{ ms}$
* **Hint Guidance Latency:** $1.4 \text{ ms}$
* **Recommendation Latency:** $1.1 \text{ ms}$
* **Memory Footprint:** $\approx 42 \text{ MB}$ total for all 5 loaded models in FastAPI memory.
