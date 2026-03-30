from __future__ import annotations

from pathlib import Path
from typing import Dict, List

import joblib
import pandas as pd

from .config import TrainConfig
from .data_loading import load_raw_datasets
from .evaluation import choose_threshold_by_f2, compute_metrics
from .features import add_behavioral_features, build_feature_lists
from .models import (
    make_lgbm_pipeline,
    make_logreg_pipeline,
    make_rf_pipeline,
    make_xgb_pipeline,
    train_catboost,
    train_pipeline_model,
)
from .preprocessing import apply_rare_category_mapping, basic_preprocess
from .splitting import split_data
from .utils import ensure_dir, save_json


KEY_COLUMNS = [
    "source_split",
    "transaction_row_id",
    "cc_num",
    "trans_date_trans_time",
    "is_fraud",
]


def run_training(config: TrainConfig) -> None:
    ensure_dir(config.processed_dir)
    ensure_dir(config.artifacts_dir)

    print("[1/8] Loading raw datasets...")
    df = load_raw_datasets(config.raw_dir, config.train_file, config.holdout_file)

    print("[2/8] Basic preprocessing...")
    df = basic_preprocess(df, create_amount_bands=config.create_amount_bands)

    print("[3/8] Feature engineering...")
    if config.create_behavioral_features:
        df = add_behavioral_features(df)

    df = df.sort_values("trans_date_trans_time").reset_index(drop=True)

    print("[4/8] Time-aware split...")
    splits = split_data(df, valid_fraction=config.validation_fraction)

    feature_cols, categorical_cols, numeric_cols = build_feature_lists(splits.train)
    all_needed = sorted(set(feature_cols + KEY_COLUMNS))

    train_df = splits.train[all_needed].copy()
    valid_df = splits.valid[all_needed].copy()
    holdout_df = splits.holdout[all_needed].copy()

    train_df, valid_df, holdout_df, rare_map = apply_rare_category_mapping(
        train_df,
        valid_df,
        holdout_df,
        categorical_cols,
        min_count=config.rare_category_min_count,
    )

    X_train = train_df[feature_cols].copy()
    y_train = train_df["is_fraud"].astype(int).to_numpy()
    X_valid = valid_df[feature_cols].copy()
    y_valid = valid_df["is_fraud"].astype(int).to_numpy()
    X_holdout = holdout_df[feature_cols].copy()
    y_holdout = holdout_df["is_fraud"].astype(int).to_numpy()

    fraud_count = int(y_train.sum())
    non_fraud_count = int((y_train == 0).sum())
    scale_pos_weight = float(non_fraud_count / max(fraud_count, 1))
    class_weight = {0: 1.0, 1: scale_pos_weight}

    print(f"Training positives: {fraud_count}")
    print(f"Training negatives: {non_fraud_count}")
    print(f"scale_pos_weight: {scale_pos_weight:.2f}")
    print(f"Features: {len(feature_cols)}")

    models: Dict[str, Dict] = {}

    print("[5/8] Training models...")
    if config.train_logistic_regression:
        model = make_logreg_pipeline(categorical_cols, numeric_cols, class_weight)
        model, valid_proba, holdout_proba = train_pipeline_model(model, X_train, y_train, X_valid, X_holdout)
        threshold, _ = choose_threshold_by_f2(y_valid, valid_proba)
        models["logistic_regression"] = {
            "model": model,
            "valid_metrics": compute_metrics(y_valid, valid_proba, threshold),
            "holdout_metrics": compute_metrics(y_holdout, holdout_proba, threshold),
            "threshold": threshold,
        }

    if config.train_random_forest:
        model = make_rf_pipeline(categorical_cols, numeric_cols, class_weight)
        model, valid_proba, holdout_proba = train_pipeline_model(model, X_train, y_train, X_valid, X_holdout)
        threshold, _ = choose_threshold_by_f2(y_valid, valid_proba)
        models["random_forest"] = {
            "model": model,
            "valid_metrics": compute_metrics(y_valid, valid_proba, threshold),
            "holdout_metrics": compute_metrics(y_holdout, holdout_proba, threshold),
            "threshold": threshold,
        }

    if config.train_xgboost:
        model = make_xgb_pipeline(categorical_cols, numeric_cols, scale_pos_weight)
        model, valid_proba, holdout_proba = train_pipeline_model(model, X_train, y_train, X_valid, X_holdout)
        threshold, _ = choose_threshold_by_f2(y_valid, valid_proba)
        models["xgboost"] = {
            "model": model,
            "valid_metrics": compute_metrics(y_valid, valid_proba, threshold),
            "holdout_metrics": compute_metrics(y_holdout, holdout_proba, threshold),
            "threshold": threshold,
        }

    if config.train_lightgbm:
        model = make_lgbm_pipeline(categorical_cols, numeric_cols, scale_pos_weight)
        model, valid_proba, holdout_proba = train_pipeline_model(model, X_train, y_train, X_valid, X_holdout)
        threshold, _ = choose_threshold_by_f2(y_valid, valid_proba)
        models["lightgbm"] = {
            "model": model,
            "valid_metrics": compute_metrics(y_valid, valid_proba, threshold),
            "holdout_metrics": compute_metrics(y_holdout, holdout_proba, threshold),
            "threshold": threshold,
        }

    if config.train_catboost:
        model, valid_proba, holdout_proba = train_catboost(
            X_train, y_train, X_valid, y_valid, X_holdout, categorical_cols, scale_pos_weight
        )
        threshold, _ = choose_threshold_by_f2(y_valid, valid_proba)
        models["catboost"] = {
            "model": model,
            "valid_metrics": compute_metrics(y_valid, valid_proba, threshold),
            "holdout_metrics": compute_metrics(y_holdout, holdout_proba, threshold),
            "threshold": threshold,
        }

    print("[6/8] Building leaderboard...")
    leaderboard_rows: List[Dict] = []
    for name, obj in models.items():
        leaderboard_rows.append(
            {
                "model": name,
                "valid_pr_auc": obj["valid_metrics"]["pr_auc"],
                "valid_recall": obj["valid_metrics"]["recall"],
                "valid_precision": obj["valid_metrics"]["precision"],
                "holdout_pr_auc": obj["holdout_metrics"]["pr_auc"],
                "holdout_recall": obj["holdout_metrics"]["recall"],
                "holdout_precision": obj["holdout_metrics"]["precision"],
                "threshold": obj["threshold"],
            }
        )

    leaderboard = pd.DataFrame(leaderboard_rows).sort_values(
        [config.primary_metric, "holdout_recall"], ascending=[False, False]
    )
    print(leaderboard.to_string(index=False))

    best_name = leaderboard.iloc[0]["model"]
    best_obj = models[best_name]

    print("[7/8] Saving artifacts...")
    leaderboard.to_csv(config.artifacts_dir / "model_leaderboard.csv", index=False)
    save_json(
        {
            name: {
                "valid_metrics": obj["valid_metrics"],
                "holdout_metrics": obj["holdout_metrics"],
                "threshold": obj["threshold"],
            }
            for name, obj in models.items()
        },
        config.artifacts_dir / "all_model_metrics.json",
    )

    metadata = {
        "best_model": best_name,
        "feature_columns": feature_cols,
        "categorical_columns": categorical_cols,
        "numeric_columns": numeric_cols,
        "rare_category_mapping": rare_map,
        "scale_pos_weight": scale_pos_weight,
        "train_rows": len(train_df),
        "valid_rows": len(valid_df),
        "holdout_rows": len(holdout_df),
    }
    save_json(metadata, config.artifacts_dir / "training_metadata.json")

    if best_name == "catboost":
        best_obj["model"].save_model(str(config.artifacts_dir / "best_model_catboost.cbm"))
    else:
        joblib.dump(best_obj["model"], config.artifacts_dir / "best_model.joblib")

    processed_preview = pd.concat([train_df.head(200), valid_df.head(100), holdout_df.head(100)], ignore_index=True)
    processed_preview.to_csv(config.processed_dir / "processed_preview.csv", index=False)

    print("[8/8] Done.")
    print(f"Best model: {best_name}")
    print(f"Artifacts saved to: {config.artifacts_dir}")
