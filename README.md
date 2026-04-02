# Transaction Fraud Detector

Hybrid fraud analysis system that combines:

- a Spring Boot backend for orchestration and APIs
- a Python fraud scoring layer built on the Kaggle `kartik2112/fraud-detection` dataset
- PostgreSQL + `pgvector` for the RAG knowledge base
- Vertex AI `text-embedding-004` for query embeddings and Vertex AI `gemini-2.5-flash` for final explanations
- an Angular frontend for fraud checks and audit review

This Phase-1 implementation supports real-time fraud analysis with:

- ML fraud scoring
- vector retrieval over a curated fraud knowledge base
- LLM-generated explanation, risk level, and recommended action
- audit logging
- frontend progress streaming for each analysis step

## System Overview

For each transaction request, the system:

1. builds a semantic transaction query from amount, category, device, geography, time, account age, and recent velocity
2. scores the request with the Python fraud model
3. generates an embedding for the semantic query using Vertex AI
4. retrieves the nearest fraud knowledge chunks from PostgreSQL + `pgvector`
5. reranks retrieved chunks with consistency penalties so obviously mismatched chunks are pushed down
6. selects a final diversified set of chunks for the prompt
7. asks Gemini to combine ML + RAG evidence into an explanation, risk level, and recommended action
8. applies a final calibration layer for clearly benign or mixed borderline cases
9. returns:
   - fraud score
   - retrieved titles and contents
   - explanation
   - risk level
   - recommended action
   - latency
10. stores the analysis in audit logs

## Tech Stack

- Backend: Java 21+, Spring Boot 3.5
- Frontend: Angular
- Database: PostgreSQL 16 + `pgvector`
- ML: Python, scikit-learn pipelines, XGBoost, LightGBM, CatBoost
- LLM / Embeddings: Spring AI + Vertex AI

## Exact Runtime Models

- Embedding model:
  - provider: Vertex AI through Spring AI `EmbeddingModel`
  - model: `text-embedding-004`
  - location: `us-central1`
- LLM used for explanation:
  - provider: Vertex AI through Spring AI `ChatClient`
  - model: `gemini-2.5-flash`
  - temperature: `0.2`
  - location: `us-central1`
- Vector database:
  - PostgreSQL 16 with `pgvector`
  - knowledge table: `fraud_knowledge`
  - stored fields used by RAG: `title`, `content`, `category`, `risk_level`, `embedding`
- Deployed ML scorer:
  - current best saved model: `lightgbm`
  - scoring entry point: `scripts/inference/score_transaction.py`
  - runtime invocation: Java launches the Python scorer per request
  - live score is not only the raw model probability; it is calibrated with contextual heuristics before the final ML decision is made

## How the Hybrid Decision Works

The API response combines three sources:

- ML output:
  - `fraudScore`
  - `fraudThreshold`
  - `fraudModelName`
  - `fraudModelDecision`
- RAG retrieval output:
  - `query`
  - `retrievedTitles`
  - `retrievedContents`
- LLM final output:
  - `explanation`
  - `riskLevel`
  - `recommendedAction`

Important behavior:

- the ML model does not directly own the final business decision
- Gemini receives both the ML assessment and the retrieved knowledge
- after the LLM responds, the backend applies an additional calibration step to reduce overly aggressive `HIGH` or `BLOCK` outcomes for mixed or clearly low-risk cases

So the practical final decision used by the app is:

- `recommendedAction`
- `riskLevel`

## Current Repository Structure

```text
fraud-rag-system/
├── configs/                      # ML training + scoring configuration
├── data/
│   ├── raw/                      # local-only Kaggle CSVs
│   └── processed/                # local-only processed outputs
├── docs/                         # setup and testing support docs
├── frontend/                     # Angular UI
│   └── src/app/
│       ├── core/services/        # frontend API client
│       └── features/
│           ├── fraud-check/      # transaction analysis UI
│           └── audit-logs/       # audit log UI
├── scripts/
│   ├── inference/                # runtime scoring entry points
│   ├── testing/                  # post-training evaluation scripts
│   └── training/                 # training helpers
├── src/
│   ├── fraud_model/              # Python ML training + inference package
│   └── main/java/com/project/fraudsystem/
│       ├── audit/                # audit controller, entity, repo, service
│       └── rag/                  # RAG controllers, DTOs, config, services
├── docker-compose.yml            # PostgreSQL container
├── pom.xml                       # backend build
├── README.md
└── .gitignore
```

