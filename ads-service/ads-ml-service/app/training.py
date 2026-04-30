"""
Model training service.
Trains all 6 models from the notebook with MLFlow tracking.
Mirrors the exact pipeline from publicite.ipynb.
"""

import logging
import os
from datetime import datetime
from typing import Dict, List, Optional, Tuple

import joblib
import mlflow
import mlflow.sklearn
import numpy as np
import pandas as pd
from sklearn.dummy import DummyClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    roc_auc_score,
)
from sklearn.model_selection import LeaveOneOut
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.svm import SVC
from sklearn.tree import DecisionTreeClassifier

from app.config import settings

logger = logging.getLogger(__name__)

# ── Model definitions (same hyperparams as notebook) ──────────────────

MODEL_CONFIGS = {
    "Dummy": {
        "cls": DummyClassifier,
        "params": {"strategy": "most_frequent", "random_state": 42},
        "proba": False,
    },
    "LogisticRegression": {
        "cls": LogisticRegression,
        "params": {
            "C": 1.0,
            "max_iter": 1000,
            "class_weight": "balanced",
            "random_state": 42,
            "solver": "lbfgs",
        },
        "proba": True,
    },
    "DecisionTree": {
        "cls": DecisionTreeClassifier,
        "params": {
            "max_depth": 3,
            "min_samples_leaf": 2,
            "class_weight": "balanced",
            "random_state": 42,
        },
        "proba": True,
    },
    "RandomForest": {
        "cls": RandomForestClassifier,
        "params": {
            "n_estimators": 200,
            "max_depth": 4,
            "min_samples_leaf": 2,
            "class_weight": "balanced",
            "random_state": 42,
            "n_jobs": -1,
        },
        "proba": True,
    },
    "SVM": {
        "cls": SVC,
        "params": {
            "C": 1.0,
            "kernel": "rbf",
            "gamma": "scale",
            "probability": True,
            "class_weight": "balanced",
            "random_state": 42,
        },
        "proba": True,
    },
    "KNN": {
        "cls": KNeighborsClassifier,
        "params": {
            "n_neighbors": 3,
            "weights": "distance",
            "metric": "euclidean",
            "n_jobs": -1,
        },
        "proba": True,
    },
}


def evaluate_loo(
    model_cls, model_params: dict, X: np.ndarray, y: np.ndarray, has_proba: bool = True
) -> Dict:
    """
    Leave-One-Out Cross-Validation evaluation.
    Exact replica of the notebook's evaluate_loo function.
    """
    loo = LeaveOneOut()
    y_true_all, y_pred_all, y_prob_all = [], [], []

    check_proba = has_proba and hasattr(model_cls(**model_params), "predict_proba")

    for train_idx, test_idx in loo.split(X):
        X_tr, X_te = X[train_idx], X[test_idx]
        y_tr, y_te = y[train_idx], y[test_idx]

        sc = StandardScaler()
        X_tr = sc.fit_transform(X_tr)
        X_te = sc.transform(X_te)

        clf = model_cls(**model_params)
        clf.fit(X_tr, y_tr)

        y_true_all.append(y_te[0])
        y_pred_all.append(clf.predict(X_te)[0])
        if check_proba:
            y_prob_all.append(clf.predict_proba(X_te)[0][1])

    y_true_all = np.array(y_true_all)
    y_pred_all = np.array(y_pred_all)
    y_prob_all = np.array(y_prob_all) if y_prob_all else None

    acc = accuracy_score(y_true_all, y_pred_all)
    f1 = f1_score(y_true_all, y_pred_all, average="weighted", zero_division=0)
    roc = (
        roc_auc_score(y_true_all, y_prob_all)
        if y_prob_all is not None and len(np.unique(y_true_all)) > 1
        else 0.0
    )

    return {
        "accuracy": acc,
        "f1": f1,
        "roc_auc": roc,
        "y_true": y_true_all,
        "y_pred": y_pred_all,
        "y_prob": y_prob_all,
    }


