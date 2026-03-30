from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict

import yaml


@dataclass
class TrainConfig:
    raw_dir: Path
    processed_dir: Path
    artifacts_dir: Path
    train_file: str
    holdout_file: str
    validation_fraction: float
    rare_category_min_count: int
    create_amount_bands: bool
    create_behavioral_features: bool
    random_seed: int
    train_logistic_regression: bool
    train_random_forest: bool
    train_xgboost: bool
    train_lightgbm: bool
    train_catboost: bool
    primary_metric: str
    threshold_strategy: str


def load_config(config_path: str | Path) -> TrainConfig:
    config_path = Path(config_path)
    with config_path.open("r", encoding="utf-8") as f:
        data: Dict[str, Any] = yaml.safe_load(f)

    base = config_path.parent.parent
    paths = data["paths"]
    split = data["split"]
    fe = data["feature_engineering"]
    models = data["models"]
    selection = data["selection"]

    return TrainConfig(
        raw_dir=base / paths["raw_dir"],
        processed_dir=base / paths["processed_dir"],
        artifacts_dir=base / paths["artifacts_dir"],
        train_file=paths["train_file"],
        holdout_file=paths["holdout_file"],
        validation_fraction=float(split["validation_fraction"]),
        rare_category_min_count=int(fe["rare_category_min_count"]),
        create_amount_bands=bool(fe["create_amount_bands"]),
        create_behavioral_features=bool(fe["create_behavioral_features"]),
        random_seed=int(data["random_seed"]),
        train_logistic_regression=bool(models["train_logistic_regression"]),
        train_random_forest=bool(models["train_random_forest"]),
        train_xgboost=bool(models["train_xgboost"]),
        train_lightgbm=bool(models["train_lightgbm"]),
        train_catboost=bool(models["train_catboost"]),
        primary_metric=str(selection["primary_metric"]),
        threshold_strategy=str(selection["threshold_strategy"]),
    )