## Main Backend Modules

- `audit`
  - stores and exposes audit log records
- `rag/controller`
  - API entry points for sync analysis and progress streaming
- `rag/service`
  - query building
  - ML scoring orchestration
  - vector retrieval
  - LLM prompt / explanation flow
  - progress streaming
- `src/fraud_model`
  - model training, evaluation, and runtime scoring

## RAG Pipeline Details

- Query building:
  - [QueryBuilderService](src/main/java/com/project/fraudsystem/rag/service/QueryBuilderService.java) converts structured transaction input into a sentence-style query
  - it explicitly describes amount band, device novelty, international/domestic context, time of day, account age, and recent velocity
- Embedding:
  - [EmbeddingService](src/main/java/com/project/fraudsystem/rag/service/EmbeddingService.java) calls the configured Vertex embedding model and produces a float vector
- Vector retrieval:
  - [VectorSearchService](src/main/java/com/project/fraudsystem/rag/service/VectorSearchService.java) embeds the query and asks PostgreSQL for nearest chunks
  - [FraudKnowledgeRepository](src/main/java/com/project/fraudsystem/rag/repository/FraudKnowledgeRepository.java) retrieves the top 10 vector candidates using pgvector distance
- Consistency reranking:
  - [RetrievalConsistencyFilterService](src/main/java/com/project/fraudsystem/rag/service/RetrievalConsistencyFilterService.java) penalizes chunks that conflict with the request, for example:
    - low-value transaction vs high-amount chunk
    - low-velocity transaction vs burst/card-testing chunk
    - known device vs new-device chunk
    - established account vs young-account chunk
- Diversification:
  - [RetrievalDiversificationService](src/main/java/com/project/fraudsystem/rag/service/RetrievalDiversificationService.java) selects 3 final chunks
  - it prefers different bucket orders depending on whether the case looks clearly low risk, borderline, or strongly risky
- Prompting and explanation:
  - [PromptBuilderService](src/main/java/com/project/fraudsystem/rag/service/PromptBuilderService.java) injects request details, ML diagnostics, the semantic query, and retrieved chunks into the prompt
  - [LlmExplanationService](src/main/java/com/project/fraudsystem/rag/service/LlmExplanationService.java) sends that prompt to Gemini
  - [RagService](src/main/java/com/project/fraudsystem/rag/service/RagService.java) parses the LLM output and may apply a final decision calibration

## ML Pipeline Details

- Dataset:
  - Kaggle: `kartik2112/fraud-detection`
  - expected files:
    - `data/raw/fraudTrain.csv`
    - `data/raw/fraudTest.csv`
- Training configuration:
  - config file: `configs/train_config.yaml`
  - enabled training candidates:
    - XGBoost
    - LightGBM
    - CatBoost
  - disabled by default:
    - Logistic Regression
    - Random Forest
  - primary selection metric: `holdout_pr_auc`
- Current saved model artifacts:
  - best model in `artifacts/training_metadata.json`: `lightgbm`
  - model comparison metrics in `artifacts/all_model_metrics.json`
- Runtime scoring logic:
  - the raw model probability is produced by the saved best model
  - then [inference.py](src/fraud_model/inference.py) calibrates it with a contextual risk estimate
  - current calibration formula:
    - `0.55 * raw_model_score + 0.45 * contextual_risk`
  - contextual risk increases for:
    - new device
    - international transaction
    - higher amount
    - higher recent velocity
    - younger account
    - riskier categories such as `misc_net` and `shopping_net`
  - contextual risk decreases for:
    - known device
    - domestic transaction
    - low recent velocity
    - older account
    - routine categories such as `grocery_pos`, `gas_transport`, `food_dining`, `health_fitness`, `shopping_pos`
- Threshold behavior:
  - saved LightGBM artifact threshold in `artifacts/all_model_metrics.json`: `0.31`
  - current runtime config in `configs/train_config.yaml` applies `inference_threshold_floor: 0.55`
  - so the live ML decision threshold used by the API is effectively at least `0.55`

## Prerequisites

