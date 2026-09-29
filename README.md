# ✦ SUDOKU — Deep Space Edition ✦
## Web Programming + Machine Learning Sudoku Application

A comprehensive, full-stack, ML-driven Sudoku application featuring a cosmic space theme, real-time single-player and multiplayer rooms, automated backtracking solvers, dynamic player intelligence, and machine learning personalization.

Built with **React 19 + TypeScript** (frontend), **Spring Boot 3.3 + Java 21** (backend), embedded **H2 Database** (`jdbc:h2:file:./data/sudokudb`), and a **Python 3.10+ FastAPI Machine Learning Microservice**.

---

## 1. Project Team Responsibilities & Boundaries

### Member 1: Player Intelligence & Personalization (Completed)
* **5 Production Machine Learning Models:**
  1. **Player Skill Classifier:** 12 gameplay features; Logistic Regression / Decision Tree; predicts `BEGINNER`, `INTERMEDIATE`, `ADVANCED`, `EXPERT`.
  2. **Puzzle Difficulty Classifier:** 13 board topology features (clues, candidates, branch factor, symmetry); Logistic Regression across 2,744 IEEE + benchmark puzzles.
  3. **In-Game Puzzle Completion Estimator:** 11 snapshot features; Decision Tree; predicts real-time completion probability and outcome.
  4. **Pedagogical Hint Recommender:** 8 scaffolding features; Gradient Boosting; predicts guidance level (`CELL`, `ROW`, `COLUMN`, `REGION`, `TECHNIQUE`, `NEXT_MOVE`).
  5. **Dynamic Personalized Puzzle Recommender:** 10 trajectory features (frustration index, boredom index, momentum); Gradient Boosting; recommends optimal next difficulty.
* **Configurable Data Pipeline:** Kaggle dataset ingestion with automatic offline benchmark fallback, dataset manifest (`dataset_manifest.json`), and preprocessing audit report (`preprocessing_report.json`).
* **Model Registry & Tracking:** Versioned metadata, dynamic model activation, and prediction telemetry logging.
* **FastAPI ML Microservice:** `/predict/*`, `/train/*`, and `/ml/*` endpoints.
* **Spring Boot Integration:** `MLPredictionService` with circuit breaker, timeout, and heuristic fallback; `AdminMLController`, and `PlayerMLController`.
* **React Admin ML Dashboard:** Unified dashboard displaying KPI overview, model registry management, dynamic model activation, live training triggers, dataset catalog, experiment comparison, and prediction telemetry.

### Member 2: Player Profile & Statistics (Completed Foundation)
* **Player Profile CRUD:**
  - `GET /api/players/{playerId}/profile`: Retrieve detailed player profile.
  - `PUT /api/players/{playerId}/profile`: Update username, display name, avatar, or settings.
  - `DELETE /api/players/{playerId}/profile`: Safely remove player account and cascade related records.
* **Streaks & Game History:**
  - `GET /api/players/{playerId}/statistics`: Returns performance stats including `currentStreak` (consecutive recent wins) and `bestStreak` (all-time historical record).
  - Pre-Hibernate H2 database schema migration (`PlayerStatisticsSchemaMigration`).
* **Frontend Profile Display:** Integrated metric cards for current and best win streaks alongside 18 gameplay metrics.

---

## 2. Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| **Java JDK** | 21+ | Spring Boot Backend |
| **Maven** | 3.9+ | Backend Build & Dependency Management |
| **Node.js** | 20+ | React Frontend Runtime |
| **npm** | 10+ | Frontend Package Management |
| **Python** | 3.10+ | ML Pipeline & FastAPI Microservice |

---

## 3. Project Directory Structure

```
java_sudoku/
├── backend/                   # Spring Boot 3.3 REST API + H2 Database
│   ├── src/main/java/         # Controllers, Services, Entities, DTOs, Security
│   ├── src/test/java/         # Unit and Integration Tests (105 passed)
│   ├── data/                  # Embedded H2 Database (sudokudb.mv.db)
│   └── pom.xml
├── frontend/                  # React 19 + TypeScript + Vite + Tailwind CSS
│   ├── src/
│   │   ├── components/        # Game Board, Number Pad, Player Sidebar
│   │   ├── pages/             # Game, Profile, Rooms, Admin ML Dashboard
│   │   ├── services/          # REST clients (gameApi, profileApi, adminMlApi)
│   │   └── types/             # TypeScript interfaces
│   └── package.json
├── ml/                        # Python Machine Learning Suite
│   ├── api/                   # FastAPI microservice (main.py)
│   ├── data/                  # Kaggle ingestion, manifests, raw & processed data
│   ├── features/              # Feature extractors (puzzle, player, snapshot)
│   ├── models/                # Saved joblib models and metadata JSON
│   ├── preprocessing/         # Data cleaning and validation pipeline
│   ├── registry/              # Model registry, experiments, and telemetry
│   ├── training/              # Training scripts for all 5 ML models
│   ├── tests/                 # Pytest test suite (10 passed)
│   └── requirements.txt
├── docs/                      # Technical Documentation
│   ├── ML_ARCHITECTURE.md     # Microservice design & model details
│   ├── DATA_PIPELINE.md       # Ingestion, validation, & feature extraction
│   └── MODEL_EVALUATION.md    # Test benchmarks, comparisons, & metrics
└── README.md
```

