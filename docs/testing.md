# Fraud Model Testing Suite

This testing suite is designed to run after the training pipeline has completed successfully and saved artifacts.

## What it gives you

1. Threshold sweep for the best model
2. Confusion matrix and detailed classification report
3. Feature importance export for the best pipeline model
4. Error analysis exports for false positives and false negatives
5. Rolling time backtest for stronger generalization testing

## Assumptions

The project already contains:

- `configs/train_config.yaml`
- `artifacts/best_model.joblib`
- `artifacts/training_metadata.json`
- source package under `src/fraud_model/`

And the dataset files already exist at:

- `data/raw/fraudTrain.csv`
- `data/raw/fraudTest.csv`

## Run Order

```bash
python scripts/testing/01_threshold_sweep.py --config configs/train_config.yaml
python scripts/testing/02_confusion_and_report.py --config configs/train_config.yaml
python scripts/testing/03_feature_importance.py --config configs/train_config.yaml
python scripts/testing/04_error_analysis.py --config configs/train_config.yaml
python scripts/testing/05_rolling_backtest.py --config configs/train_config.yaml
```

## Main Outputs

These scripts create:

- `artifacts/testing/threshold_sweep.csv`
- `artifacts/testing/confusion_matrices.json`
- `artifacts/testing/classification_reports.json`
- `artifacts/testing/feature_importance.csv`
- `artifacts/testing/false_positives.csv`
- `artifacts/testing/false_negatives.csv`
- `artifacts/testing/rolling_backtest.csv`
