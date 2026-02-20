# gRPC client — unary, server streaming, and bidi streaming demos

from __future__ import annotations

import json
import logging
import signal
import subprocess
import sys
import time
import uuid
from pathlib import Path

HERE = Path(__file__).parent.resolve()


def _ensure_stubs() -> None:
    """Invoke protoc to generate Python bindings when the stubs are absent."""
    pb2 = HERE / "calculator_pb2.py"
    pb2_grpc = HERE / "calculator_pb2_grpc.py"

    if pb2.exists() and pb2_grpc.exists():
        return

    print("[setup] Generating protobuf stubs from calculator.proto …")
    cmd = [
        sys.executable, "-m", "grpc_tools.protoc",
        f"-I{HERE}",
        f"--python_out={HERE}",
        f"--grpc_python_out={HERE}",
        str(HERE / "calculator.proto"),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"[error] protoc failed:\n{result.stderr}", file=sys.stderr)
        sys.exit(1)
    print("[setup] Stubs generated successfully.\n")


_ensure_stubs()

if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

import grpc  # noqa: E402

import calculator_pb2 as pb2  # noqa: E402
import calculator_pb2_grpc as pb2_grpc  # noqa: E402

logging.basicConfig(
    level=logging.WARNING,
    format="%(message)s",
)

RESET  = "\033[0m"
BOLD   = "\033[1m"
CYAN   = "\033[36m"
GREEN  = "\033[32m"
YELLOW = "\033[33m"
RED    = "\033[31m"
BLUE   = "\033[34m"
GREY   = "\033[90m"


def _header(title: str) -> None:
    print(f"\n{BOLD}{CYAN}{'=' * 60}{RESET}")
    print(f"{BOLD}{CYAN}  {title}{RESET}")
    print(f"{BOLD}{CYAN}{'=' * 60}{RESET}")


def _ok(msg: str) -> None:
    print(f"  {GREEN}[OK]{RESET}  {msg}")


def _err(msg: str) -> None:
    print(f"  {RED}[ERR]{RESET} {msg}")


def _info(msg: str) -> None:
    print(f"  {BLUE}[--]{RESET}  {msg}")


def _stream(msg: str) -> None:
    print(f"  {YELLOW}[>>]{RESET}  {msg}")