class ModelTrainer:
    """Manages training, evaluation, and persistence of all ML models."""

    def __init__(self):
        self.trained_models: Dict[str, object] = {}
        self.scalers: Dict[str, StandardScaler] = {}
        self.label_encoder: Optional[LabelEncoder] = None
        self.loo_results: Dict[str, Dict] = {}
        self.last_trained: Optional[str] = None
        self.n_training_samples: int = 0
        self.best_model_name: str = ""
        self._setup_mlflow()

    def _setup_mlflow(self):
        """Initialize MLFlow tracking."""
        try:
            mlflow.set_tracking_uri(settings.MLFLOW_TRACKING_URI)
            mlflow.set_experiment(settings.MLFLOW_EXPERIMENT_NAME)
            logger.info(f"[MLFlow] Connected to {settings.MLFLOW_TRACKING_URI}")
        except Exception as e:
            logger.warning(f"[MLFlow] Could not connect: {e}. Logging locally.")

    def train_all(self, df: pd.DataFrame) -> List[Dict]:
        """
        Train all 6 models on the provided ad-level feature dataframe.
        Returns list of training results.
        """
        features = settings.NUMERIC_FEATURES

        if len(df) < settings.MIN_SAMPLES_FOR_TRAINING:
            raise ValueError(
                f"Need at least {settings.MIN_SAMPLES_FOR_TRAINING} samples, got {len(df)}"
            )

        # Encode target
        self.label_encoder = LabelEncoder()
        df = df.copy()
        df["label"] = self.label_encoder.fit_transform(df["createdBy"])  # AI=0, HUMAN=1

        X = df[features].values
        y = df["label"].values
        self.n_training_samples = len(y)

        logger.info(
            f"[Training] Starting training on {len(y)} samples, "
            f"{len(features)} features, classes={list(self.label_encoder.classes_)}"
        )

        results = []
        best_acc = -1.0

        for model_name, config in MODEL_CONFIGS.items():
            logger.info(f"[Training] Training {model_name}...")

            # LOO-CV evaluation
            loo_res = evaluate_loo(
                config["cls"], config["params"], X, y, has_proba=config["proba"]
            )
            self.loo_results[model_name] = loo_res

            # Train final model on ALL data (for production predictions)
            scaler = StandardScaler()
            X_scaled = scaler.fit_transform(X)
            model = config["cls"](**config["params"])
            model.fit(X_scaled, y)

            self.trained_models[model_name] = model
            self.scalers[model_name] = scaler

            # Track best model
            if loo_res["accuracy"] > best_acc:
                best_acc = loo_res["accuracy"]
                self.best_model_name = model_name

            # MLFlow logging
            run_id = self._log_to_mlflow(model_name, config, loo_res, model, scaler)

            result = {
                "model_name": model_name,
                "accuracy_loo": round(loo_res["accuracy"], 4),
                "f1_loo": round(loo_res["f1"], 4),
                "roc_auc_loo": round(loo_res["roc_auc"], 4),
                "n_samples": len(y),
                "mlflow_run_id": run_id,
            }
            results.append(result)

            logger.info(
                f"[Training] {model_name}: "
                f"Acc={loo_res['accuracy']:.4f} "
                f"F1={loo_res['f1']:.4f} "
                f"AUC={loo_res['roc_auc']:.4f}"
            )

        # Persist models to disk
        self._save_models()
        self.last_trained = datetime.utcnow().isoformat()

        logger.info(
            f"[Training] Complete. Best model: {self.best_model_name} "
            f"(Acc={best_acc:.4f})"
        )
        return results

    def _log_to_mlflow(
        self, model_name: str, config: dict, loo_res: dict, model, scaler
    ) -> Optional[str]:
        """Log training run to MLFlow."""
        try:
            with mlflow.start_run(run_name=f"{model_name}_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}"):
                mlflow.log_params(
                    {f"param_{k}": str(v) for k, v in config["params"].items()}
                )
                mlflow.log_param("model_class", config["cls"].__name__)
                mlflow.log_param("n_features", len(settings.NUMERIC_FEATURES))
                mlflow.log_param("n_samples", self.n_training_samples)

                mlflow.log_metric("accuracy_loo", loo_res["accuracy"])
                mlflow.log_metric("f1_loo", loo_res["f1"])
                mlflow.log_metric("roc_auc_loo", loo_res["roc_auc"])

                mlflow.sklearn.log_model(model, artifact_path=f"model_{model_name}")

                return mlflow.active_run().info.run_id
        except Exception as e:
            logger.warning(f"[MLFlow] Failed to log {model_name}: {e}")
            return None

    def _save_models(self):
        """Persist trained models and scalers to disk."""
        os.makedirs(settings.MODEL_DIR, exist_ok=True)

        for name, model in self.trained_models.items():
            model_path = os.path.join(settings.MODEL_DIR, f"{name}_model.pkl")
            scaler_path = os.path.join(settings.MODEL_DIR, f"{name}_scaler.pkl")
            joblib.dump(model, model_path)
            joblib.dump(self.scalers[name], scaler_path)

        if self.label_encoder:
            le_path = os.path.join(settings.MODEL_DIR, "label_encoder.pkl")
            joblib.dump(self.label_encoder, le_path)

        # Save LOO results for benchmarking
        results_path = os.path.join(settings.MODEL_DIR, "loo_results.pkl")
        serializable = {}
        for k, v in self.loo_results.items():
            serializable[k] = {
                "accuracy": v["accuracy"],
                "f1": v["f1"],
                "roc_auc": v["roc_auc"],
            }
        joblib.dump(serializable, results_path)

        logger.info(f"[Training] Models saved to {settings.MODEL_DIR}")

    def load_models(self) -> bool:
        """Load persisted models from disk."""
        try:
            le_path = os.path.join(settings.MODEL_DIR, "label_encoder.pkl")
            if not os.path.exists(le_path):
                logger.warning("[Training] No saved models found")
                return False

            self.label_encoder = joblib.load(le_path)

            for name in MODEL_CONFIGS.keys():
                model_path = os.path.join(settings.MODEL_DIR, f"{name}_model.pkl")
                scaler_path = os.path.join(settings.MODEL_DIR, f"{name}_scaler.pkl")
                if os.path.exists(model_path) and os.path.exists(scaler_path):
                    self.trained_models[name] = joblib.load(model_path)
                    self.scalers[name] = joblib.load(scaler_path)

            results_path = os.path.join(settings.MODEL_DIR, "loo_results.pkl")
            if os.path.exists(results_path):
                self.loo_results = joblib.load(results_path)

            # Determine best model
            best_acc = -1.0
            for name, res in self.loo_results.items():
                if res["accuracy"] > best_acc:
                    best_acc = res["accuracy"]
                    self.best_model_name = name

            logger.info(
                f"[Training] Loaded {len(self.trained_models)} models from disk"
            )
            return True
        except Exception as e:
            logger.error(f"[Training] Failed to load models: {e}")
            return False

    def predict(self, features: np.ndarray, model_name: Optional[str] = None) -> List[Dict]:
        """
        Generate predictions from all (or one) model(s).
        features: 1D array of shape (n_features,)
        """
        if not self.trained_models:
            raise RuntimeError("No trained models available. Train first.")

        models_to_use = (
            {model_name: self.trained_models[model_name]}
            if model_name and model_name in self.trained_models
            else self.trained_models
        )

        predictions = []
        X = features.reshape(1, -1)

        for name, model in models_to_use.items():
            scaler = self.scalers[name]
            X_scaled = scaler.transform(X)

            pred_label = model.predict(X_scaled)[0]
            pred_class = self.label_encoder.inverse_transform([pred_label])[0]

            prob_ai, prob_human = 0.5, 0.5
            if hasattr(model, "predict_proba"):
                try:
                    probs = model.predict_proba(X_scaled)[0]
                    prob_ai = float(probs[0])
                    prob_human = float(probs[1])
                except Exception:
                    pass

            loo = self.loo_results.get(name, {})
            predictions.append(
                {
                    "model_name": name,
                    "predicted_class": pred_class,
                    "probability_ai": round(prob_ai, 4),
                    "probability_human": round(prob_human, 4),
                    "accuracy_loo": round(loo.get("accuracy", 0), 4),
                    "f1_loo": round(loo.get("f1", 0), 4),
                    "roc_auc_loo": round(loo.get("roc_auc", 0), 4),
                }
            )

        return predictions

    @property
    def is_ready(self) -> bool:
        return len(self.trained_models) > 0


# Singleton instance
trainer = ModelTrainer()
