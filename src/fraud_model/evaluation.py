from __future__ import annotations

from typing import Dict, Tuple

import numpy as np
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)


def choose_threshold_by_f2(y_true, proba) -> Tuple[float, float]:
    best_threshold = 0.5
    best_score = -1.0

    for thr in np.linspace(0.05, 0.95, 91):
        preds = (proba >= thr).astype(int)
        precision = precision_score(y_true, preds, zero_division=0)
        recall = recall_score(y_true, preds, zero_division=0)
        beta_sq = 4.0
        denom = (beta_sq * precision) + recall
        f2 = 0.0 if denom == 0 else (1 + beta_sq) * precision * recall / denom
        if f2 > best_score:
            best_score = float(f2)
            best_threshold = float(thr)

    return best_threshold, best_score


def compute_metrics(y_true, proba, threshold: float) -> Dict[str, object]:
    preds = (proba >= threshold).astype(int)
    return {
        "threshold": float(threshold),
        "pr_auc": float(average_precision_score(y_true, proba)),
        "roc_auc": float(roc_auc_score(y_true, proba)),
        "precision": float(precision_score(y_true, preds, zero_division=0)),
        "recall": float(recall_score(y_true, preds, zero_division=0)),
        "f1": float(f1_score(y_true, preds, zero_division=0)),
        "confusion_matrix": confusion_matrix(y_true, preds).tolist(),
        "classification_report": classification_report(y_true, preds, zero_division=0, output_dict=True),
    }
