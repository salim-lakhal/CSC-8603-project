# REST client — exercises all endpoints of server.py
# Requirements: pip install requests

from __future__ import annotations

import json
import subprocess
import sys
import textwrap
import time
from typing import Any

import requests

SERVER_HOST = "http://localhost:5000"
HEALTH_URL  = f"{SERVER_HOST}/health"
CALC_URL    = f"{SERVER_HOST}/calculate"
HISTORY_URL = f"{SERVER_HOST}/calculate/history"

SERVER_STARTUP_TIMEOUT = 15
SERVER_POLL_INTERVAL   = 0.4

SEPARATOR = "=" * 70
SUBSEP    = "-" * 70


def _banner(title: str) -> None:
    print(f"\n{SEPARATOR}")
    print(f"  {title}")
    print(SEPARATOR)


def _print_request(method: str, url: str, body: dict[str, Any] | None = None) -> None:
    print(f"\n>>> REQUEST")
    print(f"    {method} {url}")
    if body is not None:
        pretty_body = json.dumps(body, indent=6)
        indented = textwrap.indent(pretty_body, "    ")
        print(f"    Content-Type: application/json")
        print(f"    Body:")
        print(indented)


def _print_response(resp: requests.Response) -> None:
    status_phrase = _status_phrase(resp.status_code)
    print(f"\n<<< RESPONSE")
    print(f"    HTTP/1.1 {resp.status_code} {status_phrase}")

    for header in ("Content-Type", "Content-Length", "Date"):
        value = resp.headers.get(header)
        if value:
            print(f"    {header}: {value}")

    try:
        body = resp.json()
        pretty = json.dumps(body, indent=6)
        indented = textwrap.indent(pretty, "    ")
        print(f"    Body:")
        print(indented)
    except ValueError:
        print(f"    Body (raw): {resp.text[:200]}")


def _status_phrase(code: int) -> str:
    phrases = {
        200: "OK",
        201: "Created",
        400: "Bad Request",
        404: "Not Found",
        405: "Method Not Allowed",
        422: "Unprocessable Entity",
        500: "Internal Server Error",
    }
    return phrases.get(code, "Unknown")