def _start_server() -> subprocess.Popen:
    """Launch server.py as a child process."""
    print(f"{GREY}[setup] Starting gRPC server as background process …{RESET}")
    proc = subprocess.Popen(
        [sys.executable, str(HERE / "server.py")],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    return proc


def _wait_for_server(address: str, timeout_s: float = 10.0) -> None:
    """Block until the gRPC server is ready or the timeout expires."""
    print(f"{GREY}[setup] Waiting for server at {address} …{RESET}", end="", flush=True)
    channel = grpc.insecure_channel(address)
    future = grpc.channel_ready_future(channel)
    try:
        future.result(timeout=timeout_s)
        channel.close()
        print(f"  {GREEN}ready.{RESET}\n")
    except grpc.FutureTimeoutError:
        channel.close()
        raise RuntimeError(f"Server at {address} did not become ready within {timeout_s}s.")


def _stop_server(proc: subprocess.Popen) -> None:
    """Send SIGTERM to the server process and wait for clean exit."""
    print(f"\n{GREY}[teardown] Stopping server (PID {proc.pid}) …{RESET}")
    proc.send_signal(signal.SIGTERM)
    try:
        proc.wait(timeout=5)
        print(f"{GREY}[teardown] Server stopped cleanly.{RESET}")
    except subprocess.TimeoutExpired:
        proc.kill()
        print(f"{GREY}[teardown] Server killed (did not stop within 5 s).{RESET}")


def _req(
    a: float,
    b: float,
    op: int,
    req_id: str | None = None,
) -> pb2.CalculateRequest:
    """Convenience factory for CalculateRequest messages."""
    return pb2.CalculateRequest(
        a=a,
        b=b,
        operation=op,
        request_id=req_id or str(uuid.uuid4())[:8],
    )


def demo_unary(stub: pb2_grpc.CalculatorServiceStub) -> None:
    """Unary RPC: one request, one response."""
    _header("DEMO 1 — Unary RPC (single request / single response)")

    metadata = [
        ("x-client-version", "1.0.0"),
        ("x-demo-session",   "assignment"),
    ]

    # --- ADD ---
    req = _req(15.5, 24.3, pb2.ADD)
    _info(f"Sending  → Calculate(ADD, a={req.a}, b={req.b})  [req_id={req.request_id}]")
    _info(f"Metadata → {metadata}")

    try:
        response = stub.Calculate(req, timeout=5, metadata=metadata)
        _ok(
            f"Result   ← {response.operation_name}({req.a}, {req.b}) = {response.result}"
        )
        _ok(f"req_id={response.request_id}  success={response.success}  ts={response.timestamp_ms}")
    except grpc.RpcError as exc:
        _err(f"RPC failed: [{exc.code()}] {exc.details()}")

    print()

    # --- DIVIDE (valid) ---
    req = _req(144.0, 12.0, pb2.DIVIDE)
    _info(f"Sending  → Calculate(DIVIDE, a={req.a}, b={req.b})  [req_id={req.request_id}]")

    try:
        response = stub.Calculate(req, timeout=5)
        _ok(f"Result   ← {response.operation_name}({req.a}, {req.b}) = {response.result}")
    except grpc.RpcError as exc:
        _err(f"RPC failed: [{exc.code()}] {exc.details()}")

    print()

    # --- DIVIDE by zero — expected INVALID_ARGUMENT error ---
    req = _req(10.0, 0.0, pb2.DIVIDE)
    _info(f"Sending  → Calculate(DIVIDE, a={req.a}, b={req.b})  [expected: error]")

    try:
        response = stub.Calculate(req, timeout=5)
        _ok(f"Result: {response.result}")  # should not reach here
    except grpc.RpcError as exc:
        _err(f"gRPC Status Code : {exc.code()} (value={exc.code().value[0]})")
        _err(f"Error Detail     : {exc.details()}")
        _info("This is the correct behaviour — INVALID_ARGUMENT was expected.")


def demo_server_streaming(stub: pb2_grpc.CalculatorServiceStub) -> None:
    """Server-side streaming: one request, multiple response frames."""
    _header("DEMO 2 — Server-side Streaming RPC (one request / stream of responses)")

    req = _req(7.0, 6.0, pb2.MULTIPLY)
    _info(f"Sending  → CalculateStream(MULTIPLY, a={req.a}, b={req.b})")
    _info("Server will stream intermediate computation steps …\n")

    try:
        response_stream = stub.CalculateStream(req, timeout=10)

        frame_num = 0
        for response in response_stream:
            frame_num += 1
            if response.success and response.result != 0:
                _stream(
                    f"Frame {frame_num}: result={response.result}"
                    f"  | {response.error_message}"
                )
            else:
                _stream(
                    f"Frame {frame_num}: {response.error_message or '(intermediate step)'}"
                )

        _ok(f"Stream closed after {frame_num} frame(s).")
    except grpc.RpcError as exc:
        _err(f"Streaming RPC failed: [{exc.code()}] {exc.details()}")


def demo_bidirectional_streaming(stub: pb2_grpc.CalculatorServiceStub) -> None:
    """Bidirectional streaming: client and server both send streams."""
    _header("DEMO 3 — Bidirectional Streaming RPC (stream of requests / stream of responses)")

    batch: list[tuple[float, float, int]] = [
        (100.0,  4.0,  pb2.DIVIDE),
        (3.0,    7.0,  pb2.MULTIPLY),
        (50.0,  50.0,  pb2.SUBTRACT),
        (12.0,   0.0,  pb2.DIVIDE),   # will return an error response
        (99.0,   1.0,  pb2.ADD),
    ]

    _info(f"Sending a batch of {len(batch)} request(s) via bidirectional stream …\n")

    def _request_generator():
        for i, (a, b, op) in enumerate(batch):
            r = _req(a, b, op, req_id=f"batch-{i + 1}")
            op_name = {pb2.ADD: "ADD", pb2.SUBTRACT: "SUBTRACT",
                       pb2.MULTIPLY: "MULTIPLY", pb2.DIVIDE: "DIVIDE"}[op]
            print(f"  {GREY}  → Sending request {r.request_id}: {op_name}({a}, {b}){RESET}")
            yield r
            # small delay to avoid the network layer batching all frames at once
            time.sleep(0.05)

    try:
        response_stream = stub.CalculateBatch(_request_generator(), timeout=15)

        print()
        responses_received = 0
        for response in response_stream:
            responses_received += 1
            if response.success:
                _ok(
                    f"[{response.request_id}] "
                    f"{response.operation_name} = {response.result}"
                )
            else:
                _err(
                    f"[{response.request_id}] "
                    f"{response.operation_name} failed: {response.error_message}"
                )

        _info(f"\nBatch complete — sent {len(batch)}, received {responses_received} response(s).")
    except grpc.RpcError as exc:
        _err(f"Batch RPC failed: [{exc.code()}] {exc.details()}")


def demo_binary_efficiency() -> None:
    """Compare protobuf serialized size against equivalent compact JSON."""
    _header("DEMO 4 — Binary Efficiency: Protobuf vs. JSON")

    response = pb2.CalculateResponse(
        result=39.8,
        operation_name="ADD",
        request_id="abc12345",
        success=True,
        error_message="",
        timestamp_ms=int(time.time() * 1000),
    )

    proto_bytes: bytes = response.SerializeToString()

    json_equivalent = {
        "result":         response.result,
        "operation_name": response.operation_name,
        "request_id":     response.request_id,
        "success":        response.success,
        "error_message":  response.error_message,
        "timestamp_ms":   response.timestamp_ms,
    }
    json_compact = json.dumps(json_equivalent, separators=(",", ":"))
    json_bytes = json_compact.encode("utf-8")

    proto_size = len(proto_bytes)
    json_size  = len(json_bytes)
    ratio      = json_size / proto_size if proto_size else float("inf")

    _info(f"Protobuf binary size : {proto_size:>4} bytes  →  {proto_bytes.hex()}")
    _info(f"JSON (compact) size  : {json_size:>4} bytes  →  {json_bytes.decode()}")
    _info(f"JSON is {ratio:.1f}× larger than Protobuf for this message.")
    _info("")
    _info("Why protobuf is smaller:")
    _info("  • Field names ('operation_name', 'request_id', …) are omitted.")
    _info("    On the wire, fields are identified only by their number (1 byte).")
    _info("  • Doubles use fixed 8 bytes; JSON uses variable-length ASCII digits.")
    _info("  • Booleans are encoded as a single bit, not the string 'true'.")
    _info("  • Empty strings / zero-value fields are omitted entirely.")
    _info("")
    _info("Practical impact:")
    _info("  • Lower bandwidth cost — significant at high call volumes.")
    _info("  • Faster serialization/deserialization (no string parsing).")
    _info("  • Better cache utilisation — smaller messages fit more per cache line.")
    _info("")
    _info("Trade-offs:")
    _info("  • Binary format is not human-readable without tooling.")
    _info("  • Both sides MUST have the same .proto schema to decode messages.")
    _info("  • REST+JSON can be consumed from a browser with no extra setup.")


def demo_metadata_and_deadline(stub: pb2_grpc.CalculatorServiceStub) -> None:
    """Show per-call deadline and how to read initial/trailing metadata."""
    _header("DEMO 5 — Deadline (timeout) and gRPC Metadata")

    req = _req(99.0, 9.0, pb2.DIVIDE)
    _info(f"Sending DIVIDE(99, 9) with a 5-second deadline …")

    start = time.monotonic()
    try:
        response, call = stub.Calculate.with_call(
            req,
            timeout=5,
            metadata=[("x-request-source", "demo-client")],
        )
        elapsed = time.monotonic() - start
        _ok(f"Result: {response.result:.4f}  (round-trip: {elapsed * 1000:.1f} ms)")
        _info(f"Initial metadata  : {list(call.initial_metadata())}")
        _info(f"Trailing metadata : {list(call.trailing_metadata())}")
    except grpc.RpcError as exc:
        elapsed = time.monotonic() - start
        if exc.code() == grpc.StatusCode.DEADLINE_EXCEEDED:
            _err(f"Deadline exceeded after {elapsed:.2f}s — server was too slow.")
        else:
            _err(f"RPC failed: [{exc.code()}] {exc.details()}")


def main() -> None:
    SERVER_ADDRESS = "localhost:50051"

    print(f"\n{BOLD}gRPC Calculator Client — CS Assignment Demo{RESET}")
    print(f"{GREY}Transport: HTTP/2  |  Encoding: Protocol Buffers{RESET}\n")

    server_proc = _start_server()
    try:
        _wait_for_server(SERVER_ADDRESS, timeout_s=10.0)

        # Reuse one channel for all RPCs — opening a new channel per call
        # would defeat HTTP/2 multiplexing
        channel_options = [
            ("grpc.max_receive_message_length", 10 * 1024 * 1024),  # 10 MB
            ("grpc.keepalive_time_ms", 30_000),                      # 30 s
        ]
        with grpc.insecure_channel(SERVER_ADDRESS, options=channel_options) as channel:
            stub = pb2_grpc.CalculatorServiceStub(channel)

            demo_unary(stub)
            demo_server_streaming(stub)
            demo_bidirectional_streaming(stub)
            demo_binary_efficiency()
            demo_metadata_and_deadline(stub)

        print(f"\n{BOLD}{GREEN}All demos completed successfully.{RESET}")

    finally:
        _stop_server(server_proc)


if __name__ == "__main__":
    main()
