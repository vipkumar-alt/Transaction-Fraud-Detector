
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Tuple

import joblib
import numpy as np
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = PROJECT_ROOT / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from fraud_model.config import load_config
from fraud_model.data_loading import load_raw_datasets
from fraud_model.preprocessing import basic_preprocess, apply_rare_category_mapping
from fraud_model.features import add_behavioral_features, build_feature_lists
from fraud_model.splitting import split_data
from fraud_model.evaluation import compute_metrics


KEY_COLUMNS = [
    "source_split",
    "transaction_row_id",
    "cc_num",
    "trans_date_trans_time",
    "is_fraud",
]


def prepare_datasets(config_path: str | Path):
    config = load_config(config_path)
    df = load_raw_datasets(config.raw_dir, config.train_file, config.holdout_file)
    df = basic_preprocess(df, create_amount_bands=config.create_amount_bands)
    if config.create_behavioral_features:
        df = add_behavioral_features(df)
    df = df.sort_values("trans_date_trans_time").reset_index(drop=True)

    splits = split_data(df, valid_fraction=config.validation_fraction)
    feature_cols, categorical_cols, numeric_cols = build_feature_lists(splits.train)
    all_needed = sorted(set(feature_cols + KEY_COLUMNS))

    train_df = splits.train[all_needed].copy()
    valid_df = splits.valid[all_needed].copy()
    holdout_df = splits.holdout[all_needed].copy()

    train_df, valid_df, holdout_df, rare_map = apply_rare_category_mapping(
        train_df, valid_df, holdout_df, categorical_cols, min_count=config.rare_category_min_count
    )

    X_train = train_df[feature_cols].copy()
    y_train = train_df["is_fraud"].astype(int).to_numpy()

    X_valid = valid_df[feature_cols].copy()
    y_valid = valid_df["is_fraud"].astype(int).to_numpy()

    X_holdout = holdout_df[feature_cols].copy()
    y_holdout = holdout_df["is_fraud"].astype(int).to_numpy()

    return {
        "config": config,
        "feature_cols": feature_cols,
        "categorical_cols": categorical_cols,
        "numeric_cols": numeric_cols,
        "train_df": train_df,
        "valid_df": valid_df,
        "holdout_df": holdout_df,
        "X_train": X_train,
        "y_train": y_train,
        "X_valid": X_valid,
        "y_valid": y_valid,
        "X_holdout": X_holdout,
        "y_holdout": y_holdout,
        "rare_map": rare_map,
    }


def load_best_model(config_path: str | Path):
    config = load_config(config_path)
    metadata_path = config.artifacts_dir / "training_metadata.json"
    if not metadata_path.exists():
        raise FileNotFoundError(f"Missing metadata file: {metadata_path}")

    metadata = json.loads(metadata_path.read_text())
    best_name = metadata["best_model"]

    if best_name == "catboost":
        model_path = config.artifacts_dir / "best_model_catboost.cbm"
        raise ValueError(
            "Best model is CatBoost. These scripts currently assume a joblib pipeline. "
            "Adjust the loader if CatBoost becomes best."
        )
    else:
        model_path = config.artifacts_dir / "best_model.joblib"

    if not model_path.exists():
        raise FileNotFoundError(f"Missing best model artifact: {model_path}")

    model = joblib.load(model_path)
    return config, metadata, model


def ensure_testing_dir(config):
    out_dir = config.artifacts_dir / "testing"
    out_dir.mkdir(parents=True, exist_ok=True)
    return out_dir


def predict_proba(model, X: pd.DataFrame) -> np.ndarray:
    proba = model.predict_proba(X)[:, 1]
    return np.asarray(proba)
