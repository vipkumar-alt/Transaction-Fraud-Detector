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


def choose_threshold(y_true, proba, strategy: str = "f2") -> Tuple[float, float]:
    normalized = (strategy or "f2").strip().lower()
    if normalized in {"f2", "recall_heavy"}:
        return choose_threshold_by_beta(y_true, proba, beta=2.0)
    if normalized in {"f1", "balanced"}:
        return choose_threshold_by_beta(y_true, proba, beta=1.0)
    if normalized in {"f0.5", "f0_5", "precision_balanced", "precision_heavy"}:
        return choose_threshold_by_beta(y_true, proba, beta=0.5)
    raise ValueError(f"Unsupported threshold strategy: {strategy}")


def choose_threshold_by_f2(y_true, proba) -> Tuple[float, float]:
    return choose_threshold_by_beta(y_true, proba, beta=2.0)


def choose_threshold_by_beta(y_true, proba, beta: float) -> Tuple[float, float]:
    best_threshold = 0.5
    best_score = -1.0
    best_precision = -1.0
    best_recall = -1.0
    beta_sq = beta * beta

    for thr in np.linspace(0.05, 0.95, 91):
        preds = (proba >= thr).astype(int)
        precision = precision_score(y_true, preds, zero_division=0)
        recall = recall_score(y_true, preds, zero_division=0)
        denom = (beta_sq * precision) + recall
        f_beta = 0.0 if denom == 0 else (1 + beta_sq) * precision * recall / denom
        if (
            f_beta > best_score
            or (np.isclose(f_beta, best_score) and precision > best_precision)
            or (np.isclose(f_beta, best_score) and np.isclose(precision, best_precision) and recall > best_recall)
        ):
            best_score = float(f_beta)
            best_precision = float(precision)
            best_recall = float(recall)
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
