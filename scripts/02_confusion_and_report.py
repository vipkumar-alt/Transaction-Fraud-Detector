
from __future__ import annotations

import argparse
import json
from sklearn.metrics import classification_report, confusion_matrix

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
            import pandas as pd
            lb = pd.read_csv(leaderboard)
            best_name = metadata["best_model"]
            threshold = float(lb.loc[lb["model"] == best_name, "threshold"].iloc[0])
    except Exception:
        pass

    outputs = {}

    for split_name, X, y in [
        ("valid", prepared["X_valid"], prepared["y_valid"]),
        ("holdout", prepared["X_holdout"], prepared["y_holdout"]),
    ]:
        proba = predict_proba(model, X)
        preds = (proba >= threshold).astype(int)
        outputs[split_name] = {
            "threshold": threshold,
            "confusion_matrix": confusion_matrix(y, preds).tolist(),
            "classification_report": classification_report(y, preds, zero_division=0, output_dict=True),
        }

    (out_dir / "confusion_matrices.json").write_text(json.dumps({k: v["confusion_matrix"] for k, v in outputs.items()}, indent=2))
    (out_dir / "classification_reports.json").write_text(json.dumps(outputs, indent=2))

    print(json.dumps(outputs, indent=2))
    print(f"Saved: {out_dir / 'confusion_matrices.json'}")
    print(f"Saved: {out_dir / 'classification_reports.json'}")


if __name__ == "__main__":
    main()
