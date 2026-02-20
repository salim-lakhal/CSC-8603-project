# gRPC calculator server

from __future__ import annotations

import logging
import subprocess
import sys
import time
from concurrent import futures
from pathlib import Path

HERE = Path(__file__).parent.resolve()


def _ensure_stubs() -> None:
    """Run protoc to generate Python stubs when they are missing."""
    pb2_file = HERE / "calculator_pb2.py"
    pb2_grpc_file = HERE / "calculator_pb2_grpc.py"

    if pb2_file.exists() and pb2_grpc_file.exists():
        return

    logging.info("Protobuf stubs not found. Generating from calculator.proto …")
    cmd = [
        sys.executable, "-m", "grpc_tools.protoc",
        f"-I{HERE}",
        f"--python_out={HERE}",
        f"--grpc_python_out={HERE}",
        str(HERE / "calculator.proto"),
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(
            f"protoc failed:\nSTDOUT: {result.stdout}\nSTDERR: {result.stderr}"
        )
    logging.info("Stubs generated successfully.")


_ensure_stubs()

# Imports after stub generation so Python can find the generated modules
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

import grpc  # noqa: E402
from grpc_reflection.v1alpha import reflection  # noqa: E402

import calculator_pb2 as pb2  # noqa: E402
import calculator_pb2_grpc as pb2_grpc  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [SERVER] %(levelname)-8s %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)

_OP_NAMES: dict[int, str] = {
    pb2.ADD:      "ADD",
    pb2.SUBTRACT: "SUBTRACT",
    pb2.MULTIPLY: "MULTIPLY",
    pb2.DIVIDE:   "DIVIDE",
}


def _now_ms() -> int:
    return int(time.time() * 1000)


def _op_name(op: int) -> str:
    return _OP_NAMES.get(op, "UNKNOWN")


class CalculatorServiceServicer(pb2_grpc.CalculatorServiceServicer):
    """Implements CalculatorService from calculator.proto."""

    def Calculate(
        self,
        request: pb2.CalculateRequest,
        context: grpc.ServicerContext,
    ) -> pb2.CalculateResponse:
        op_str = _op_name(request.operation)
        logger.info(
            "Unary Calculate called: %s(%s, %s) [req_id=%s]",
            op_str, request.a, request.b, request.request_id,
        )

        if request.operation == pb2.DIVIDE and request.b == 0:
            logger.warning("Division by zero requested — aborting RPC.")
            context.abort(
                grpc.StatusCode.INVALID_ARGUMENT,
                "Division by zero is undefined. Provide a non-zero divisor.",
            )
            # never reached, but satisfies type checkers
            return pb2.CalculateResponse()

        result = _compute(request.a, request.b, request.operation)
        response = pb2.CalculateResponse(
            result=result,
            operation_name=op_str,
            request_id=request.request_id,
            success=True,
            timestamp_ms=_now_ms(),
        )
        logger.info("Unary result: %.4f", result)
        return response

    def CalculateStream(
        self,
        request: pb2.CalculateRequest,
        context: grpc.ServicerContext,
    ):
        """Server-side streaming: yields intermediate steps then the final result."""
        op_str = _op_name(request.operation)
        logger.info(
            "Streaming Calculate called: %s(%s, %s) [req_id=%s]",
            op_str, request.a, request.b, request.request_id,
        )

        yield pb2.CalculateResponse(
            result=0,
            operation_name=op_str,
            request_id=request.request_id,
            success=True,
            error_message="Step 1/3: Request received — validating inputs …",
            timestamp_ms=_now_ms(),
        )
        time.sleep(0.1)

        if request.operation == pb2.DIVIDE and request.b == 0:
            yield pb2.CalculateResponse(
                result=0,
                operation_name=op_str,
                request_id=request.request_id,
                success=False,
                error_message="Step 2/3: Validation failed — division by zero.",
                timestamp_ms=_now_ms(),
            )
            return

        yield pb2.CalculateResponse(
            result=0,
            operation_name=op_str,
            request_id=request.request_id,
            success=True,
            error_message="Step 2/3: Inputs valid — computing result …",
            timestamp_ms=_now_ms(),
        )
        time.sleep(0.1)

        result = _compute(request.a, request.b, request.operation)
        yield pb2.CalculateResponse(
            result=result,
            operation_name=op_str,
            request_id=request.request_id,
            success=True,
            error_message="Step 3/3: Computation complete.",
            timestamp_ms=_now_ms(),
        )
        logger.info("Streaming complete. Final result: %.4f", result)

    def CalculateBatch(
        self,
        request_iterator,
        context: grpc.ServicerContext,
    ):
        """Bidirectional streaming: process each incoming request and yield a response."""
        logger.info("Bidirectional streaming session opened.")
        request_count = 0

        for request in request_iterator:
            request_count += 1
            op_str = _op_name(request.operation)
            logger.info(
                "Batch item #%d: %s(%s, %s) [req_id=%s]",
                request_count, op_str, request.a, request.b, request.request_id,
            )

            if request.operation == pb2.DIVIDE and request.b == 0:
                yield pb2.CalculateResponse(
                    result=0,
                    operation_name=op_str,
                    request_id=request.request_id,
                    success=False,
                    error_message="Division by zero in batch item.",
                    timestamp_ms=_now_ms(),
                )
                continue

            result = _compute(request.a, request.b, request.operation)
            yield pb2.CalculateResponse(
                result=result,
                operation_name=op_str,
                request_id=request.request_id,
                success=True,
                timestamp_ms=_now_ms(),
            )

        logger.info("Bidirectional session closed after %d request(s).", request_count)


def _compute(a: float, b: float, op: int) -> float:
    """Perform arithmetic. Caller must validate inputs before calling."""
    if op == pb2.ADD:
        return a + b
    if op == pb2.SUBTRACT:
        return a - b
    if op == pb2.MULTIPLY:
        return a * b
    if op == pb2.DIVIDE:
        return a / b
    raise ValueError(f"Unknown operation code: {op}")


def serve(port: int = 50051, max_workers: int = 10) -> None:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=max_workers))

    pb2_grpc.add_CalculatorServiceServicer_to_server(
        CalculatorServiceServicer(), server
    )

    # Enable gRPC reflection so grpcurl and similar tools can introspect the service
    service_names = (
        pb2.DESCRIPTOR.services_by_name["CalculatorService"].full_name,
        reflection.SERVICE_NAME,
    )
    reflection.enable_server_reflection(service_names, server)

    listen_addr = f"[::]:{port}"
    server.add_insecure_port(listen_addr)
    server.start()

    logger.info("=" * 60)
    logger.info("gRPC CalculatorService listening on port %d", port)
    logger.info("Transport: HTTP/2  |  Encoding: Protocol Buffers (binary)")
    logger.info("Reflection: enabled (grpcurl-compatible)")
    logger.info("Press Ctrl+C to stop.")
    logger.info("=" * 60)

    try:
        server.wait_for_termination()
    except KeyboardInterrupt:
        logger.info("Interrupt received — initiating graceful shutdown …")
        server.stop(grace=5)
        logger.info("Server stopped.")


if __name__ == "__main__":
    serve()
