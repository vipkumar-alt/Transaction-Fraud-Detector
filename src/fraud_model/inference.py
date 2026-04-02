from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List

import joblib
import numpy as np
import pandas as pd
from catboost import CatBoostClassifier

from .config import TrainConfig
from .features import add_behavioral_features
from .preprocessing import basic_preprocess


@dataclass
class ScoringBundle:
    model_name: str
    model: Any
    threshold: float
    feature_columns: List[str]
    categorical_columns: List[str]
    create_amount_bands: bool
    create_behavioral_features: bool
    rare_category_mapping: Dict[str, List[str]]


def load_scoring_bundle(config: TrainConfig) -> ScoringBundle:
    metadata_path = config.artifacts_dir / "training_metadata.json"
    if not metadata_path.exists():
        raise FileNotFoundError(f"Missing scoring metadata: {metadata_path}")

    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    model_name = metadata.get("best_model")
    if not model_name:
        raise ValueError(f"Missing 'best_model' in {metadata_path}")
    if not metadata.get("feature_columns"):
        raise ValueError(f"Missing 'feature_columns' in {metadata_path}")

    threshold = _load_threshold(config, metadata, model_name)

    if model_name == "catboost":
        model_path = config.artifacts_dir / "best_model_catboost.cbm"
        if not model_path.exists():
            raise FileNotFoundError(f"Missing CatBoost model artifact: {model_path}")
        model = CatBoostClassifier()
        model.load_model(str(model_path))
    else:
        model_path = config.artifacts_dir / "best_model.joblib"
        if not model_path.exists():
            raise FileNotFoundError(f"Missing model artifact: {model_path}")
        model = joblib.load(model_path)

    return ScoringBundle(
        model_name=model_name,
        model=model,
        threshold=threshold,
        feature_columns=list(metadata.get("feature_columns", [])),
        categorical_columns=list(metadata.get("categorical_columns", [])),
        create_amount_bands=bool(metadata.get("create_amount_bands", True)),
        create_behavioral_features=bool(metadata.get("create_behavioral_features", True)),
        rare_category_mapping=dict(metadata.get("rare_category_mapping", {})),
    )


def score_transaction(payload: Dict[str, Any], config: TrainConfig) -> Dict[str, Any]:
    bundle = load_scoring_bundle(config)
    features = build_feature_frame(payload, bundle)
    raw_fraud_score = predict_score(features, bundle)
    fraud_score = calibrate_score(raw_fraud_score, payload)

    return {
        "modelName": bundle.model_name,
        "fraudScore": fraud_score,
        "fraudThreshold": bundle.threshold,
        "modelDecision": "FRAUD" if fraud_score >= bundle.threshold else "LEGIT",
    }


def build_feature_frame(payload: Dict[str, Any], bundle: ScoringBundle) -> pd.DataFrame:
    raw_row = {
        "merchant": _optional_text(payload.get("merchant")),
        "category": _optional_text(payload.get("merchantCategory") or payload.get("category")),
        "amt": _optional_float(payload.get("amount") if payload.get("amount") is not None else payload.get("amt"), 0.0),
        "gender": _optional_text(payload.get("gender")),
        "city": _optional_text(payload.get("city")),
        "state": _optional_text(payload.get("state")),
        "job": _optional_text(payload.get("job")),
        "zip": _optional_text(payload.get("zip")),
        "lat": _optional_float(payload.get("latitude") if payload.get("latitude") is not None else payload.get("lat")),
        "long": _optional_float(payload.get("longitude") if payload.get("longitude") is not None else payload.get("long")),
        "merch_lat": _optional_float(
            payload.get("merchantLatitude") if payload.get("merchantLatitude") is not None else payload.get("merch_lat")
        ),
        "merch_long": _optional_float(
            payload.get("merchantLongitude") if payload.get("merchantLongitude") is not None else payload.get("merch_long")
        ),
        "city_pop": _optional_float(
            payload.get("cityPopulation") if payload.get("cityPopulation") is not None else payload.get("city_pop")
        ),
        "dob": _optional_text(payload.get("dob")),
        "cc_num": _optional_text(payload.get("ccNum") if payload.get("ccNum") is not None else payload.get("cc_num")) or "0",
        "trans_date_trans_time": _resolve_transaction_timestamp(payload),
    }

    frame = pd.DataFrame([raw_row])
    frame = basic_preprocess(frame, create_amount_bands=bundle.create_amount_bands)

    if bundle.create_behavioral_features:
        frame = add_behavioral_features(frame)

    _apply_runtime_overrides(frame, payload)
    _apply_rare_category_mapping(frame, bundle.rare_category_mapping)

    for column in bundle.feature_columns:
        if column in frame.columns:
            continue
        if column in bundle.categorical_columns:
            frame[column] = "__missing__"
        else:
            frame[column] = np.nan

    feature_frame = frame[bundle.feature_columns].copy()

    for column in bundle.feature_columns:
        if column in bundle.categorical_columns:
            feature_frame[column] = feature_frame[column].astype("string").fillna("__missing__")
        else:
            feature_frame[column] = pd.to_numeric(feature_frame[column], errors="coerce")

    return feature_frame


