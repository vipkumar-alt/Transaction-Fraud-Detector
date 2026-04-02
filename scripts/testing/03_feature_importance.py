
from __future__ import annotations

import argparse
import pandas as pd

from _common_testing import load_best_model, prepare_datasets, ensure_testing_dir


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", required=True)
    args = parser.parse_args()

    config, metadata, model = load_best_model(args.config)
    prepared = prepare_datasets(args.config)
    out_dir = ensure_testing_dir(config)

    pipeline = model
    pre = pipeline.named_steps["preprocess"]
    booster = pipeline.named_steps["model"]

    try:
        feature_names = list(pre.get_feature_names_out())
    except Exception:
        feature_names = metadata["feature_columns"]

    importances = booster.feature_importances_
    df = pd.DataFrame({
        "feature": feature_names,
        "importance": importances,
    }).sort_values("importance", ascending=False)

    df.to_csv(out_dir / "feature_importance.csv", index=False)
    print(df.head(30).to_string(index=False))
    print(f"Saved: {out_dir / 'feature_importance.csv'}")


if __name__ == "__main__":
    main()
