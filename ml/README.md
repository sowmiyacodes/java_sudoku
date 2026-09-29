# Member 1 — Player Intelligence & Personalization ML Module

## 1. Dataset Name & Overview
- **Dataset Names**:
  1. **Sudoku Exchange Puzzle Bank** (by Grantm, generated with QQWing and rated via Sukaku Explainer)
  2. **IEEE Cloud Sudoku Human Gameplay Dataset** (by S.W. Wang, IEEE Access 2024)
  3. **Application Session Telemetry** (exported from the application's persistent H2 database)

## 2. Dataset Sources & Licenses
- **Sudoku Exchange Puzzle Bank**: [https://github.com/grantm/sudoku-exchange-puzzle-bank](https://github.com/grantm/sudoku-exchange-puzzle-bank)  
  *License*: Dedicated to the Public Domain (CC0 / Public Domain Mark).
- **IEEE Cloud Sudoku**: [https://github.com/synnwang/sudoku_dataset_difficulty](https://github.com/synnwang/sudoku_dataset_difficulty)  
  *Citation*: S.W. Wang, "A Dataset of Sudoku Puzzles With Difficulty Metrics Experienced by Human Players," *IEEE Access*, vol. 12, pp. 104254-104262, 2024.
- **Application Telemetry**: Exported from local H2 database (`games`, `moves`, `hint_history`, `leaderboard_scores`).

## 3. Automated Download Procedure
The dataset is downloaded and ingested automatically via:
```bash
python ml/data/download_dataset.py
```
or end-to-end via:
```bash
python train.py
```
No manual downloading or Kaggle credentials required.

## 4. Dataset Size & Records
- Raw session records: 15,572 gameplay records across 600 unique players.
- Benchmark puzzles: 1,500 puzzles categorized across Easy (<1.5), Medium (<2.5), and Hard (<5.0) difficulty ratings.
- Aggregated player-level profiles: 600 complete player behavioral feature vectors.

## 5. Raw vs Cleaned Columns
- **Raw Session Columns**:
  `game_id`, `player_id`, `difficulty`, `duration`, `moves`, `mistakes`, `hints`, `undos`, `accuracy`, `score`, `completion_status`, `timestamp`.
- **Cleaned & Processed Columns**:
  Deduplicated by `game_id`, missing hints/undos imputed with 0, duration validated $> 0$, accuracy clipped between $0.0$ and $1.0$, difficulty standardized to `Easy`, `Medium`, `Hard`.

## 6. Feature Engineering (15 Core Dimensions)
For each player, session telemetry is aggregated into:
1. `games_played`: Total games initiated.
2. `completion_rate`: Ratio of completed games to total games played.
3. `average_time`: Mean solving duration in seconds.
4. `best_time`: Shortest time achieved on a completed game.
5. `average_score`: Mean points awarded.
6. `average_accuracy`: Average placement accuracy ($moves / (moves + mistakes)$).
7. `average_mistakes`: Average errors per game.
8. `average_hints`: Average hints used per game.
9. `average_undos`: Average undos invoked per game.
10. `average_moves`: Average valid placements made.
11. `hard_completion_rate`: Successful completion rate on Hard puzzles.
12. `medium_completion_rate`: Successful completion rate on Medium puzzles.
13. `easy_completion_rate`: Successful completion rate on Easy puzzles.
14. `recent_accuracy`: Accuracy over the player's last 5 games.
15. `recent_error_rate`: Mistake ratio over the player's last 5 games.
16. `average_moves_per_minute`: Pacing metric ($total\_moves / total\_duration\_mins$).

## 7. Label Generation Methodology (Derived Skill Labels)
Because raw gameplay telemetry does not contain human skill labels, we establish a **transparent, derived skill labeling methodology** based on a multi-criteria **Behavioral Evaluation Scoring Index (BESI)**:

$$M = 0.25 \cdot c_1 + 0.25 \cdot c_2 + 0.15 \cdot c_3 + 0.15 \cdot c_4 + 0.10 \cdot c_5 + 0.10 \cdot c_6$$

Where:
- $c_1$: Overall completion rate $[0, 1]$
- $c_2$: Overall accuracy $[0, 1]$
- $c_3$: Error restraint ($1.0 - \min(1.0, mistakes / 5.0)$)
- $c_4$: Hint independence ($1.0 - \min(1.0, hints / 4.0)$)
- $c_5$: Hard difficulty proficiency ($0.6 \cdot hard\_completion + 0.4 \cdot medium\_completion$)
- $c_6$: Speed efficiency ($\text{clamp}(1.0 - (avg\_time - 250) / 900, 0, 1)$)

### Class Thresholds:
- **EXPERT**: $M \ge 0.85$
- **ADVANCED**: $0.70 \le M < 0.85$
- **INTERMEDIATE**: $0.52 \le M < 0.70$
- **BEGINNER**: $M < 0.52$

### Data Leakage Prevention:
The composite index $M$ is **not** included as an input feature in $X$. The candidate models learn non-linear decision boundaries directly from the raw multidimensional performance, speed, error, and assistance features.

## 8. Train / Validation / Test Split
- Stratified 3-way split:
  - **70% Training set** (420 players)
  - **15% Validation set** (90 players)
  - **15% Test set** (90 players)
- Reproducible random seed: `random_state = 42`.
- The test set remains untouched during model selection and hyperparameter tuning.

## 9. Model Comparison & Results
All candidate models were trained on the training set and evaluated on the validation set:

| Model | Val Accuracy | Val Precision (w) | Val Recall (w) | Val F1 (weighted) | Val F1 (macro) |
|---|:---:|:---:|:---:|:---:|:---:|
| **Decision Tree** | **0.9667** | **0.9675** | **0.9667** | **0.9667** | **0.9625** |
| Gradient Boosting | 0.9667 | 0.9675 | 0.9667 | 0.9667 | 0.9625 |
| Logistic Regression | 0.9444 | 0.9448 | 0.9444 | 0.9439 | 0.9399 |
| Random Forest | 0.9444 | 0.9448 | 0.9444 | 0.9439 | 0.9399 |

### Final Selected Model: **Decision Tree Classifier**
Selected based on top validation F1 score and high explainability for player coaching.

### Final Untouched Test Set Evaluation:
- **Test Accuracy**: 92.22%
- **Test Precision**: 93.28%
- **Test Recall**: 92.22%
- **Test F1 (Weighted)**: 92.14%
- **Test F1 (Macro)**: 91.53%

## 10. Saved Model Artifacts
Stored in `ml/models/`:
- `player_skill_decision_tree_v1.pkl`
- `best_skill_model.pkl`
- `scaler.pkl`
- `label_encoder.pkl`
- `model_metadata.json`
- `confusion_matrix.json`

## 11. How to Retrain
```bash
python train.py
```
Or via REST API:
```http
POST http://localhost:8000/train/skill
```

## 12. How to Start FastAPI Service
```bash
python -m uvicorn ml.api.main:app --host 0.0.0.0 --port 8000 --reload
```

## 13. How Spring Boot Connects to FastAPI
Spring Boot's `MLPredictionService` calls:
```http
POST http://localhost:8000/predict/skill
```
Passing player telemetry computed by `PlayerStatisticsService`. If FastAPI is offline, a graceful circuit breaker activates and returns player statistics with a clear status notice.
