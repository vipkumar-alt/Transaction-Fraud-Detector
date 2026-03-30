from __future__ import annotations

from typing import Dict, List, Tuple

import pandas as pd
from catboost import CatBoostClassifier
from lightgbm import LGBMClassifier
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OrdinalEncoder, StandardScaler
from xgboost import XGBClassifier


def make_preprocessor(categorical_cols: List[str], numeric_cols: List[str], scale_numeric: bool = False) -> ColumnTransformer:
    num_steps = [("imputer", SimpleImputer(strategy="median"))]
    if scale_numeric:
        num_steps.append(("scaler", StandardScaler()))

    return ColumnTransformer(
        transformers=[
            (
                "num",
                Pipeline(steps=num_steps),
                numeric_cols,
            ),
            (
                "cat",
                Pipeline(
                    steps=[
                        ("imputer", SimpleImputer(strategy="most_frequent")),
                        ("encoder", OrdinalEncoder(handle_unknown="use_encoded_value", unknown_value=-1)),
                    ]
                ),
                categorical_cols,
            ),
        ],
        remainder="drop",
        verbose_feature_names_out=False,
    )


def make_logreg_pipeline(categorical_cols: List[str], numeric_cols: List[str], class_weight: Dict[int, float]) -> Pipeline:
    pre = make_preprocessor(categorical_cols, numeric_cols, scale_numeric=True)
    model = LogisticRegression(max_iter=1000, class_weight=class_weight, random_state=42)
    return Pipeline(steps=[("preprocess", pre), ("model", model)])


def make_rf_pipeline(categorical_cols: List[str], numeric_cols: List[str], class_weight: Dict[int, float]) -> Pipeline:
    pre = make_preprocessor(categorical_cols, numeric_cols)
    model = RandomForestClassifier(
        n_estimators=300,
        min_samples_leaf=2,
        class_weight=class_weight,
        n_jobs=-1,
        random_state=42,
    )
    return Pipeline(steps=[("preprocess", pre), ("model", model)])


def make_xgb_pipeline(categorical_cols: List[str], numeric_cols: List[str], scale_pos_weight: float) -> Pipeline:
    pre = make_preprocessor(categorical_cols, numeric_cols)
    model = XGBClassifier(
        n_estimators=500,
        max_depth=6,
        min_child_weight=5,
        learning_rate=0.05,
        subsample=0.85,
        colsample_bytree=0.85,
        reg_alpha=0.05,
        reg_lambda=2.0,
        gamma=0.0,
        objective="binary:logistic",
        eval_metric="aucpr",
        tree_method="hist",
        scale_pos_weight=scale_pos_weight,
        n_jobs=-1,
        random_state=42,
    )
    return Pipeline(steps=[("preprocess", pre), ("model", model)])


def make_lgbm_pipeline(categorical_cols: List[str], numeric_cols: List[str], scale_pos_weight: float) -> Pipeline:
    pre = make_preprocessor(categorical_cols, numeric_cols)
    model = LGBMClassifier(
        objective="binary",
        n_estimators=600,
        learning_rate=0.05,
        num_leaves=63,
        subsample=0.85,
        colsample_bytree=0.85,
        reg_lambda=1.5,
        scale_pos_weight=scale_pos_weight,
        n_jobs=-1,
        random_state=42,
    )
    return Pipeline(steps=[("preprocess", pre), ("model", model)])


def train_pipeline_model(
    pipeline: Pipeline,
    X_train: pd.DataFrame,
    y_train,
    X_valid: pd.DataFrame,
    X_holdout: pd.DataFrame,
):
    pipeline.fit(X_train, y_train)
    valid_proba = pipeline.predict_proba(X_valid)[:, 1]
    holdout_proba = pipeline.predict_proba(X_holdout)[:, 1]
    return pipeline, valid_proba, holdout_proba


def train_catboost(
    X_train: pd.DataFrame,
    y_train,
    X_valid: pd.DataFrame,
    y_valid,
    X_holdout: pd.DataFrame,
    categorical_cols: List[str],
    pos_weight: float,
) -> Tuple[CatBoostClassifier, object, object]:
    X_train_cb = X_train.copy()
    X_valid_cb = X_valid.copy()
    X_holdout_cb = X_holdout.copy()

    for frame in [X_train_cb, X_valid_cb, X_holdout_cb]:
        for col in categorical_cols:
            if col in frame.columns:
                frame[col] = frame[col].astype("string").fillna("__missing__")

    model = CatBoostClassifier(
        loss_function="Logloss",
        eval_metric="PRAUC",
        iterations=600,
        learning_rate=0.05,
        depth=8,
        l2_leaf_reg=5.0,
        random_seed=42,
        class_weights=[1.0, float(pos_weight)],
        verbose=False,
    )
    model.fit(X_train_cb, y_train, cat_features=categorical_cols, eval_set=(X_valid_cb, y_valid), verbose=False)

    valid_proba = model.predict_proba(X_valid_cb)[:, 1]
    holdout_proba = model.predict_proba(X_holdout_cb)[:, 1]
    return model, valid_proba, holdout_proba
