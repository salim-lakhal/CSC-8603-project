# SOAP/WSDL example — mock server + zeep client

from __future__ import annotations

import sys
import time
import textwrap
import threading
import http.server
import urllib.parse
from pathlib import Path
from xml.etree import ElementTree as ET

try:
    import zeep
    from zeep import Client
    from zeep.transports import Transport
    import requests
except ImportError:
    print(
        "\n[ERROR] Required packages are missing.\n"
        "Install them with one of:\n"
        "  pip install zeep requests lxml\n"
        "  uv add zeep requests lxml\n"
    )
    sys.exit(1)

try:
    from lxml import etree as lxml_etree
    LXML_AVAILABLE = True
except ImportError:
    LXML_AVAILABLE = False


SCRIPT_DIR = Path(__file__).resolve().parent

SERVICE_HOST = "127.0.0.1"
SERVICE_PORT = 8080
BASE_URL = f"http://{SERVICE_HOST}:{SERVICE_PORT}"
WSDL_URL = f"{BASE_URL}/calculator?wsdl"
ENDPOINT_URL = f"{BASE_URL}/calculator"

NS_SOAP_ENV = "http://schemas.xmlsoap.org/soap/envelope/"
NS_CALCULATOR = "http://calculator.example.com/"

OPERATIONS = {"ADD", "SUBTRACT", "MULTIPLY", "DIVIDE"}


def pretty_xml(xml_bytes: bytes) -> str:
    """Return indented XML string. Uses lxml if available, falls back to stdlib."""
    if LXML_AVAILABLE:
        try:
            root = lxml_etree.fromstring(xml_bytes)
            return lxml_etree.tostring(
                root,
                pretty_print=True,
                encoding="unicode",
            )
        except lxml_etree.XMLSyntaxError:
            pass

    try:
        ET.register_namespace("soapenv", NS_SOAP_ENV)
        ET.register_namespace("cal", NS_CALCULATOR)
        root = ET.fromstring(xml_bytes)
        ET.indent(root, space="  ")
        return ET.tostring(root, encoding="unicode")
    except ET.ParseError:
        return xml_bytes.decode("utf-8", errors="replace")


def separator(title: str = "", char: str = "=", width: int = 72) -> None:
    """Print a visual separator line, optionally with a centred title."""
    if title:
        padded = f"  {title}  "
        side = (width - len(padded)) // 2
        print(char * side + padded + char * side)
    else:
        print(char * width)


def print_soap_envelope(label: str, xml_bytes: bytes) -> None:
    """Pretty-print a SOAP envelope with a labelled header."""
    separator(label)
    print(pretty_xml(xml_bytes))
    separator()
    print()


def _build_soap_response(result: float, operation: str, error: str = "") -> bytes:
    """Build a SOAP 1.1 response envelope."""
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope
    xmlns:soapenv="{NS_SOAP_ENV}"
    xmlns:cal="{NS_CALCULATOR}">
  <soapenv:Header/>
  <soapenv:Body>
    <cal:CalculateResponse>
      <cal:result>{result}</cal:result>
      <cal:operation>{operation}</cal:operation>
      <cal:error>{error}</cal:error>
    </cal:CalculateResponse>
  </soapenv:Body>
</soapenv:Envelope>""".encode("utf-8")


def _build_soap_fault(code: str, message: str, detail: str = "") -> bytes:
    """Build a SOAP 1.1 Fault envelope."""
    detail_block = (
        f"<detail><cal:errorDetail xmlns:cal='{NS_CALCULATOR}'>"
        f"{detail}</cal:errorDetail></detail>"
        if detail else ""
    )
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope
    xmlns:soapenv="{NS_SOAP_ENV}"
    xmlns:cal="{NS_CALCULATOR}">
  <soapenv:Header/>
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>{code}</faultcode>
      <faultstring xml:lang="en">{message}</faultstring>
      <faultactor>{ENDPOINT_URL}</faultactor>
      {detail_block}
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>""".encode("utf-8")