- Java 21 or later
- Maven wrapper support
- Python 3.9+
- Node.js + npm
- Docker
- Google Cloud / Vertex credentials for embeddings and Gemini

## Local Setup

### 1. Start PostgreSQL

```bash
docker compose up -d postgres
```

### 2. Prepare Python environment

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 3. Configure Google credentials

Use one of:

```bash
gcloud auth application-default login
```

or:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/service-account.json
```

### 4. Optional ML dataset placement

If you want to retrain the model locally, place:

- `fraudTrain.csv`
- `fraudTest.csv`

inside:

- `data/raw/`

These files are intentionally ignored by Git.

## Run the Application

### Backend

```bash
./mvnw spring-boot:run
```

Backend runs on:

- `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs on:

- `http://localhost:4200`

The frontend proxies `/api/*` calls to the backend.

## API Endpoints

### Fraud analysis

- `POST /api/rag/query`
  - synchronous analysis

- `POST /api/rag/query/start`
  - starts async analysis and returns `analysisId`

- `GET /api/rag/query/progress/{analysisId}`
  - server-sent events stream for live step progress

### Audit

- `GET /api/audit/all`
  - returns audit log entries

## Example Fraud Request

```json
{
  "amount": 42.75,
  "merchantCategory": "grocery_pos",
  "deviceType": "mobile",
  "newDevice": false,
  "international": false,
  "transactionTimestamp": "2026-04-02T11:20:00",
  "accountAgeDays": 720,
  "transactionsLast24h": 1,
  "merchant": "whole_foods",
  "ccNum": "4532015112830366",
  "dob": "1988-06-10",
  "gender": "female",
  "city": "New York",
  "state": "NY",
  "job": "software_engineer",
  "zip": "10001",
  "cityPopulation": 8804190,
  "latitude": 40.7128,
  "longitude": -74.0060,
  "merchantLatitude": 40.7150,
  "merchantLongitude": -74.0110
}
```

## What the Response Contains

- `query`
  - the semantic search sentence built from the request
- `retrievedTitles`
  - titles of the final RAG chunks used in the prompt
- `retrievedContents`
  - contents of the final RAG chunks used in the prompt
- `fraudScore`
  - calibrated ML fraud score
- `fraudThreshold`
  - threshold used for the raw ML decision
- `fraudModelName`
  - currently `lightgbm`
- `fraudModelDecision`
  - `FRAUD` or `LEGIT` from the ML layer alone
- `explanation`
  - Gemini-generated narrative explanation
- `riskLevel`
  - final hybrid severity: `LOW`, `MEDIUM`, or `HIGH`
- `recommendedAction`
  - final hybrid action: `ALLOW`, `REVIEW`, or `BLOCK`
- `latencyMs`
  - end-to-end backend latency for the analysis

## Model Notes

- current live scoring uses the best saved model from `artifacts/`
- current saved best model is `lightgbm`
- runtime scoring is performed via `scripts/inference/score_transaction.py`
- the training pipeline can train XGBoost, LightGBM, and CatBoost
- `configs/train_config.yaml` currently requests:
  - `threshold_strategy: precision_balanced`
  - `inference_threshold_floor: 0.55`
- if you retrain, new metadata and model artifacts will be written into `artifacts/`

## Quality / Maintenance Notes

Current codebase priorities after Phase-1:

- keep the integrated backend + frontend workflow stable
- avoid large folder moves that break imports or runtime paths
- improve naming, comments, and docs incrementally
- separate future work into:
  - model retraining and evaluation
  - backend cleanup / test coverage
  - frontend polish

Additional project docs:

- testing workflow: `docs/testing.md`
- Spring Boot notes: `docs/spring-help.md`

## Recommended Git Workflow

Before pushing:

```bash
git status
git add .
git commit -m "Phase 1 integration and documentation cleanup"
git push origin Mohit-Naman-Vipul
```

If `main` is still empty, treat your integration branch as the release candidate and merge to `main` only after final verification.

## Verification Checklist

- backend starts on `8080`
- frontend starts on `4200`
- `POST /api/rag/query` works
- progress streaming works
- audit logs are written
- frontend displays fraud result and audit logs correctly

## Related Docs

- [Testing Guide](docs/testing.md)
- [Spring Starter Notes](docs/spring-help.md)
