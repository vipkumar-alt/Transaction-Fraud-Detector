from __future__ import annotations

import numpy as np
import pandas as pd


def add_behavioral_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy().sort_values(["cc_num", "trans_date_trans_time"]).reset_index(drop=True)

    prev_time = df.groupby("cc_num")["trans_date_trans_time"].shift(1)
    delta_min = (df["trans_date_trans_time"] - prev_time).dt.total_seconds() / 60.0
    df["mins_since_prev_txn"] = delta_min.fillna(999999).clip(lower=0)

    indexed = df.set_index("trans_date_trans_time")
    grouped_amt = indexed.groupby("cc_num")["amt"]

    count_1h = grouped_amt.rolling("1H").count().reset_index(level=0, drop=True) - 1
    count_24h = grouped_amt.rolling("24H").count().reset_index(level=0, drop=True) - 1
    sum_24h = grouped_amt.rolling("24H").sum().reset_index(level=0, drop=True) - indexed["amt"]
    mean_7d = grouped_amt.rolling("7D").mean().reset_index(level=0, drop=True)
    std_7d = grouped_amt.rolling("7D").std().reset_index(level=0, drop=True)

    df["txn_count_1h"] = count_1h.fillna(0).clip(lower=0)
    df["txn_count_24h"] = count_24h.fillna(0).clip(lower=0)
    df["txn_amt_sum_24h"] = sum_24h.fillna(0).clip(lower=0)
    df["txn_amt_mean_7d"] = mean_7d.fillna(df["amt"].median())
    df["txn_amt_std_7d"] = std_7d.fillna(0)

    merchant_group = indexed.groupby(["cc_num", "merchant"])["amt"]
    category_group = indexed.groupby(["cc_num", "category"])["amt"]

    same_merchant_24h = merchant_group.rolling("24H").count().reset_index(level=[0, 1], drop=True) - 1
    same_category_24h = category_group.rolling("24H").count().reset_index(level=[0, 1], drop=True) - 1

    df["same_merchant_count_24h"] = same_merchant_24h.fillna(0).clip(lower=0)
    df["same_category_count_24h"] = same_category_24h.fillna(0).clip(lower=0)

    safe_mean = df["txn_amt_mean_7d"].replace(0, np.nan)
    df["amt_vs_mean7d_ratio"] = (df["amt"] / safe_mean).replace([np.inf, -np.inf], np.nan).fillna(1.0)

    safe_sum = df["txn_amt_sum_24h"].replace(0, np.nan)
    df["amt_vs_sum24h_ratio"] = (df["amt"] / safe_sum).replace([np.inf, -np.inf], np.nan).fillna(1.0)

    return df


def build_feature_lists(df: pd.DataFrame):
    excluded = {
        "is_fraud",
        "source_split",
        "transaction_row_id",
        "trans_date_trans_time",
        "cc_num",
    }

    feature_cols = [c for c in df.columns if c not in excluded]

    categorical_cols = [
        c for c in [
            "merchant", "category", "gender", "city", "state", "job", "zip",
            "txn_dayofweek", "txn_month", "amt_band"
        ] if c in feature_cols
    ]
    categorical_cols = sorted(set(categorical_cols))
    numeric_cols = [c for c in feature_cols if c not in categorical_cols]

    return feature_cols, categorical_cols, numeric_cols