def _process_soap_request(body: bytes) -> tuple[bytes, int]:
    """Parse SOAP request body and return (response_bytes, http_status)."""
    try:
        root = ET.fromstring(body)

        body_el = root.find(f"{{{NS_SOAP_ENV}}}Body")
        if body_el is None:
            return _build_soap_fault(
                "soapenv:Client",
                "Malformed SOAP envelope: missing Body element",
            ), 500

        calc_req = body_el.find(f"{{{NS_CALCULATOR}}}CalculateRequest")
        if calc_req is None:
            return _build_soap_fault(
                "soapenv:Client",
                "Unknown operation: expected CalculateRequest in body",
            ), 500

        # Child elements are unqualified (elementFormDefault="unqualified")
        operation_el = calc_req.find("operation")
        a_el = calc_req.find("a")
        b_el = calc_req.find("b")

        if any(el is None for el in (operation_el, a_el, b_el)):
            return _build_soap_fault(
                "soapenv:Client",
                "Missing required fields: operation, a, and b are all required",
            ), 500

        operation = operation_el.text.strip().upper()
        a = float(a_el.text.strip())
        b = float(b_el.text.strip())

        if operation not in OPERATIONS:
            return _build_soap_fault(
                "soapenv:Client",
                f"Unknown operation '{operation}'. "
                f"Allowed: {', '.join(sorted(OPERATIONS))}",
            ), 500

        result = 0.0
        error_msg = ""

        if operation == "ADD":
            result = a + b
        elif operation == "SUBTRACT":
            result = a - b
        elif operation == "MULTIPLY":
            result = a * b
        elif operation == "DIVIDE":
            if b == 0.0:
                return _build_soap_fault(
                    "soapenv:Server",
                    "Arithmetic Error: Division by zero is undefined",
                    f"Cannot divide {a} by {b}. "
                    "The divisor (b) must be a non-zero value.",
                ), 500
            result = a / b

        return _build_soap_response(result, operation, error_msg), 200

    except ET.ParseError as exc:
        return _build_soap_fault(
            "soapenv:Client",
            f"XML parse error: {exc}",
        ), 500
    except ValueError as exc:
        return _build_soap_fault(
            "soapenv:Client",
            f"Type conversion error: {exc}. "
            "Fields 'a' and 'b' must be valid floating-point numbers.",
        ), 500
    except Exception as exc:  # noqa: BLE001
        return _build_soap_fault(
            "soapenv:Server",
            f"Internal server error: {exc}",
        ), 500


class SOAPRequestHandler(http.server.BaseHTTPRequestHandler):
    """HTTP handler for the mock CalculatorService (WSDL + SOAP endpoint)."""

    def log_message(self, format: str, *args: object) -> None:
        pass  # suppress access log

    def do_GET(self) -> None:  # noqa: N802
        """Serve the WSDL file on GET /calculator?wsdl"""
        parsed = urllib.parse.urlparse(self.path)
        params = urllib.parse.parse_qs(parsed.query)

        if (parsed.path == "/calculator" and "wsdl" in params) or parsed.query == "wsdl":
            wsdl_path = SCRIPT_DIR / "calculator.wsdl"
            if not wsdl_path.exists():
                self.send_error(404, "calculator.wsdl not found")
                return

            wsdl_bytes = wsdl_path.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/xml; charset=utf-8")
            self.send_header("Content-Length", str(len(wsdl_bytes)))
            self.end_headers()
            self.wfile.write(wsdl_bytes)
        else:
            self.send_error(404, "Not Found")

    def do_POST(self) -> None:  # noqa: N802
        """Process SOAP request on POST /calculator"""
        if not self.path.startswith("/calculator"):
            self.send_error(404, "Not Found")
            return

        content_length = int(self.headers.get("Content-Length", 0))
        request_body = self.rfile.read(content_length)

        print_soap_envelope("INCOMING SOAP REQUEST (Server received)", request_body)

        response_body, http_status = _process_soap_request(request_body)

        print_soap_envelope("OUTGOING SOAP RESPONSE (Server sending)", response_body)

        self.send_response(http_status)
        self.send_header("Content-Type", "text/xml; charset=utf-8")
        self.send_header("Content-Length", str(len(response_body)))
        self.end_headers()
        self.wfile.write(response_body)


def start_soap_server() -> http.server.HTTPServer:
    """Start the mock SOAP server in a background daemon thread."""
    server = http.server.HTTPServer(
        (SERVICE_HOST, SERVICE_PORT),
        SOAPRequestHandler,
    )
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    return server


