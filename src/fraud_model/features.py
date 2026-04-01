from __future__ import annotations

import numpy as np
import pandas as pd


def _flatten_result(values) -> np.ndarray:
    return np.asarray(values).reshape(-1)


def _rolling_stat_by_card(df: pd.DataFrame, window: str, stat: str) -> np.ndarray:
    subset = df[["cc_num", "trans_date_trans_time", "amt"]]
    res = (
        subset.groupby("cc_num", group_keys=False)[["trans_date_trans_time", "amt"]]
        .apply(lambda g: getattr(g.rolling(window, on="trans_date_trans_time")["amt"], stat)())
        .to_numpy()
    )
    return _flatten_result(res)


def _rolling_count_by_card_merchant(df: pd.DataFrame, window: str, key: str) -> np.ndarray:
    subset = df[["cc_num", key, "trans_date_trans_time", "amt"]]
    res = (
        subset.groupby(["cc_num", key], group_keys=False)[["trans_date_trans_time", "amt"]]
        .apply(lambda g: g.rolling(window, on="trans_date_trans_time")["amt"].count())
        .to_numpy()
    )
    return _flatten_result(res)


def add_behavioral_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy().sort_values(["cc_num", "trans_date_trans_time"]).reset_index(drop=True)

    prev_time = df.groupby("cc_num")["trans_date_trans_time"].shift(1)
    delta_min = (df["trans_date_trans_time"] - prev_time).dt.total_seconds() / 60.0
    df["mins_since_prev_txn"] = delta_min.fillna(999999).clip(lower=0)

    count_1h = _rolling_stat_by_card(df, "1h", "count") - 1
    count_24h = _rolling_stat_by_card(df, "24h", "count") - 1
    sum_24h = _rolling_stat_by_card(df, "24h", "sum") - df["amt"].to_numpy()
    mean_7d = _rolling_stat_by_card(df, "7d", "mean")
    std_7d = _rolling_stat_by_card(df, "7d", "std")
    max_7d = _rolling_stat_by_card(df, "7d", "max")
    median_7d = _rolling_stat_by_card(df, "7d", "median")

    df["txn_count_1h"] = pd.Series(count_1h).fillna(0).clip(lower=0)
    df["txn_count_24h"] = pd.Series(count_24h).fillna(0).clip(lower=0)
    df["txn_amt_sum_24h"] = pd.Series(sum_24h).fillna(0).clip(lower=0)
    df["txn_amt_mean_7d"] = pd.Series(mean_7d).fillna(df["amt"].median())
    df["txn_amt_std_7d"] = pd.Series(std_7d).fillna(0)
    df["txn_amt_max_7d"] = pd.Series(max_7d).fillna(df["amt"])
    df["txn_amt_median_7d"] = pd.Series(median_7d).fillna(df["amt"].median())

    same_merchant_24h = _rolling_count_by_card_merchant(df, "24h", "merchant") - 1
    same_category_24h = _rolling_count_by_card_merchant(df, "24h", "category") - 1

    df["same_merchant_count_24h"] = pd.Series(same_merchant_24h).fillna(0).clip(lower=0)
    df["same_category_count_24h"] = pd.Series(same_category_24h).fillna(0).clip(lower=0)
    df["new_merchant_flag"] = (df["same_merchant_count_24h"] == 0).astype(int)
    df["new_category_flag"] = (df["same_category_count_24h"] == 0).astype(int)

    safe_mean = df["txn_amt_mean_7d"].replace(0, np.nan)
    safe_sum = df["txn_amt_sum_24h"].replace(0, np.nan)
    safe_std = df["txn_amt_std_7d"].replace(0, np.nan)
    safe_max = df["txn_amt_max_7d"].replace(0, np.nan)
    safe_median = df["txn_amt_median_7d"].replace(0, np.nan)

    df["amt_vs_mean7d_ratio"] = (df["amt"] / safe_mean).replace([np.inf, -np.inf], np.nan).fillna(1.0)
    df["amt_vs_sum24h_ratio"] = (df["amt"] / safe_sum).replace([np.inf, -np.inf], np.nan).fillna(1.0)
    df["amt_vs_max7d_ratio"] = (df["amt"] / safe_max).replace([np.inf, -np.inf], np.nan).fillna(1.0)
    df["amt_vs_median7d_ratio"] = (df["amt"] / safe_median).replace([np.inf, -np.inf], np.nan).fillna(1.0)
    df["amt_zscore_7d"] = ((df["amt"] - df["txn_amt_mean_7d"]) / safe_std).replace([np.inf, -np.inf], np.nan).fillna(0.0)

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
