from __future__ import annotations

from dataclasses import dataclass

import pandas as pd


@dataclass
class SplitData:
    train: pd.DataFrame
    valid: pd.DataFrame
    holdout: pd.DataFrame


def split_data(df: pd.DataFrame, valid_fraction: float = 0.15) -> SplitData:
    train_part = df[df["source_split"] == "train"].copy().sort_values("trans_date_trans_time")
    holdout_part = df[df["source_split"] == "holdout"].copy().sort_values("trans_date_trans_time")

    split_idx = int(len(train_part) * (1.0 - valid_fraction))
    train_df = train_part.iloc[:split_idx].copy()
    valid_df = train_part.iloc[split_idx:].copy()

    return SplitData(train=train_df, valid=valid_df, holdout=holdout_part)