def create_zeep_client() -> Client:
    """Create and return a configured zeep SOAP client."""
    session = requests.Session()
    transport = Transport(
        session=session,
        operation_timeout=30,
        timeout=10,
    )
    client = Client(wsdl=WSDL_URL, transport=transport)
    return client


def demonstrate_successful_call(client: Client) -> None:
    """Demo: ADD 15.5 + 24.3"""
    separator("DEMO 1: Successful SOAP Call — ADD 15.5 + 24.3", char="-")
    print()
    print("Calling: CalculatorService.calculate(operation='ADD', a=15.5, b=24.3)")
    print()

    response = client.service.calculate(
        operation="ADD",
        a=15.5,
        b=24.3,
    )

    print("--- zeep parsed response object ---")
    print(f"  result    = {response.result}")
    print(f"  operation = {response.operation}")
    print(f"  error     = '{response.error}'")
    print()

    expected = 15.5 + 24.3
    if abs(response.result - expected) < 1e-9:
        print(f"  [OK] Result {response.result} matches expected {expected:.1f}")
    else:
        print(f"  [MISMATCH] Got {response.result}, expected {expected}")
    print()


def demonstrate_division_call(client: Client) -> None:
    """Demo: DIVIDE 100.0 / 4.0"""
    separator("DEMO 2: Successful SOAP Call — DIVIDE 100.0 / 4.0", char="-")
    print()
    print("Calling: CalculatorService.calculate(operation='DIVIDE', a=100.0, b=4.0)")
    print()

    response = client.service.calculate(
        operation="DIVIDE",
        a=100.0,
        b=4.0,
    )

    print("--- zeep parsed response object ---")
    print(f"  result    = {response.result}")
    print(f"  operation = {response.operation}")
    print(f"  error     = '{response.error}'")
    print()

    expected = 25.0
    if abs(response.result - expected) < 1e-9:
        print(f"  [OK] Result {response.result} matches expected {expected}")
    else:
        print(f"  [MISMATCH] Got {response.result}, expected {expected}")
    print()


def demonstrate_fault_call(client: Client) -> None:
    """Demo: SOAP Fault on divide-by-zero. zeep raises zeep.exceptions.Fault."""
    separator("DEMO 3: SOAP Fault — DIVIDE by Zero", char="-")
    print()
    print("Calling: CalculatorService.calculate(operation='DIVIDE', a=42.0, b=0.0)")
    print("Expecting a zeep.exceptions.Fault to be raised...")
    print()

    try:
        _response = client.service.calculate(
            operation="DIVIDE",
            a=42.0,
            b=0.0,
        )
        print("  [UNEXPECTED] No exception raised — server should have faulted!")

    except zeep.exceptions.Fault as fault:
        print("  [EXPECTED] zeep.exceptions.Fault raised:")
        print(f"    fault.code    = {fault.code!r}")
        print(f"    fault.message = {fault.message!r}")
        if fault.detail is not None:
            detail_str = (
                lxml_etree.tostring(fault.detail, encoding="unicode")
                if LXML_AVAILABLE
                else str(fault.detail)
            )
            print(f"    fault.detail  = {detail_str}")
        print()
        print("  This demonstrates SOAP's structured error propagation.")
        print("  The full SOAP Fault XML was shown above in the server output.")
    print()


def demonstrate_raw_soap(client: Client) -> None:
    """Demo: inspect the SOAP envelope zeep would send without actually sending it."""
    separator("DEMO 4: Inspect Raw SOAP Envelope (zeep create_message)", char="-")
    print()
    print("Using client.create_message() to inspect the SOAP envelope zeep")
    print("would send for: calculate(operation='MULTIPLY', a=7.0, b=6.0)")
    print()

    # create_message builds the XML without making a network call
    envelope_node = client.create_message(
        client.service,
        "calculate",
        operation="MULTIPLY",
        a=7.0,
        b=6.0,
    )

    if LXML_AVAILABLE:
        raw_bytes = lxml_etree.tostring(envelope_node, pretty_print=True)
    else:
        raw_bytes = ET.tostring(envelope_node)

    print_soap_envelope("RAW SOAP REQUEST ENVELOPE (zeep generated)", raw_bytes)

    print("Key observations:")
    print("  - The Envelope/Body structure is identical to soap_request.xml")
    print("  - zeep automatically sets the correct namespace prefixes")
    print("  - The SOAPAction HTTP header would be sent separately")
    print("  - Without zeep, you would construct this XML manually")
    print()


