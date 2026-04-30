# Ads-ML-Service

Real-time ML prediction service for classifying ads as **AI-generated** or **HUMAN-created** based on user engagement behavior (VIEW, CLICK, HOVER).

## Architecture

```
┌──────────────┐     ┌─────────┐     ┌────────────────────┐     ┌──────────┐
│  Frontend    │────→│  Kafka  │────→│  Ads-ML-Service    │────→│  MLFlow  │
│  (Angular)   │     │ad-events│     │  (FastAPI :8099)   │     │  (:5000) │
└──────────────┘     └────┬────┘     │                    │     └──────────┘
                          │          │  6 ML Models:      │
┌──────────────┐          │          │  - Dummy Baseline  │
│  ads-service │──────────┘          │  - Logistic Reg.   │
│  (Spring     │                     │  - Decision Tree   │
│   Boot:8090) │←── HTTP ───────────→│  - Random Forest   │
└──────────────┘                     │  - SVM (RBF)       │
                                     │  - KNN (k=3)       │
┌──────────────┐                     │                    │
│  ClickHouse  │←── SQL Queries ────→│  Feature Pipeline  │
│  (:8123)     │                     └────────────────────┘
└──────────────┘
```

## Data Flow

1. **Frontend** captures user interactions (VIEW, HOVER, CLICK) on ads
2. **ads-service** (Spring Boot) publishes events to Kafka topic `ad-events`
3. **ClickHouse** ingests events via Kafka Engine (Materialized View)
4. **Ads-ML-Service** does two things:
   - **Kafka Consumer**: Buffers events in real-time for instant predictions
   - **ClickHouse Reader**: Aggregates historical events for training
5. **MLFlow** tracks all training runs, metrics, and model artifacts

## Features (from notebook)

9 numeric features aggregated per ad:

| Feature | Description |
|---|---|
| `n_views` | Total VIEW events |
| `n_clicks` | Total CLICK events |
| `n_hovers` | Total HOVER events |
| `n_events` | Total interaction events |
| `n_unique_users` | Distinct users interacting |
| `ctr` | Click-Through Rate = n_clicks / n_views |
| `hover_rate` | Hover Rate = n_hovers / n_views |
| `click_per_event` | Click density = n_clicks / n_events |
| `lifespan_hours` | Time span of interactions (hours) |

## API Endpoints

### Health
- `GET /health` — Full connectivity check (Kafka, ClickHouse, MLFlow)
- `GET /status` — Quick operational status

### Training
- `POST /train` — Trigger training of all 6 models from ClickHouse data (LOO-CV + MLFlow)

### Prediction
- `GET /predict/{ad_id}` — Predict AI/HUMAN for a single ad (all 6 models)
- `GET /predict/{ad_id}?model=RandomForest` — Predict with a specific model
- `GET /predict-all` — Bulk predict all ads in ClickHouse

### Benchmarking
- `GET /benchmark` — Performance comparison of all 6 models (LOO-CV metrics)

### Kafka Monitoring
- `GET /kafka/stats` — Real-time Kafka consumer buffer stats
- `GET /kafka/ad/{ad_id}` — Buffered events for a specific ad

## Quick Start

### Option 1: With parent docker-compose (recommended)

```bash
cd ads-service/
docker-compose up --build
```

This starts everything: Kafka, ClickHouse, MLFlow, ads-ml-service.

### Option 2: Standalone (if ads-service stack is already running)

```bash
cd ads-service/ads-ml-service/
docker-compose up --build
```

Make sure the parent stack's network is named `ads-service_default`.

### First-time setup

1. Start all services
2. Wait for Kafka/ClickHouse to be ready
3. Trigger initial training:
   ```bash
   curl -X POST http://localhost:8099/train
   ```
4. Make predictions:
   ```bash
   curl http://localhost:8099/predict/0
   ```
5. View benchmark:
   ```bash
   curl http://localhost:8099/benchmark
   ```
6. Open MLFlow UI: http://localhost:5000

## Model Performance (from notebook LOO-CV)

| Model | Accuracy | F1 | ROC-AUC |
|---|---|---|---|
| Dummy Classifier | 0.000 | 0.000 | N/A |
| Logistic Regression | 1.000 | 1.000 | 1.000 |
| Decision Tree | 1.000 | 1.000 | 1.000 |
| Random Forest | 1.000 | 1.000 | 1.000 |
| SVM (RBF) | 0.900 | 0.900 | 0.990 |
| KNN (k=3) | 0.950 | 0.950 | 0.980 |

## Automatic Retraining

The service automatically retrains every 30 minutes (configurable via `RETRAIN_INTERVAL_MINUTES`). This ensures the models stay up-to-date as new ad events flow through Kafka.

## Ports

| Service | Port | URL |
|---|---|---|
| Ads-ML-Service (FastAPI) | 8099 | http://localhost:8099/docs |
| MLFlow UI | 5000 | http://localhost:5000 |
| Kafka UI | 8085 | http://localhost:8085 |
| ClickHouse HTTP | 8123 | http://localhost:8123 |
| ads-service (Spring Boot) | 8090 | http://localhost:8090 |