---

## 4. Quick Start Guide

### Step 1: Start the Python ML Service
In your first terminal:
```bash
cd ml
python -m venv venv

# Windows PowerShell:
.\venv\Scripts\Activate.ps1
# Linux / macOS:
# source venv/bin/activate

pip install -r requirements.txt

# (Optional) Download datasets & train models if not already initialized:
python data/download_dataset.py
python training/train_skill.py
python training/train_difficulty.py
python training/train_completion.py
python training/train_hint.py
python training/train_recommendation.py

# Launch FastAPI microservice on port 8000:
uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
```
* ML Microservice interactive API Docs: **http://localhost:8000/docs**

### Step 2: Start the Spring Boot Backend
In your second terminal:
```bash
cd backend
mvn spring-boot:run
```
* Backend starts at **http://localhost:8080**.
* H2 Database Console: **http://localhost:8080/h2-console**  
  - JDBC URL: `jdbc:h2:file:./data/sudokudb`  
  - Username: `sa` | Password: *(blank)*

### Step 3: Start the React Frontend
In your third terminal:
```bash
cd frontend
npm install
npm run dev
```
* Frontend starts at **http://localhost:5173**.

---

## 5. Kaggle Dataset Configuration

The ingestion script (`ml/data/download_dataset.py`) supports downloading real Sudoku and human gameplay datasets directly from Kaggle.

To configure Kaggle credentials, set the following environment variables:
```bash
export KAGGLE_USERNAME="your_kaggle_username"
export KAGGLE_KEY="your_kaggle_api_key"
export KAGGLE_DATASET="bryanpark/sudoku" # optional override
```

> **Offline Fallback Guarantee:** If credentials are not supplied, the pipeline automatically detects this and falls back gracefully to the verified 2,744 IEEE human game records and benchmark puzzle banks included in `ml/data/`. All training scripts and FastAPI endpoints run smoothly with or without Kaggle keys.

---

## 6. REST API Reference

### 6.1 Machine Learning Endpoints (FastAPI — Port 8000)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/predict/skill` | Classifies player skill level from 12 historical gameplay features |
| `POST` | `/predict/difficulty` | Classifies puzzle difficulty from 13 board topology features |
| `POST` | `/predict/completion` | Predicts in-game completion probability and binary completion |
| `POST` | `/predict/hint` | Recommends pedagogical scaffolding hint type |
| `POST` | `/predict/recommendation` | Recommends next optimal puzzle difficulty |
| `POST` | `/train/{model_type}` | Retrains specified model type and registers new experiment |
| `GET` | `/ml/models` | Lists all registered models and active versions |
| `POST` | `/ml/models/{id}/activate` | Dynamically activates a model version |
| `GET` | `/ml/experiments` | Retrieves experiment tracking and comparison history |
| `GET` | `/ml/datasets` | Retrieves dataset manifest and preprocessing audit report |
| `GET` | `/ml/predictions` | Retrieves real-time inference telemetry logs |
| `GET` | `/ml/health` | Service health status and loaded model inventory |

### 6.2 Backend ML & Admin Proxy Endpoints (Spring Boot — Port 8080)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/admin/ml/overview` | Aggregated ML status, active models, dataset counts, and recent telemetry |
| `GET` | `/api/admin/ml/models` | Proxies model registry listing |
| `POST` | `/api/admin/ml/models/{id}/activate` | Activates model version |
| `POST` | `/api/admin/ml/train/{modelType}` | Triggers asynchronous retraining |
| `GET` | `/api/admin/ml/experiments` | Experiment comparison table |
| `GET` | `/api/admin/ml/datasets` | Dataset manifest and quality audit report |
| `GET` | `/api/admin/ml/predictions` | Real-time prediction logs |
| `POST` | `/api/ml/predict/difficulty` | Player/Game puzzle difficulty prediction |
| `POST` | `/api/ml/predict/completion` | Real-time game session completion estimator |
| `POST` | `/api/ml/predict/hint` | Pedagogical hint recommendation |

### 6.3 Player Profile & Streak Endpoints (Spring Boot — Port 8080)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/players/{playerId}/profile` | Get full player profile details |
| `PUT` | `/api/players/{playerId}/profile` | Update profile information |
| `DELETE` | `/api/players/{playerId}/profile` | Delete player account |
| `GET` | `/api/players/{playerId}/statistics` | Retrieve statistics including `currentStreak` & `bestStreak` |

---

## 7. Testing & Quality Verification

* **Python ML Tests:**
  ```bash
  cd ml
  pytest tests/test_ml.py -v
  ```
  *Result: 10/10 passed.*
* **Spring Boot Backend Tests:**
  ```bash
  cd backend
  mvn test
  ```
  *Result: 105/105 passed (0 failures, 0 errors).*
* **Frontend TypeScript & Build Verification:**
  ```bash
  cd frontend
  npm run build
  ```
  *Result: 0 errors; built production bundle cleanly.*

---

## 8. License

This project is developed as part of a Web Programming & Machine Learning curriculum. All rights reserved.