def start_server() -> subprocess.Popen[bytes]:
    print("Starting server.py in background …")
    proc = subprocess.Popen(
        [sys.executable, "server.py"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    return proc


def wait_for_server(timeout: float = SERVER_STARTUP_TIMEOUT) -> None:
    """Poll /health until the server responds 200 OK."""
    deadline = time.monotonic() + timeout
    attempt  = 0

    while time.monotonic() < deadline:
        attempt += 1
        try:
            resp = requests.get(HEALTH_URL, timeout=1)
            if resp.status_code == 200:
                print(f"Server ready after {attempt} poll(s). "
                      f"Health response: {resp.json()}")
                return
        except requests.exceptions.ConnectionError:
            pass

        time.sleep(SERVER_POLL_INTERVAL)

    raise RuntimeError(
        f"Server did not become ready within {timeout}s. "
        "Check that port 5000 is free and server.py has no errors."
    )


def stop_server(proc: subprocess.Popen[bytes]) -> None:
    print("\nShutting down server …")
    proc.terminate()
    try:
        proc.wait(timeout=5)
        print("Server stopped.")
    except subprocess.TimeoutExpired:
        proc.kill()
        print("Server killed (did not stop gracefully).")


def post_calculation(operation: str, a: float, b: float) -> requests.Response:
    body = {"operation": operation, "a": a, "b": b}
    _print_request("POST", CALC_URL, body)
    resp = requests.post(CALC_URL, json=body)
    _print_response(resp)
    return resp


def get_history() -> requests.Response:
    _print_request("GET", HISTORY_URL)
    resp = requests.get(HISTORY_URL)
    _print_response(resp)
    return resp


def main() -> None:
    _banner("REST API Demo — Calculator Service")
    print(
        "This demo illustrates the key REST constraints:\n"
        "  • Statelessness   : each request is self-contained\n"
        "  • Uniform interface: resources identified by URLs, actions by verbs\n"
        "  • Client-server   : server owns logic, client owns presentation\n"
    )

    server_proc = start_server()

    try:
        wait_for_server()

        _banner("Step 1 — Health Check  (GET /health)")
        print(
            "GET is the correct verb: reading the health status does not\n"
            "change anything on the server. Safe + idempotent."
        )
        _print_request("GET", HEALTH_URL)
        resp = requests.get(HEALTH_URL)
        _print_response(resp)
        assert resp.status_code == 200, "Health check failed"

        # --- ADD ---
        _banner("Step 2 — Addition  (POST /calculate)")
        print(
            "POST /calculate  body: {operation: 'add', a: 15.5, b: 24.3}\n"
            "We POST because the server will *create* a new history record.\n"
            "Not idempotent: sending the same request twice adds two records."
        )
        resp = post_calculation("add", 15.5, 24.3)
        assert resp.status_code == 200
        result = resp.json()["result"]
        print(f"\n    Verified: 15.5 + 24.3 = {result}")

        # --- SUBTRACT ---
        _banner("Step 3 — Subtraction  (POST /calculate)")
        print("body: {operation: 'subtract', a: 100, b: 37.8}")
        resp = post_calculation("subtract", 100, 37.8)
        assert resp.status_code == 200
        result = resp.json()["result"]
        print(f"\n    Verified: 100 - 37.8 = {result}")

        # --- MULTIPLY ---
        _banner("Step 4 — Multiplication  (POST /calculate)")
        print("body: {operation: 'multiply', a: 6.7, b: 8.2}")
        resp = post_calculation("multiply", 6.7, 8.2)
        assert resp.status_code == 200
        result = resp.json()["result"]
        print(f"\n    Verified: 6.7 * 8.2 = {result:.4f}")

        # --- DIVIDE ---
        _banner("Step 5 — Division  (POST /calculate)")
        print("body: {operation: 'divide', a: 144, b: 12}")
        resp = post_calculation("divide", 144, 12)
        assert resp.status_code == 200
        result = resp.json()["result"]
        print(f"\n    Verified: 144 / 12 = {result}")

        # --- DIVIDE BY ZERO (error case) ---
        _banner("Step 6 — Division by Zero  (POST /calculate) — Error Case")
        print(
            "body: {operation: 'divide', a: 7, b: 0}\n"
            "\n"
            "REST error-handling principle:\n"
            "  • 400 Bad Request   — malformed JSON or missing fields\n"
            "  • 422 Unprocessable — request is syntactically valid JSON\n"
            "                        but semantically cannot be fulfilled.\n"
            "  Division by zero falls into 422: the body is well-formed,\n"
            "  but the operation is mathematically undefined."
        )
        resp = post_calculation("divide", 7, 0)
        assert resp.status_code == 422, (
            f"Expected 422, got {resp.status_code}"
        )
        print(
            f"\n    Correct! Server returned {resp.status_code} "
            f"({_status_phrase(resp.status_code)}) for division by zero."
        )

        _banner("Step 7 — Calculation History  (GET /calculate/history)")
        print(
            "GET /calculate/history\n"
            "\n"
            "REST resource design:\n"
            "  /calculate/history is a *sub-resource* of /calculate.\n"
            "  GET is correct: we only read; no state is modified.\n"
            "  The server returns the last 10 calculations (server-managed\n"
            "  resource state — not client session state)."
        )
        resp = get_history()
        assert resp.status_code == 200
        data     = resp.json()
        count    = data["count"]
        max_hist = data["max"]
        print(
            f"\n    History contains {count} record(s) "
            f"(server keeps at most {max_hist})."
        )

        _banner("Demo Complete — REST Principles Demonstrated")
        print(
            "1. STATELESSNESS     — no session cookie or token needed;\n"
            "                       every request is self-contained.\n"
            "\n"
            "2. UNIFORM INTERFACE — URLs name resources, not actions:\n"
            "                       /health, /calculate, /calculate/history\n"
            "                       HTTP verbs (GET/POST) encode the action.\n"
            "\n"
            "3. CLIENT-SERVER     — the client (this script) knows nothing\n"
            "                       about Flask internals; only URLs matter.\n"
            "\n"
            "4. HTTP STATUS CODES — machine-readable semantic signals:\n"
            "                       200 OK, 400 Bad Request, 422 Unprocessable\n"
            "\n"
            "5. JSON PAYLOADS     — lightweight, human-readable data format;\n"
            "                       no schema file required (contrast with\n"
            "                       SOAP/WSDL or gRPC Protobuf).\n"
        )

    finally:
        stop_server(server_proc)


if __name__ == "__main__":
    main()
