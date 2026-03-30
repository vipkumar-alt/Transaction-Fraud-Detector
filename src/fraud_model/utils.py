from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def ensure_dir(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def save_json(data: Any, path: Path) -> None:
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")
