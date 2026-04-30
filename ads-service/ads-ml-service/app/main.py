"""
Ads-ML-Service — FastAPI Application
Real-time ML prediction service for AI vs HUMAN ad classification.

Architecture:
  Kafka (ad-events) → EventBuffer → Feature Aggregation → 6 ML Models → Predictions
  ClickHouse → Training Data → LOO-CV Evaluation → MLFlow Tracking
"""

import logging
from contextlib import asynccontextmanager
from datetime import datetime
from typing import Optional

import numpy as np
import pandas as pd
from apscheduler.schedulers.background import BackgroundScheduler
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.clickhouse_client import fetch_ad_features, fetch_single_ad_features
from app.config import settings
from app.kafka_consumer import event_buffer, kafka_consumer
from app.models import (
    BenchmarkResponse,
    BulkPredictionResponse,
    HealthResponse,
    ModelPerformance,
    PredictionResponse,
    SinglePrediction,
    TrainingResponse,
    TrainingResult,
)
from app.training import trainer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

scheduler = BackgroundScheduler()


def scheduled_retrain():
    """Periodic retraining job triggered by APScheduler."""
    logger.info("[Scheduler] Checking for retraining...")
    try:
        df = fetch_ad_features()
        if df.empty or len(df) < settings.MIN_SAMPLES_FOR_TRAINING:
            logger.info(
                f"[Scheduler] Not enough data ({len(df)} ads). "
                f"Need {settings.MIN_SAMPLES_FOR_TRAINING}."
            )
            return
        trainer.train_all(df)
        logger.info("[Scheduler] Retraining complete.")
    except Exception as e:
        logger.error(f"[Scheduler] Retraining failed: {e}")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup and shutdown lifecycle."""
    # Startup
    logger.info(f"[Startup] {settings.APP_NAME} v{settings.APP_VERSION}")

    # 1. Try loading saved models
    loaded = trainer.load_models()
    if loaded:
        logger.info("[Startup] Pre-trained models loaded from disk.")
    else:
        logger.info("[Startup] No saved models. Will train on first request or schedule.")

    # 2. Start Kafka consumer
    try:
        kafka_consumer.start()
    except Exception as e:
        logger.warning(f"[Startup] Kafka consumer failed to start: {e}")

    # 3. Start retraining scheduler
    scheduler.add_job(
        scheduled_retrain,
        "interval",
        minutes=settings.RETRAIN_INTERVAL_MINUTES,
        id="retrain_job",
        replace_existing=True,
    )
    scheduler.start()
    logger.info(
        f"[Startup] Retraining scheduled every {settings.RETRAIN_INTERVAL_MINUTES} min"
    )

    yield

    # Shutdown
    scheduler.shutdown(wait=False)
    kafka_consumer.stop()
    logger.info("[Shutdown] Ads-ML-Service stopped.")


app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION,
    description=(
        "Real-time ML service for classifying ads as AI-generated or HUMAN-created "
        "based on user engagement behavior (VIEW, CLICK, HOVER). "
        "Consumes events from Kafka, aggregates features from ClickHouse, "
        "trains 6 ML models with MLFlow tracking."
    ),
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ═══════════════════════════════════════════════════════════════════════
# HEALTH & STATUS
# ═══════════════════════════════════════════════════════════════════════


@app.get("/health", response_model=HealthResponse, tags=["Health"])
def health_check():
    """Service health check with connectivity status."""
    ch_ok = False
    try:
        from app.clickhouse_client import get_client
        client = get_client()
        client.query("SELECT 1")
        ch_ok = True
    except Exception:
        pass

    mlflow_ok = False
    try:
        import mlflow
        mlflow.set_tracking_uri(settings.MLFLOW_TRACKING_URI)
        mlflow.search_experiments(max_results=1)
        mlflow_ok = True
    except Exception:
        pass

    return HealthResponse(
        status="healthy" if trainer.is_ready else "warming_up",
        service=settings.APP_NAME,
        version=settings.APP_VERSION,
        kafka_connected=kafka_consumer.is_connected,
        clickhouse_connected=ch_ok,
        mlflow_connected=mlflow_ok,
        models_loaded=trainer.is_ready,
        n_models=len(trainer.trained_models),
    )


@app.get("/status", tags=["Health"])
def status():
    """Quick operational status."""
    return {
        "models_loaded": trainer.is_ready,
        "n_models": len(trainer.trained_models),
        "best_model": trainer.best_model_name or "N/A",
        "last_trained": trainer.last_trained,
        "n_training_samples": trainer.n_training_samples,
        "kafka_buffer_ads": event_buffer.n_ads,
        "kafka_buffer_events": event_buffer.n_events_total,
    }


# ═══════════════════════════════════════════════════════════════════════
# TRAINING
# ═══════════════════════════════════════════════════════════════════════


@app.post("/train", response_model=TrainingResponse, tags=["Training"])
def train_models():
    """
    Trigger model training from ClickHouse data.
    Trains all 6 models (Dummy, LR, DT, RF, SVM, KNN) with LOO-CV
    and logs results to MLFlow.
    """
    try:
        df = fetch_ad_features()
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"ClickHouse unavailable: {e}")

    if df.empty or len(df) < settings.MIN_SAMPLES_FOR_TRAINING:
        raise HTTPException(
            status_code=400,
            detail=f"Not enough data: {len(df)} ads found, "
            f"need {settings.MIN_SAMPLES_FOR_TRAINING}",
        )

    try:
        results = trainer.train_all(df)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Training failed: {e}")

    return TrainingResponse(
        status="success",
        n_samples=trainer.n_training_samples,
        models=[TrainingResult(**r) for r in results],
        best_model=trainer.best_model_name,
        best_accuracy=max(r["accuracy_loo"] for r in results),
    )


# ═══════════════════════════════════════════════════════════════════════
# PREDICTION
# ═══════════════════════════════════════════════════════════════════════


@app.get("/predict/{ad_id}", response_model=PredictionResponse, tags=["Prediction"])
def predict_ad(ad_id: int, model: Optional[str] = None):
    """
    Predict whether an ad (by ID) was created by AI or HUMAN.
    Fetches features from ClickHouse, runs all 6 models.
    Optionally filter by ?model=RandomForest
    """
    if not trainer.is_ready:
        raise HTTPException(status_code=503, detail="Models not trained yet. POST /train first.")

    # Try ClickHouse first, fall back to Kafka buffer
    df = None
    try:
        df = fetch_single_ad_features(ad_id)
    except Exception as e:
        logger.warning(f"[Predict] ClickHouse failed for ad {ad_id}: {e}")

    if df is None or df.empty:
        # Fall back to Kafka buffer
        stats = event_buffer.get_ad_stats(ad_id)
        if stats is None:
            raise HTTPException(
                status_code=404,
                detail=f"Ad {ad_id} not found in ClickHouse or Kafka buffer",
            )
        df = pd.DataFrame([stats])

    features = settings.NUMERIC_FEATURES
    X = df[features].values[0]
    feature_dict = {f: float(X[i]) for i, f in enumerate(features)}

    predictions = trainer.predict(X, model_name=model)

    # Find recommended prediction (from best model)
    best_pred = next(
        (p for p in predictions if p["model_name"] == trainer.best_model_name),
        predictions[0] if predictions else None,
    )

    return PredictionResponse(
        ad_id=ad_id,
        features=feature_dict,
        predictions=[SinglePrediction(**p) for p in predictions],
        recommended_model=trainer.best_model_name,
        recommended_prediction=best_pred["predicted_class"] if best_pred else "UNKNOWN",
        recommended_confidence=max(
            best_pred.get("probability_ai", 0.5),
            best_pred.get("probability_human", 0.5),
        )
        if best_pred
        else 0.5,
    )


@app.get("/predict-all", response_model=BulkPredictionResponse, tags=["Prediction"])
def predict_all_ads():
    """Predict all ads currently in ClickHouse."""
    if not trainer.is_ready:
        raise HTTPException(status_code=503, detail="Models not trained yet.")

    try:
        df = fetch_ad_features()
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"ClickHouse unavailable: {e}")

    if df.empty:
        raise HTTPException(status_code=404, detail="No ads found")

    features = settings.NUMERIC_FEATURES
    results = []

    for _, row in df.iterrows():
        X = row[features].values.astype(float)
        feature_dict = {f: float(X[i]) for i, f in enumerate(features)}
        predictions = trainer.predict(X)

        best_pred = next(
            (p for p in predictions if p["model_name"] == trainer.best_model_name),
            predictions[0],
        )

        results.append(
            PredictionResponse(
                ad_id=int(row["adId"]),
                features=feature_dict,
                predictions=[SinglePrediction(**p) for p in predictions],
                recommended_model=trainer.best_model_name,
                recommended_prediction=best_pred["predicted_class"],
                recommended_confidence=max(
                    best_pred.get("probability_ai", 0.5),
                    best_pred.get("probability_human", 0.5),
                ),
            )
        )

    return BulkPredictionResponse(total_ads=len(results), predictions=results)


# ═══════════════════════════════════════════════════════════════════════
# BENCHMARKING
# ═══════════════════════════════════════════════════════════════════════


@app.get("/benchmark", response_model=BenchmarkResponse, tags=["Benchmarking"])
def get_benchmark():
    """
    Returns performance benchmarking of all 6 trained models.
    Shows LOO-CV Accuracy, F1, ROC-AUC for each model.
    """
    if not trainer.loo_results:
        raise HTTPException(status_code=503, detail="No training results. POST /train first.")

    models = []
    for name, res in trainer.loo_results.items():
        acc = res.get("accuracy", 0)
        n_errors = int(round((1 - acc) * trainer.n_training_samples))
        models.append(
            ModelPerformance(
                model_name=name,
                accuracy_loo=round(acc, 4),
                f1_loo=round(res.get("f1", 0), 4),
                roc_auc_loo=round(res.get("roc_auc", 0), 4),
                errors=f"{n_errors}/{trainer.n_training_samples}",
                status="perfect" if acc == 1.0 else ("good" if acc >= 0.9 else "baseline"),
            )
        )

    return BenchmarkResponse(
        models=models,
        best_model=trainer.best_model_name,
        last_trained=trainer.last_trained,
        n_training_samples=trainer.n_training_samples,
    )


# ═══════════════════════════════════════════════════════════════════════
# KAFKA BUFFER (real-time monitoring)
# ═══════════════════════════════════════════════════════════════════════


@app.get("/kafka/stats", tags=["Kafka"])
def kafka_stats():
    """Real-time stats from the Kafka event buffer."""
    return {
        "consumer_connected": kafka_consumer.is_connected,
        "buffered_ads": event_buffer.n_ads,
        "buffered_events": event_buffer.n_events_total,
        "topic": settings.KAFKA_TOPIC_AD_EVENTS,
    }


@app.get("/kafka/ad/{ad_id}", tags=["Kafka"])
def kafka_ad_stats(ad_id: int):
    """Get buffered event stats for a specific ad from Kafka stream."""
    stats = event_buffer.get_ad_stats(ad_id)
    if stats is None:
        raise HTTPException(status_code=404, detail=f"Ad {ad_id} not in Kafka buffer")
    return stats
