from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd


REQUIRED_TARGET = "is_fraud"


def load_raw_datasets(raw_dir: Path, train_file: str, holdout_file: str) -> pd.DataFrame:
    train_path = raw_dir / train_file
    holdout_path = raw_dir / holdout_file

    if not train_path.exists():
        raise FileNotFoundError(f"Missing file: {train_path}")
    if not holdout_path.exists():
        raise FileNotFoundError(f"Missing file: {holdout_path}")

    train_df = pd.read_csv(train_path)
    holdout_df = pd.read_csv(holdout_path)

    if REQUIRED_TARGET not in train_df.columns or REQUIRED_TARGET not in holdout_df.columns:
        raise ValueError(f"Expected target column '{REQUIRED_TARGET}' in both files")

    train_df["source_split"] = "train"
    holdout_df["source_split"] = "holdout"

    full_df = pd.concat([train_df, holdout_df], ignore_index=True)
    full_df["transaction_row_id"] = np.arange(len(full_df))
    return full_df
