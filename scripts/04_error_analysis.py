
from __future__ import annotations

import argparse
import pandas as pd

from _common_testing import load_best_model, prepare_datasets, ensure_testing_dir, predict_proba


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args()

    config, metadata, model = load_best_model(args.config)
    prepared = prepare_datasets(args.config)
    out_dir = ensure_testing_dir(config)

    threshold = 0.33
    try:
        leaderboard = (config.artifacts_dir / "model_leaderboard.csv")
        if leaderboard.exists():
            lb = pd.read_csv(leaderboard)
            best_name = metadata["best_model"]
            threshold = float(lb.loc[lb["model"] == best_name, "threshold"].iloc[0])
    except Exception:
        pass

    holdout_df = prepared["holdout_df"].copy()
    X_holdout = prepared["X_holdout"]
    y_holdout = prepared["y_holdout"]

    proba = predict_proba(model, X_holdout)
    preds = (proba >= threshold).astype(int)

    holdout_df["actual"] = y_holdout
    holdout_df["predicted"] = preds
    holdout_df["score"] = proba

    false_positives = holdout_df[(holdout_df["actual"] == 0) & (holdout_df["predicted"] == 1)].sort_values("score", ascending=False)
    false_negatives = holdout_df[(holdout_df["actual"] == 1) & (holdout_df["predicted"] == 0)].sort_values("score", ascending=True)

    fp_cols = [c for c in [
        "cc_num", "amt", "merchant", "category", "state", "city",
        "txn_hour", "txn_dayofweek", "merchant_distance_km",
        "txn_count_1h", "txn_count_24h", "txn_amt_sum_24h",
        "same_merchant_count_24h", "same_category_count_24h",
        "mins_since_prev_txn", "score"
    ] if c in false_positives.columns]

    fn_cols = fp_cols

    false_positives[fp_cols].head(500).to_csv(out_dir / "false_positives.csv", index=False)
    false_negatives[fn_cols].head(500).to_csv(out_dir / "false_negatives.csv", index=False)

    print(f"False positives: {len(false_positives)}")
    print(f"False negatives: {len(false_negatives)}")
    print(f"Saved: {out_dir / 'false_positives.csv'}")
    print(f"Saved: {out_dir / 'false_negatives.csv'}")


if __name__ == "__main__":
    main()
