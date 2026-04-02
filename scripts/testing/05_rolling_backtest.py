
from __future__ import annotations

import argparse
import pandas as pd

from _common_testing import load_best_model, ensure_testing_dir
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_DIR = PROJECT_ROOT / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from fraud_model.config import load_config
from fraud_model.data_loading import load_raw_datasets
from fraud_model.preprocessing import basic_preprocess, apply_rare_category_mapping
from fraud_model.features import add_behavioral_features, build_feature_lists
from fraud_model.models import make_xgb_pipeline
from fraud_model.evaluation import compute_metrics


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    parser.add_argument("--windows", type=int, default=4)
    args = parser.parse_args()

    config = load_config(args.config)
    out_dir = ensure_testing_dir(config)

    df = load_raw_datasets(config.raw_dir, config.train_file, config.holdout_file)
    df = df[df["source_split"] == "train"].copy()
    df = basic_preprocess(df, create_amount_bands=config.create_amount_bands)
    if config.create_behavioral_features:
        df = add_behavioral_features(df)
    df = df.sort_values("trans_date_trans_time").reset_index(drop=True)

    feature_cols, categorical_cols, numeric_cols = build_feature_lists(df)

    n = len(df)
    windows = args.windows
    chunk = n // (windows + 1)

    rows = []
    for i in range(windows):
        train_end = chunk * (i + 1)
        valid_end = chunk * (i + 2)
        train_df = df.iloc[:train_end].copy()
        valid_df = df.iloc[train_end:valid_end].copy()

        if len(valid_df) == 0 or train_df["is_fraud"].sum() == 0:
            continue

        train_df, valid_df, _, _ = apply_rare_category_mapping(
            train_df,
            valid_df,
            valid_df.copy(),
            categorical_cols,
            min_count=config.rare_category_min_count,
        )

        X_train = train_df[feature_cols]
        y_train = train_df["is_fraud"].astype(int).to_numpy()

        X_valid = valid_df[feature_cols]
        y_valid = valid_df["is_fraud"].astype(int).to_numpy()

        pos = int(y_train.sum())
        neg = int((y_train == 0).sum())
        scale_pos_weight = float(neg / max(pos, 1))

        model = make_xgb_pipeline(categorical_cols, numeric_cols, scale_pos_weight)
        model.fit(X_train, y_train)
        proba = model.predict_proba(X_valid)[:, 1]

        # use the current project winner threshold
        metrics = compute_metrics(y_valid, proba, threshold=0.33)
        rows.append({
            "window": i + 1,
            "train_rows": len(train_df),
            "valid_rows": len(valid_df),
            "pr_auc": metrics["pr_auc"],
            "roc_auc": metrics["roc_auc"],
            "precision": metrics["precision"],
            "recall": metrics["recall"],
            "f1": metrics["f1"],
        })

    out = pd.DataFrame(rows)
    out.to_csv(out_dir / "rolling_backtest.csv", index=False)
    print(out.to_string(index=False))
    print(f"Saved: {out_dir / 'rolling_backtest.csv'}")


if __name__ == "__main__":
    main()
