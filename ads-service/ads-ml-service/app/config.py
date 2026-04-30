"""
Ads-ML-Service Configuration
Centralized settings loaded from environment variables.
"""

from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    # --- API ---
    APP_NAME: str = "Ads-ML-Service"
    APP_VERSION: str = "1.0.0"
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8099

    # --- Kafka ---
    KAFKA_BOOTSTRAP_SERVERS: str = "kafka:29092"
    KAFKA_TOPIC_AD_EVENTS: str = "ad-events"
    KAFKA_TOPIC_ML_PREDICTIONS: str = "ml-predictions"
    KAFKA_CONSUMER_GROUP: str = "ads-ml-service"

    # --- ClickHouse ---
    CLICKHOUSE_HOST: str = "clickhouse-server"
    CLICKHOUSE_PORT: int = 8123
    CLICKHOUSE_DB: str = "ads_analytics"
    CLICKHOUSE_USER: str = "default"
    CLICKHOUSE_PASSWORD: str = ""

    # --- MLFlow ---
    MLFLOW_TRACKING_URI: str = "http://mlflow:5000"
    MLFLOW_EXPERIMENT_NAME: str = "ads-ai-vs-human"

    # --- Ads-Service (Spring Boot backend) ---
    ADS_SERVICE_URL: str = "http://ads-service:8090"

    # --- Model ---
    MODEL_DIR: str = "/app/models"
    RETRAIN_INTERVAL_MINUTES: int = 30
    MIN_SAMPLES_FOR_TRAINING: int = 10

    # --- Features ---
    NUMERIC_FEATURES: List[str] = [
        "n_views", "n_clicks", "n_hovers", "n_events",
        "n_unique_users", "ctr", "hover_rate", "click_per_event",
        "lifespan_hours"
    ]

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
