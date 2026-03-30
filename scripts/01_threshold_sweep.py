
from __future__ import annotations

import argparse
import json
import numpy as np
import pandas as pd

from _common_testing import load_best_model, prepare_datasets, ensure_testing_dir, predict_proba
from fraud_model.evaluation import compute_metrics


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args()

    config, metadata, model = load_best_model(args.config)
    prepared = prepare_datasets(args.config)
    out_dir = ensure_testing_dir(config)

    y_valid = prepared["y_valid"]
    y_holdout = prepared["y_holdout"]

    valid_proba = predict_proba(model, prepared["X_valid"])
    holdout_proba = predict_proba(model, prepared["X_holdout"])

    rows = []
    for thr in np.round(np.arange(0.05, 0.96, 0.05), 2):
        valid_metrics = compute_metrics(y_valid, valid_proba, float(thr))
        holdout_metrics = compute_metrics(y_holdout, holdout_proba, float(thr))
        rows.append({
            "threshold": float(thr),
            "valid_pr_auc": valid_metrics["pr_auc"],
            "valid_roc_auc": valid_metrics["roc_auc"],
            "valid_precision": valid_metrics["precision"],
            "valid_recall": valid_metrics["recall"],
            "valid_f1": valid_metrics["f1"],
            "holdout_pr_auc": holdout_metrics["pr_auc"],
            "holdout_roc_auc": holdout_metrics["roc_auc"],
            "holdout_precision": holdout_metrics["precision"],
            "holdout_recall": holdout_metrics["recall"],
            "holdout_f1": holdout_metrics["f1"],
        })

    df = pd.DataFrame(rows)
    df.to_csv(out_dir / "threshold_sweep.csv", index=False)
    print(df.to_string(index=False))
    print(f"Saved: {out_dir / 'threshold_sweep.csv'}")


if __name__ == "__main__":
    main()