def predict_score(feature_frame: pd.DataFrame, bundle: ScoringBundle) -> float:
    if bundle.model_name == "catboost":
        catboost_frame = feature_frame.copy()
        for column in bundle.categorical_columns:
            if column in catboost_frame.columns:
                catboost_frame[column] = catboost_frame[column].astype("string").fillna("__missing__")
        probabilities = bundle.model.predict_proba(catboost_frame)[:, 1]
    else:
        probabilities = bundle.model.predict_proba(feature_frame)[:, 1]

    return float(probabilities[0])


def calibrate_score(raw_score: float, payload: Dict[str, Any]) -> float:
    contextual_risk = estimate_contextual_risk(payload)
    calibrated = (0.55 * raw_score) + (0.45 * contextual_risk)
    return float(min(max(calibrated, 0.0), 1.0))


def estimate_contextual_risk(payload: Dict[str, Any]) -> float:
    amount = _optional_float(payload.get("amount") if payload.get("amount") is not None else payload.get("amt"), 0.0) or 0.0
    account_age_days = _safe_int(payload.get("accountAgeDays"))
    transactions_last_24h = _safe_int(payload.get("transactionsLast24h"))
    merchant_category = (_optional_text(payload.get("merchantCategory") or payload.get("category")) or "").lower()
    new_device = bool(payload.get("newDevice"))
    international = bool(payload.get("international"))

    risk = 0.30

    if new_device:
        risk += 0.15
    else:
        risk -= 0.10

    if international:
        risk += 0.18
    else:
        risk -= 0.08

    if transactions_last_24h >= 6:
        risk += 0.12
    elif transactions_last_24h >= 3:
        risk += 0.06
    else:
        risk -= 0.08

    if amount >= 500.0:
        risk += 0.10
    elif amount >= 250.0:
        risk += 0.05
    elif amount <= 75.0:
        risk -= 0.10

    if account_age_days < 60:
        risk += 0.08
    elif account_age_days < 180:
        risk += 0.04
    elif account_age_days >= 365:
        risk -= 0.10

    if merchant_category in {"misc_net", "shopping_net"}:
        risk += 0.06
    elif merchant_category in {"entertainment"}:
        risk += 0.02
    elif merchant_category in {"grocery_pos", "gas_transport", "food_dining", "health_fitness", "shopping_pos"}:
        risk -= 0.08

    return float(min(max(risk, 0.02), 0.98))


def _load_threshold(config: TrainConfig, metadata: Dict[str, Any], model_name: str) -> float:
    threshold = metadata.get("best_threshold")
    if threshold is not None:
        return _apply_inference_threshold_floor(float(threshold), metadata, config.inference_threshold_floor)

    metrics_path = config.artifacts_dir / "all_model_metrics.json"
    if metrics_path.exists():
        metrics = json.loads(metrics_path.read_text(encoding="utf-8"))
        model_metrics = metrics.get(model_name, {})
        if "threshold" in model_metrics:
            return _apply_inference_threshold_floor(
                float(model_metrics["threshold"]),
                metadata,
                config.inference_threshold_floor
            )

    return _apply_inference_threshold_floor(0.5, metadata, config.inference_threshold_floor)


def _apply_inference_threshold_floor(threshold: float, metadata: Dict[str, Any], configured_floor: float) -> float:
    metadata_floor = metadata.get("inference_threshold_floor")
    effective_floor = configured_floor if metadata_floor is None else max(float(metadata_floor), configured_floor)
    return float(max(float(threshold), float(effective_floor)))


def _apply_rare_category_mapping(frame: pd.DataFrame, rare_mapping: Dict[str, List[str]]) -> None:
    for column, allowed_values in rare_mapping.items():
        if column not in frame.columns:
            continue
        allowed = set(allowed_values)
        values = frame[column].astype("string").fillna("__missing__")
        frame[column] = values.where(values.isin(allowed), "__other__")


def _apply_runtime_overrides(frame: pd.DataFrame, payload: Dict[str, Any]) -> None:
    tx_count_24h = payload.get("transactionsLast24h")
    if tx_count_24h is not None and tx_count_24h != "" and "txn_count_24h" in frame.columns:
        try:
            frame["txn_count_24h"] = max(int(tx_count_24h), 0)
        except (TypeError, ValueError):
            pass


def _resolve_transaction_timestamp(payload: Dict[str, Any]) -> str:
    timestamp = payload.get("transactionTimestamp") or payload.get("trans_date_trans_time")
    if isinstance(timestamp, str) and timestamp.strip():
        return timestamp.strip()

    time_only = payload.get("transactionTime")
    if isinstance(time_only, str) and time_only.strip():
        cleaned = time_only.strip()
        if len(cleaned) == 5:
            cleaned = f"{cleaned}:00"
        return f"{datetime.now().date().isoformat()}T{cleaned}"

    return datetime.now().replace(microsecond=0).isoformat()


def _optional_text(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _optional_float(value: Any, default: float | None = None) -> float | None:
    if value is None or value == "":
        return default
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _safe_int(value: Any) -> int:
    if value is None or value == "":
        return 0
    try:
        return max(int(value), 0)
    except (TypeError, ValueError):
        return 0
