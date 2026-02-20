# REST calculator API — Flask
# Requirements: pip install flask flask-cors

from __future__ import annotations

import threading
from collections import deque
from datetime import datetime, timezone
from typing import Any

from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

MAX_HISTORY = 10
_history: deque[dict[str, Any]] = deque(maxlen=MAX_HISTORY)
_history_lock = threading.Lock()


def _record(entry: dict[str, Any]) -> None:
    """Append a calculation record to the shared history (thread-safe)."""
    with _history_lock:
        _history.append(entry)


def _error(message: str, status: int) -> tuple[Any, int]:
    """Return a consistent JSON error envelope."""
    return jsonify({"error": message, "status": status}), status


@app.get("/health")
def health() -> tuple[Any, int]:
    return jsonify({
        "status": "ok",
        "service": "calculator-rest",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }), 200


@app.post("/calculate")
def calculate() -> tuple[Any, int]:
    # force=True accepts any content-type, not just application/json
    data = request.get_json(force=True, silent=True)

    if data is None:
        return _error(
            "Request body must be valid JSON "
            "(e.g. {\"operation\": \"add\", \"a\": 1, \"b\": 2})",
            400,
        )

    missing = [f for f in ("operation", "a", "b") if f not in data]
    if missing:
        return _error(
            f"Missing required field(s): {', '.join(missing)}. "
            "Expected: {\"operation\": str, \"a\": float, \"b\": float}",
            400,
        )

    for field in ("a", "b"):
        if not isinstance(data[field], (int, float)):
            return _error(
                f"Field '{field}' must be a number, got "
                f"{type(data[field]).__name__!r}.",
                400,
            )

    operation: str = str(data["operation"]).strip().lower()
    a: float = float(data["a"])
    b: float = float(data["b"])

    valid_operations = {"add", "subtract", "multiply", "divide"}
    if operation not in valid_operations:
        return _error(
            f"Unknown operation {operation!r}. "
            f"Allowed values: {sorted(valid_operations)}",
            400,
        )

    if operation == "add":
        result = a + b
    elif operation == "subtract":
        result = a - b
    elif operation == "multiply":
        result = a * b
    else:  # divide
        if b == 0:
            # 422 distinguishes "bad semantics" from 400 "bad format"
            return _error(
                "Division by zero is undefined. "
                "Field 'b' must be a non-zero number when operation is 'divide'.",
                422,
            )
        result = a / b

    timestamp = datetime.now(timezone.utc).isoformat()

    entry: dict[str, Any] = {
        "result":    result,
        "operation": operation,
        "a":         a,
        "b":         b,
        "timestamp": timestamp,
    }

    _record(entry)
    return jsonify(entry), 200


@app.get("/calculate/history")
def history() -> tuple[Any, int]:
    with _history_lock:
        snapshot = list(_history)

    return jsonify({
        "history": snapshot,
        "count":   len(snapshot),
        "max":     MAX_HISTORY,
    }), 200


@app.errorhandler(404)
def not_found(exc: Any) -> tuple[Any, int]:
    return _error(f"Resource not found: {request.path}", 404)


@app.errorhandler(405)
def method_not_allowed(exc: Any) -> tuple[Any, int]:
    return _error(
        f"HTTP method {request.method} is not allowed on {request.path}. "
        "Check the API documentation for the correct verb.",
        405,
    )


if __name__ == "__main__":
    print("Starting Calculator REST API on http://localhost:5000")
    print()
    print("Available endpoints:")
    print("  GET  /health              — service health check")
    print("  POST /calculate           — perform a calculation")
    print("  GET  /calculate/history   — last 10 calculations")
    print()
    app.run(port=5000, debug=True, use_reloader=False)
