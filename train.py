from __future__ import annotations

import argparse
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
SRC_DIR = PROJECT_ROOT / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from fraud_model.config import load_config
from fraud_model.pipeline import run_training


def main() -> None:
    parser = argparse.ArgumentParser(description="Train fraud detection models on the kartik Kaggle dataset.")
    parser.add_argument("--config", required=True, help="Path to YAML training config")
    args = parser.parse_args()

    config = load_config(args.config)
    run_training(config)


if __name__ == "__main__":
    main()
