# Fraud Model Training Project (Improved)

A clean, modular training project for building the **fraud scoring layer** of your analyst-facing fraud detection application.

This improved version focuses on:
- removing accidental CSV index leakage (`Unnamed: 0`)
- stronger behavioral features
- cleaner default model shortlist
- integrated testing scripts for thresholding, feature importance, error analysis, and rolling backtest

## Final modeling direction

- **Primary dataset:** `kartik2112/fraud-detection`
- **Primary model:** XGBoost
- **Challengers:** LightGBM, CatBoost
- **Decision policy:** use testing outputs to define allow / review / block thresholds

## Expected dataset files

Place these files in `data/raw/`:

- `fraudTrain.csv`
- `fraudTest.csv`

## Project structure

```text
fraud_model_project/
├── README.md
├── requirements.txt
├── train.py
├── configs/
│   └── train_config.yaml
├── data/
│   ├── raw/
│   └── processed/
├── artifacts/
├── notebooks/
├── scripts/
│   ├── run_training.sh
│   ├── 01_threshold_sweep.py
│   ├── 02_confusion_and_report.py
│   ├── 03_feature_importance.py
│   ├── 04_error_analysis.py
│   ├── 05_rolling_backtest.py
│   └── _common_testing.py
└── src/
    └── fraud_model/
        ├── config.py
        ├── data_loading.py
        ├── preprocessing.py
        ├── features.py
        ├── splitting.py
        ├── models.py
        ├── evaluation.py
        ├── pipeline.py
        └── utils.py
```

## Install

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip setuptools wheel
python -m pip install -r requirements.txt
```

## Train

```bash
python train.py --config configs/train_config.yaml
```

## Test the final scorer

```bash
python scripts/01_threshold_sweep.py --config configs/train_config.yaml
python scripts/02_confusion_and_report.py --config configs/train_config.yaml
python scripts/03_feature_importance.py --config configs/train_config.yaml
python scripts/04_error_analysis.py --config configs/train_config.yaml
python scripts/05_rolling_backtest.py --config configs/train_config.yaml
```

## What changed in this improved version

1. `Unnamed:*` columns are dropped automatically.
2. Logistic regression and random forest are disabled by default.
3. Behavioral features were expanded with:
   - `txn_amt_max_7d`
   - `txn_amt_median_7d`
   - `amt_vs_max7d_ratio`
   - `amt_vs_median7d_ratio`
   - `amt_zscore_7d`
   - `new_merchant_flag`
   - `new_category_flag`
4. Rolling-feature warnings were reduced by grouping only the required columns.
5. Testing scripts are part of the main project package.

## Recommended next workflow

1. Retrain the improved project.
2. Run all testing scripts.
3. Freeze the final scorer.
4. Move to backend integration.
5. Add RAG explanation later.