def print_wsdl_introspection(client: Client) -> None:
    """Print zeep's introspection of the parsed WSDL."""
    separator("WSDL Introspection (what zeep learned from the WSDL)", char="-")
    print()
    print("zeep parsed the following service contract from calculator.wsdl:")
    print()

    for service_name, service in client.wsdl.services.items():
        print(f"  Service: {service_name}")
        for port_name, port in service.ports.items():
            print(f"    Port: {port_name}")
            print(f"    Binding: {port.binding.name.localname}")
            for op_name, operation in port.binding._operations.items():
                print(f"    Operation: {op_name}")
                input_body = operation.input.body
                if input_body and hasattr(input_body, 'type'):
                    print(f"      Input type: {input_body.type}")
    print()


def main() -> None:
    separator("SOAP/WSDL Calculator Service Demo")
    print()
    print("This demo starts a local SOAP server and uses zeep to interact with it.")
    print(f"  WSDL endpoint : {WSDL_URL}")
    print(f"  SOAP endpoint : {ENDPOINT_URL}")
    print()

    print("[1/3] Starting mock CalculatorService SOAP server...")
    server = start_soap_server()

    time.sleep(0.2)
    print(f"      Server listening on {SERVICE_HOST}:{SERVICE_PORT}")
    print()

    print("[2/3] Creating zeep SOAP client (fetching WSDL)...")
    try:
        client = create_zeep_client()
    except Exception as exc:
        print(f"      [ERROR] Failed to create zeep client: {exc}")
        server.shutdown()
        sys.exit(1)
    print("      WSDL parsed successfully. Client ready.")
    print()

    print("[3/3] Running SOAP communication demonstrations...")
    separator()
    print()

    print_wsdl_introspection(client)
    demonstrate_successful_call(client)
    demonstrate_division_call(client)
    demonstrate_fault_call(client)
    demonstrate_raw_soap(client)

    separator("SUMMARY: SOAP/WSDL Key Characteristics")
    print()
    summary = """
    1. CONTRACT-FIRST DESIGN
       The WSDL defines the full service contract before any code is
       written. Both client and server must conform to this contract.
       Compare: REST has no mandatory schema (OpenAPI is optional);
                GraphQL has a schema but it is introspected at runtime;
                gRPC requires a .proto file (similar contract-first approach).

    2. XML EVERYWHERE
       All messages are XML (the SOAP envelope, the Body, the Fault).
       XML is verbose but self-describing and supports namespaces,
       schemas, and digital signatures natively.
       Compare: REST typically uses JSON (compact, human-readable);
                gRPC uses Protobuf (compact binary, not human-readable).

    3. PROTOCOL INDEPENDENCE
       SOAP can run over HTTP, SMTP, JMS, or any transport. The WSDL
       binding declares the transport; the application logic is the same.
       In practice, HTTP is almost always used today.

    4. BUILT-IN EXTENSIBILITY (WS-* STANDARDS)
       The SOAP Header enables WS-Security, WS-Addressing,
       WS-ReliableMessaging, WS-Transaction — standardised extensions
       for enterprise needs. No equivalent ecosystem exists for REST.

    5. STRONG TYPING
       XSD types (xsd:double, xsd:string, etc.) are enforced at the
       protocol level. The WSDL validation catches type mismatches
       before the business logic even runs.

    6. FAULT HANDLING
       SOAP Faults provide a structured, protocol-level error mechanism
       with fault codes, messages, and detail blocks.

    7. VERBOSITY
       A SOAP message for "add(15.5, 24.3)" is ~800 bytes of XML.
       The equivalent REST JSON body is ~40 bytes.
       This overhead matters for high-throughput, low-latency systems.

    8. TOOLING MATURITY
       SOAP tooling is mature (20+ years) but declining. Modern projects
       prefer REST/GraphQL/gRPC. SOAP remains dominant in:
         - Banking and financial services (ISO 20022, SWIFT)
         - Healthcare (HL7, insurance portals)
         - Legacy enterprise systems (SAP, Oracle EBS integrations)
         - Government services (tax portals, e-procurement systems)
    """
    print(textwrap.dedent(summary))

    separator()
    print()

    server.shutdown()
    print("Server shut down. Demo complete.")
    print()


if __name__ == "__main__":
    main()
