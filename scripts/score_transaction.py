from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = PROJECT_ROOT / "src"
if str(SRC_DIR) not in sys.path:
    sys.path.insert(0, str(SRC_DIR))

from fraud_model.config import load_config
from fraud_model.inference import score_transaction


def main() -> None:
    parser = argparse.ArgumentParser(description="Score a single transaction with the trained fraud model.")
    parser.add_argument("--config", required=True, help="Path to YAML training config")
    args = parser.parse_args()

    request = json.load(sys.stdin)
    config = load_config(args.config)
    result = score_transaction(request, config)
    json.dump(result, sys.stdout)


if __name__ == "__main__":
    main()
