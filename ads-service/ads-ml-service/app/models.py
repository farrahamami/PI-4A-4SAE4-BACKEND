"""
Pydantic schemas for API requests/responses and internal data structures.
"""

from typing import Dict, List, Optional
from pydantic import BaseModel


# --- Kafka Event Schema ---
class AdEventMessage(BaseModel):
    adId: int
    type: str  # VIEW, CLICK, HOVER, CREATION
    createdBy: str  # AI, HUMAN
    userId: Optional[int] = None
    timestamp: Optional[str] = None
    ip: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    city: Optional[str] = None


# --- Prediction ---
class PredictionRequest(BaseModel):
    ad_id: int


class SinglePrediction(BaseModel):
    model_name: str
    predicted_class: str  # AI or HUMAN
    probability_ai: float
    probability_human: float
    accuracy_loo: Optional[float] = None
    f1_loo: Optional[float] = None
    roc_auc_loo: Optional[float] = None


class PredictionResponse(BaseModel):
    ad_id: int
    features: Dict[str, float]
    predictions: List[SinglePrediction]
    recommended_model: str
    recommended_prediction: str
    recommended_confidence: float


class BulkPredictionResponse(BaseModel):
    total_ads: int
    predictions: List[PredictionResponse]


# --- Training ---
class TrainingResult(BaseModel):
    model_name: str
    accuracy_loo: float
    f1_loo: float
    roc_auc_loo: float
    n_samples: int
    mlflow_run_id: Optional[str] = None


class TrainingResponse(BaseModel):
    status: str
    n_samples: int
    models: List[TrainingResult]
    best_model: str
    best_accuracy: float


# --- Model Performance ---
class ModelPerformance(BaseModel):
    model_name: str
    accuracy_loo: float
    f1_loo: float
    roc_auc_loo: float
    errors: str
    status: str


class BenchmarkResponse(BaseModel):
    models: List[ModelPerformance]
    best_model: str
    last_trained: Optional[str] = None
    n_training_samples: int


# --- Health ---
class HealthResponse(BaseModel):
    status: str
    service: str
    version: str
    kafka_connected: bool
    clickhouse_connected: bool
    mlflow_connected: bool
    models_loaded: bool
    n_models: int
