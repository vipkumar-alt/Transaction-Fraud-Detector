from __future__ import annotations

import numpy as np
import pandas as pd

RAW_DROP_COLUMNS = [
    "first",
    "last",
    "street",
    "trans_num",
]
BASE_CATEGORICAL_COLUMNS = [
    "merchant",
    "category",
    "gender",
    "city",
    "state",
    "job",
    "zip",
]


def normalize_text(series: pd.Series) -> pd.Series:
    series = series.astype("string").fillna("__missing__")
    series = series.str.strip().str.lower()
    series = series.str.replace(r"\s+", "_", regex=True)
    series = series.str.replace(r"[^a-z0-9_]+", "_", regex=True)
    series = series.str.replace(r"_+", "_", regex=True)
    series = series.str.strip("_")
    return series.replace("", "__missing__")


def haversine_km(lat1, lon1, lat2, lon2):
    lat1 = np.radians(lat1.astype(float))
    lon1 = np.radians(lon1.astype(float))
    lat2 = np.radians(lat2.astype(float))
    lon2 = np.radians(lon2.astype(float))
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = np.sin(dlat / 2.0) ** 2 + np.cos(lat1) * np.cos(lat2) * np.sin(dlon / 2.0) ** 2
    return 6371.0 * (2 * np.arcsin(np.sqrt(a)))


def basic_preprocess(df: pd.DataFrame, create_amount_bands: bool = True) -> pd.DataFrame:
    df = df.copy()

    # Drop obvious CSV index artifacts and raw identifier-style columns
    unnamed_cols = [c for c in df.columns if c.lower().startswith("unnamed")]
    drop_cols = [c for c in RAW_DROP_COLUMNS if c in df.columns] + unnamed_cols
    if drop_cols:
        df = df.drop(columns=drop_cols)

    df["trans_date_trans_time"] = pd.to_datetime(df["trans_date_trans_time"], errors="coerce")
    if df["trans_date_trans_time"].isna().any():
        raise ValueError("Failed to parse one or more timestamps")

    df = df.sort_values("trans_date_trans_time").reset_index(drop=True)

    for col in BASE_CATEGORICAL_COLUMNS:
        if col in df.columns:
            df[col] = normalize_text(df[col])

    if "zip" in df.columns:
        df["zip"] = df["zip"].astype("Int64").astype("string").fillna("__missing__")

    if "dob" in df.columns:
        dob = pd.to_datetime(df["dob"], errors="coerce")
        age_years = (df["trans_date_trans_time"] - dob).dt.days / 365.25
        df["customer_age_years"] = age_years.clip(lower=18, upper=100)
        df = df.drop(columns=["dob"])

    ts = df["trans_date_trans_time"]
    df["txn_hour"] = ts.dt.hour
    df["txn_dayofweek"] = ts.dt.dayofweek.astype(str)
    df["txn_month"] = ts.dt.month.astype(str)
    df["is_weekend"] = ts.dt.dayofweek.isin([5, 6]).astype(int)
    df["is_night"] = ts.dt.hour.isin([0, 1, 2, 3, 4, 5]).astype(int)

    if "amt" in df.columns:
        df["log_amt"] = np.log1p(df["amt"].clip(lower=0))
        df["city_pop_log"] = np.log1p(df["city_pop"].clip(lower=0)) if "city_pop" in df.columns else np.nan
        if create_amount_bands:
            df["amt_band"] = pd.cut(
                df["amt"],
                bins=[-0.1, 10, 50, 100, 500, 1000, np.inf],
                labels=["very_low", "low", "medium", "high", "very_high", "extreme"],
            ).astype("string").fillna("unknown")

    if all(c in df.columns for c in ["lat", "long", "merch_lat", "merch_long"]):
        distance = haversine_km(df["lat"], df["long"], df["merch_lat"], df["merch_long"])
        df["merchant_distance_km"] = distance
        df["log_merchant_distance_km"] = np.log1p(distance.clip(lower=0))

    return df


def apply_rare_category_mapping(train_df, valid_df, holdout_df, categorical_cols, min_count=50):
    train_df = train_df.copy()
    valid_df = valid_df.copy()
    holdout_df = holdout_df.copy()
    kept_values = {}

    for col in categorical_cols:
        if col not in train_df.columns:
            continue
        vc = train_df[col].astype("string").value_counts()
        keep = set(vc[vc >= min_count].index.tolist())
        kept_values[col] = sorted(keep)

        def mapper(s: pd.Series) -> pd.Series:
            s = s.astype("string").fillna("__missing__")
            return s.where(s.isin(keep), "__other__")

        train_df[col] = mapper(train_df[col])
        valid_df[col] = mapper(valid_df[col])
        holdout_df[col] = mapper(holdout_df[col])

    return train_df, valid_df, holdout_df, kept_values
