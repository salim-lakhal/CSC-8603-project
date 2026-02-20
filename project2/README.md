# Insurance Claim Processing System

An end-to-end insurance claim processing platform built as a polyglot microservices architecture. The system deliberately uses four distinct API communication technologies — **REST**, **SOAP/WSDL**, **gRPC**, and **GraphQL** — to demonstrate real-world trade-offs between each paradigm. An eleven-service pipeline, orchestrated by Bonita BPM, processes a claim from initial submission through identity verification, fraud detection, document review, expert assessment, compensation calculation, payment authorization, and final notification.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture Diagram](#2-architecture-diagram)
3. [Service Descriptions](#3-service-descriptions)
4. [Prerequisites](#4-prerequisites)
5. [Repository Layout](#5-repository-layout)
6. [Build Instructions](#6-build-instructions)
7. [Running with Docker Compose](#7-running-with-docker-compose)
8. [Testing Each API Type](#8-testing-each-api-type)
   - [REST — claim-submission (8081)](#81-rest--claim-submission-port-8081)
   - [REST — policy-validation (8083)](#82-rest--policy-validation-port-8083)
   - [SOAP — identity-verification (8082)](#83-soap--identity-verification-port-8082)
   - [gRPC — fraud-detection (9090)](#84-grpc--fraud-detection-port-9090)
   - [REST — eligibility (8084)](#85-rest--eligibility-port-8084)
   - [GraphQL — document-review (8085)](#86-graphql--document-review-port-8085)
   - [REST — expert-assessment (8086)](#87-rest--expert-assessment-port-8086)
   - [REST — compensation (8087)](#88-rest--compensation-port-8087)
   - [REST — payment-authorization (8088)](#89-rest--payment-authorization-port-8088)
   - [REST — notification (8089)](#810-rest--notification-port-8089)
   - [GraphQL — claim-tracking (8090)](#811-graphql--claim-tracking-port-8090)
9. [End-to-End Walkthrough](#9-end-to-end-walkthrough)
10. [Bonita BPM Integration](#10-bonita-bpm-integration)
11. [API Documentation](#11-api-documentation)
12. [Business Rules Reference](#12-business-rules-reference)
13. [Technology Comparison](#13-technology-comparison)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Project Overview

### What the System Does

A claimant submits an insurance claim (AUTO, HOME, HEALTH, or LIFE). The claim then flows through a fixed processing pipeline:

```
Submit -> Verify Identity -> Validate Policy -> Detect Fraud ->
Check Eligibility -> Review Documents -> Expert Assessment ->
Calculate Compensation -> Authorize Payment -> Send Notification -> Track Status
```

Each step is implemented as an independent microservice. Services share no code and communicate only through their published API contracts, which makes the API technology choice for each service visible and self-contained.

### Technologies at a Glance

| Service | Technology | Port | Framework |
|---|---|---|---|
| claim-submission | REST | 8081 | Spring Boot 3.2 + Spring MVC |
| identity-verification | SOAP/WSDL | 8082 | Spring Boot 3.2 + Spring-WS |
| policy-validation | REST | 8083 | Spring Boot 3.2 + Spring MVC |
| fraud-detection | gRPC | 9090 | grpc-java 1.62.2 + Netty (standalone) |
| eligibility | REST | 8084 | Spring Boot 3.2 + Spring MVC |
| document-review | GraphQL | 8085 | Spring Boot 3.2 + Spring for GraphQL |
| expert-assessment | REST | 8086 | Spring Boot 3.2 + Spring MVC |
| compensation | REST | 8087 | Spring Boot 3.2 + Spring MVC |
| payment-authorization | REST | 8088 | Spring Boot 3.2 + Spring MVC |
| notification | REST | 8089 | Spring Boot 3.2 + Spring MVC |
| claim-tracking | GraphQL | 8090 | Spring Boot 3.2 + Spring for GraphQL |

**Orchestration:** Bonita BPM Community 2021.2 (BPMN workflow engine)
**Java version:** 21 (records, text blocks, pattern matching)
**Build tool:** Maven 3.9+
**Container runtime:** Docker + Docker Compose

### Why Four API Technologies?

| Paradigm | Why It Was Chosen for This Service |
|---|---|
| REST | Default for most request/response operations; human-readable JSON, excellent tooling. Used for 8 of 11 services. |
| SOAP/WSDL | Identity verification requires a formal, schema-validated, auditable contract. SOAP document/literal style satisfies enterprise compliance requirements. |
| gRPC | Fraud detection runs CPU-intensive scoring logic. Binary Protobuf encoding over HTTP/2 with server-streaming RPCs lets the service push incremental risk updates without polling. |
| GraphQL | Document review and claim tracking expose graphs of nested data. GraphQL lets clients request exactly the fields they need, eliminating over-fetching across the audit trail. |

---

## 2. Architecture Diagram

```
                         BONITA BPM (Workflow Orchestrator)
                         ===================================
                         Orchestrates the full claim pipeline
                         via REST connectors to each service
                              |
          +-------------------+--------------------+
          |                   |                    |
          v                   v                    v
  +--------------+   +-------------------+  +------------------+
  | claim-       |   | identity-         |  | policy-          |
  | submission   |   | verification      |  | validation       |
  | REST :8081   |   | SOAP/WSDL :8082   |  | REST :8083       |
  +--------------+   +-------------------+  +------------------+
  POST /claims        POST /ws/identity      POST /policies/validate
  GET  /claims/{id}   WSDL: /ws/identity     JSON request/response
  GET  /claims        .wsdl                  (POL-XXXXXX pattern)
                      XML envelope
          |                   |                    |
          +-------------------+--------------------+
                              |
          +-------------------+--------------------+
          |                   |                    |
          v                   v                    v
  +--------------+   +-------------------+  +------------------+
  | fraud-       |   | eligibility       |  | document-        |
  | detection    |   | REST :8084        |  | review           |
  | gRPC :9090   |   +-------------------+  | GraphQL :8085    |
  +--------------+   POST /eligibility/  |  +------------------+
  AssessFraudRisk     check              |  POST /graphql
  StreamRiskUpdates  JSON request/       |  GET  /graphiql
  Protobuf binary     response           |  Schema: document-
  HTTP/2 transport   (amount <= $50k     |  review.graphqls
                      threshold)        |
          |                   |                    |
          +-------------------+--------------------+
                              |
          +-------------------+--------------------+
          |                   |                    |
          v                   v                    v
  +--------------+   +-------------------+  +------------------+
  | expert-      |   | compensation      |  | payment-         |
  | assessment   |   | REST :8087        |  | authorization    |
  | REST :8086   |   +-------------------+  | REST :8088       |
  +--------------+   POST /compensation/ |  +------------------+
  POST /assessments   calculate           |  POST /payments/
  JSON               formula:             |  authorize
  (90%/80%/REVIEW    gross - deductible  |  AUTHORIZED or DENIED
  decision rules)    - 2% admin tax      |
          |                   |                    |
          +-------------------+--------------------+
                              |
          +-------------------+--------------------+
          |                                        |
          v                                        v
  +--------------+                       +------------------+
  | notification |                       | claim-tracking   |
  | REST :8089   |                       | GraphQL :8090    |
  +--------------+                       +------------------+
  POST /notifications/send               POST /graphql
  EMAIL channel                          GET  /graphiql
  SENT / FAILED status                   Schema: claim-
                                         tracking.graphqls
                                         Full status history
```

### Data Flow Summary

```
ClaimRequest (JSON)
       |
       v
  [claim-submission] ──────────────────────────────────────── UUID assigned
       |
       v
  [identity-verification] ──────── SOAP XML ─────────────── VERIFIED / PENDING / FAILED
       |
       v
  [policy-validation] ──────────── REST JSON ────────────── ACTIVE / EXPIRED
       |
       v
  [fraud-detection] ────────────── gRPC Protobuf ─────────── LOW/MEDIUM/HIGH/CRITICAL risk
       |
       v
  [eligibility] ────────────────── REST JSON ────────────── eligible (amount <= $50k)
       |
       v
  [document-review] ───────────── GraphQL ───────────────── PENDING_REVIEW / APPROVED
       |
       v
  [expert-assessment] ─────────── REST JSON ────────────── APPROVE (90% or 80%) / REVIEW
       |
       v
  [compensation] ──────────────── REST JSON ────────────── grossAmount - deductible - 2% tax
       |
       v
  [payment-authorization] ─────── REST JSON ────────────── AUTHORIZED / DENIED
       |
       v
  [notification] ──────────────── REST JSON ────────────── SENT / FAILED
       |
       v
  [claim-tracking] ────────────── GraphQL ───────────────── full status history
```

---

## 3. Service Descriptions

### claim-submission (REST, port 8081)

Entry point for the entire pipeline. Accepts a `ClaimRequest` record (Java 21) with fields: `policyNumber`, `claimantName`, `incidentDate`, `description` (max 1000 chars), `estimatedAmount` (positive), and `claimType` (AUTO | HOME | HEALTH | LIFE). All fields are validated via Jakarta Bean Validation. On success, returns HTTP 201 with a `Location` header and a `ClaimResponse` containing the system-generated UUID claim ID.

**Key source files:**
- `services/claim-submission/src/main/java/com/insurance/claim/controller/ClaimController.java`
- `services/claim-submission/src/main/java/com/insurance/claim/model/ClaimRequest.java`

### identity-verification (SOAP/WSDL, port 8082)

Spring-WS endpoint implementing the `VerifyIdentity` SOAP operation (document/literal style, WS-I Basic Profile compliant). Uses JAXB 2 for XML marshalling. The WSDL is auto-served at `/ws/identity.wsdl`. Verification logic: policy numbers matching `POL-[0-9]{6}` are immediately `VERIFIED` with a `VC-XXXXXX` token; non-matching but non-blank numbers are `PENDING` for manual review; blank numbers return `FAILED`.

**Key source files:**
- `services/identity-verification/src/main/java/com/insurance/identity/endpoint/IdentityVerificationEndpoint.java`
- `services/identity-verification/src/main/resources/wsdl/identity.wsdl`

### policy-validation (REST, port 8083)

Validates whether a policy number is active and eligible for claims. Policy numbers matching `POL-[0-9]{6}` are `ACTIVE` with a $50,000 coverage amount. Any other format yields `EXPIRED` with zero coverage.

### fraud-detection (gRPC, port 9090)

A standalone gRPC server (no Spring Boot; uses `grpc-netty-shaded` and a custom `GrpcServer` lifecycle class) running on HTTP/2. Exposes two RPCs defined in `fraud.proto`:

- `AssessFraudRisk` — unary RPC returning a `FraudAssessmentResponse` with a `RiskLevel` enum (LOW/MEDIUM/HIGH/CRITICAL), a numeric score, and an `requires_investigation` flag.
- `StreamRiskUpdates` — server-streaming RPC that pushes three incremental `RiskUpdate` messages (500 ms apart) simulating a multi-stage analysis pipeline.

Risk scoring thresholds: amount > $100,000 → CRITICAL (0.95); > $50,000 → HIGH (0.80); previous claims > 3 → MEDIUM (0.60); amount > $10,000 → MEDIUM (0.40); otherwise → LOW (0.10).

**Key source files:**
- `services/fraud-detection/src/main/java/com/insurance/fraud/service/FraudDetectionServiceImpl.java`
- `services/fraud-detection/src/main/proto/fraud.proto`
- `services/fraud-detection/src/main/java/com/insurance/fraud/server/GrpcServer.java`

### eligibility (REST, port 8084)

Determines whether a claim amount is within coverage limits. Claims at or below $50,000 are eligible (score: 0.95). Claims above $50,000 are ineligible (score: 0.30) and require manual review.

### document-review (GraphQL, port 8085)

Spring for GraphQL service with a GraphiQL browser IDE at `/graphiql`. The schema (`document-review.graphqls`) defines `Document`, `DocumentType` (POLICE_REPORT | MEDICAL_RECORD | REPAIR_ESTIMATE | IDENTITY_PROOF | INSURANCE_CARD | PHOTO_EVIDENCE), and `DocumentStatus` (PENDING_REVIEW | APPROVED | REJECTED | REQUIRES_RESUBMISSION). Documents of type `POLICE_REPORT` or `MEDICAL_RECORD` are automatically flagged as structurally valid on submission.

**Key source files:**
- `services/document-review/src/main/java/com/insurance/document/controller/DocumentController.java`
- `services/document-review/src/main/resources/graphql/document-review.graphqls`

### expert-assessment (REST, port 8086)

Assigns an assessor from a pool (Dr. Marie Dupont, Jean-Pierre Martin, Sophie Bernard, Luc Moreau) deterministically by claim ID hash. Assessment tiers: amount > $100,000 → `REVIEW` (committee required, approved amount = $0); amount > $50,000 → `APPROVE` at 80% of estimated; amount <= $50,000 → `APPROVE` at 90% of estimated.

### compensation (REST, port 8087)

Calculates the net payment using the formula:
```
grossAmount  = approvedAmount
netAmount    = approvedAmount - deductible
taxAmount    = netAmount * 0.02   (2% administrative tax)
totalPayment = netAmount - taxAmount
```
Rejects requests where `deductible > approvedAmount`.

### payment-authorization (REST, port 8088)

Authorizes fund transfers. Requires `totalPayment > 0` AND a non-blank `bankAccount`. On authorization, issues a deterministic `AUTH-XXXXXX` code derived from the claim ID hash. Masks all but the last 4 characters of the account number in log output.

### notification (REST, port 8089)

Dispatches email notifications. Supported `notificationType` values: `SUBMITTED`, `APPROVED`, `REJECTED`, `PAYMENT_SENT`. Any `recipientEmail` containing `@` results in status `SENT`; otherwise `FAILED`. In production, this would invoke an SMTP or messaging provider.

### claim-tracking (GraphQL, port 8090)

Provides a complete audit trail for any claim. The `ClaimStatus` aggregate stores `currentStatus`, `statusHistory` (ordered list of `StatusEntry` objects with status, timestamp, description, and updatedBy), `lastUpdated`, and `estimatedCompletionDate`. Supports 12 status values tracking every pipeline step from `SUBMITTED` through `COMPLETED` or `REJECTED`. GraphiQL IDE available at `/graphiql`.

**Key source files:**
- `services/claim-tracking/src/main/java/com/insurance/tracking/controller/ClaimTrackingController.java`
- `services/claim-tracking/src/main/resources/graphql/claim-tracking.graphqls`

---

## 4. Prerequisites

### Required

| Tool | Version | Purpose |
|---|---|---|
| JDK | 21 | All services use Java 21 records and modern language features |
| Maven | 3.9+ | Build system for all 11 services |
| Docker | 24+ | Container runtime |
| Docker Compose | 2.x | Multi-service orchestration |

### Optional but Recommended

| Tool | Version | Purpose |
|---|---|---|
| Python | 3.11+ | Running the P1 example scripts under `examples/` |
| grpcurl | latest | Command-line gRPC client for testing fraud-detection |
| SoapUI | 5.7+ | GUI client for testing SOAP identity-verification |
| Bonita Community | 2021.2-u0 | Workflow engine for end-to-end BPM orchestration |

### Installing grpcurl

```bash
# macOS (Homebrew)
brew install grpcurl

# Linux (download binary)
curl -sSL https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_linux_x86_64.tar.gz \
  | tar -xz -C /usr/local/bin

# Verify
grpcurl --version
```

### Verifying Your Environment

```bash
java -version   # Must show: openjdk version "21"
mvn -version    # Must show: Apache Maven 3.9.x
docker version  # Must show client and server versions
docker compose version  # Must show: Docker Compose version v2.x
```

---

## 5. Repository Layout

```
insurance-claim/
├── services/                        # 11 independent microservices
│   ├── claim-submission/            # REST, port 8081
│   │   ├── pom.xml
│   │   └── src/main/java/com/insurance/claim/
│   │       ├── ClaimSubmissionApplication.java
│   │       ├── controller/ClaimController.java
│   │       ├── service/ClaimService.java
│   │       ├── repository/ClaimRepository.java
│   │       ├── model/
│   │       │   ├── ClaimRequest.java     (Java 21 record)
│   │       │   ├── ClaimResponse.java    (Java 21 record)
│   │       │   ├── ClaimType.java        (enum: AUTO/HOME/HEALTH/LIFE)
│   │       │   └── ClaimStatus.java
│   │       └── exception/
│   │           ├── ClaimNotFoundException.java
│   │           └── GlobalExceptionHandler.java
│   ├── identity-verification/       # SOAP/WSDL, port 8082
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/insurance/identity/
│   │       │   ├── IdentityVerificationApplication.java
│   │       │   ├── endpoint/IdentityVerificationEndpoint.java
│   │       │   ├── config/WebServiceConfig.java
│   │       │   └── model/
│   │       │       ├── VerifyIdentityRequest.java
│   │       │       └── VerifyIdentityResponse.java
│   │       └── resources/wsdl/identity.wsdl
│   ├── policy-validation/           # REST, port 8083
│   ├── fraud-detection/             # gRPC, port 9090
│   │   ├── pom.xml                  # Standalone (no Spring Boot)
│   │   └── src/main/
│   │       ├── java/com/insurance/fraud/
│   │       │   ├── FraudDetectionApplication.java
│   │       │   ├── server/GrpcServer.java
│   │       │   └── service/FraudDetectionServiceImpl.java
│   │       └── proto/fraud.proto    # Protobuf IDL
│   ├── eligibility/                 # REST, port 8084
│   ├── document-review/             # GraphQL, port 8085
│   │   └── src/main/resources/graphql/document-review.graphqls
│   ├── expert-assessment/           # REST, port 8086
│   ├── compensation/                # REST, port 8087
│   ├── payment-authorization/       # REST, port 8088
│   ├── notification/                # REST, port 8089
│   └── claim-tracking/              # GraphQL, port 8090
│       └── src/main/resources/graphql/claim-tracking.graphqls
│
├── api-docs/                        # Canonical API specifications
│   ├── openapi/                     # OpenAPI 3.1 YAML specs
│   │   ├── policy-validation.yaml
│   │   ├── eligibility.yaml
│   │   ├── expert-assessment.yaml
│   │   ├── compensation.yaml
│   │   ├── payment-authorization.yaml
│   │   └── notification.yaml
│   ├── wsdl/
│   │   └── identity.wsdl            # Canonical WSDL for SOAP service
│   ├── proto/
│   │   └── fraud.proto              # Canonical Protobuf IDL
│   └── graphql/
│       ├── document-review.graphqls
│       └── claim-tracking.graphqls
│
└── workflow/                        # Bonita BPM artifacts
    └── insurance_claim.bpmn         # BPMN 2.0 process definition
```

---

## 6. Build Instructions

### Building a Single Service

Each service is a self-contained Maven project. Navigate to its directory and run:

```bash
cd services/claim-submission && mvn clean package -DskipTests
```

For the fraud-detection service (standalone gRPC, not Spring Boot), the build also runs `protoc` to generate Java classes from `fraud.proto`:

```bash
cd services/fraud-detection && mvn clean package -DskipTests
# Generated sources appear in:
# target/generated-sources/protobuf/java/    (message classes)
# target/generated-sources/protobuf/grpc-java/  (service stubs)
```

### Building All Services

Run the following script from the repository root:

```bash
for svc in claim-submission identity-verification policy-validation fraud-detection \
  eligibility document-review expert-assessment compensation payment-authorization \
  notification claim-tracking; do
  echo "Building $svc..."
  (cd services/$svc && mvn clean package -DskipTests)
  echo "$svc built successfully."
  echo "---"
done
```

Expected output for each service ends with:
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXX s
```

### Build Artifacts

| Service | JAR Location |
|---|---|
| claim-submission | `services/claim-submission/target/claim-submission-1.0.0.jar` |
| identity-verification | `services/identity-verification/target/identity-verification-1.0.0.jar` |
| policy-validation | `services/policy-validation/target/policy-validation-1.0.0.jar` |
| fraud-detection | `services/fraud-detection/target/fraud-detection.jar` (executable JAR) |
| eligibility | `services/eligibility/target/eligibility-1.0.0.jar` |
| document-review | `services/document-review/target/document-review-1.0.0.jar` |
| expert-assessment | `services/expert-assessment/target/expert-assessment-1.0.0.jar` |
| compensation | `services/compensation/target/compensation-1.0.0.jar` |
| payment-authorization | `services/payment-authorization/target/payment-authorization-1.0.0.jar` |
| notification | `services/notification/target/notification-1.0.0.jar` |
| claim-tracking | `services/claim-tracking/target/claim-tracking-1.0.0.jar` |

---

## 7. Running with Docker Compose

### Start All Services

```bash
# Build images and start all 11 services in foreground
docker-compose up --build

# Or in detached mode (recommended for development)
docker-compose up -d --build
```

### Verify All Services Are Running

```bash
docker-compose ps
```

All 11 containers should show status `Up`. Check individual service health:

```bash
# REST services expose Spring Actuator health endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health
curl http://localhost:8088/actuator/health
curl http://localhost:8089/actuator/health
curl http://localhost:8090/actuator/health
```

Expected response:
```json
{"status":"UP","components":{"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}
```

For the gRPC service (no HTTP actuator), use grpcurl to probe:

```bash
grpcurl -plaintext localhost:9090 list
# Expected: com.insurance.fraud.FraudDetectionService
```

### Stop All Services

```bash
docker-compose down

# Remove volumes too (clears any persisted state):
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs -f

# Single service
docker-compose logs -f fraud-detection
docker-compose logs -f claim-submission
```

---

## 8. Testing Each API Type

The examples below use `CLM-001` and `POL-123456` as sample identifiers throughout, so responses from earlier steps can be fed into later steps.

---

### 8.1 REST — claim-submission (port 8081)

**Submit a new claim**

```bash
curl -X POST http://localhost:8081/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-123456",
    "claimantName": "Jean Dupont",
    "incidentDate": "2024-01-15",
    "description": "Vehicle damage after collision at intersection of Rue de Rivoli and Boulevard Saint-Michel.",
    "estimatedAmount": 8500.00,
    "claimType": "AUTO"
  }'
```

Expected response (HTTP 201):
```json
{
  "claimId": "a3f7d2b1-9e4c-4a8f-b6d0-123456789abc",
  "status": "SUBMITTED",
  "message": "Claim submitted successfully.",
  "submissionTimestamp": "2024-01-15T10:30:00",
  "policyNumber": "POL-123456"
}
```

> The `Location` response header will contain `/claims/a3f7d2b1-9e4c-4a8f-b6d0-123456789abc`. Save this UUID for subsequent steps.

**Retrieve a specific claim by ID**

```bash
curl http://localhost:8081/claims/{claimId}
# Replace {claimId} with the UUID returned above
```

**List all submitted claims**

```bash
curl http://localhost:8081/claims
```

Returns an array of `ClaimResponse` objects. Returns `[]` when no claims exist.

**Health check**

```bash
curl http://localhost:8081/actuator/health
```

**Valid claim types:** `AUTO`, `HOME`, `HEALTH`, `LIFE`

**Validation errors** return HTTP 400 with a structured error body:

```bash
curl -X POST http://localhost:8081/claims \
  -H "Content-Type: application/json" \
  -d '{"policyNumber": "", "claimantName": "Test"}'
```

```json
{
  "status": 400,
  "error": "Validation Failed",
  "details": [
    "Policy number must not be blank",
    "Incident date is required",
    "Description must not be blank",
    "Estimated amount is required",
    "Claim type is required"
  ]
}
```

---

### 8.2 REST — policy-validation (port 8083)

**Validate an active policy**

```bash
curl -X POST http://localhost:8083/policies/validate \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-123456",
    "claimantName": "Jean Dupont",
    "claimType": "PROPERTY_DAMAGE"
  }'
```

Expected response (HTTP 200):
```json
{
  "valid": true,
  "policyStatus": "ACTIVE",
  "coverageAmount": 50000.0,
  "message": "Policy is active and eligible for claims. Coverage amount: $50000.0"
}
```

**Validate an expired/invalid policy**

```bash
curl -X POST http://localhost:8083/policies/validate \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "EXPIRED-999",
    "claimantName": "Jean Dupont",
    "claimType": "VEHICLE"
  }'
```

```json
{
  "valid": false,
  "policyStatus": "EXPIRED",
  "coverageAmount": 0.0,
  "message": "Policy number EXPIRED-999 does not match a valid active policy format."
}
```

**Policy number format:** `POL-` followed by exactly 6 digits (e.g., `POL-000001` through `POL-999999`).

**Valid claim types:** `PROPERTY_DAMAGE`, `MEDICAL`, `VEHICLE`, `LIABILITY`, `LIFE`

---

### 8.3 SOAP — identity-verification (port 8082)

**Retrieve the WSDL contract**

```bash
curl http://localhost:8082/ws/identity.wsdl
```

This returns the full WSDL document. The service is at namespace `http://insurance.com/identity`, operation `VerifyIdentity`, SOAP 1.1, document/literal style.

**Verify a valid identity (VERIFIED outcome)**

```bash
curl -X POST http://localhost:8082/ws/identity \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: \"http://insurance.com/identity/VerifyIdentity\"" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <tns:verifyIdentityRequest xmlns:tns="http://insurance.com/identity">
      <tns:policyNumber>POL-123456</tns:policyNumber>
      <tns:claimantName>Jean Dupont</tns:claimantName>
      <tns:dateOfBirth>1985-07-22</tns:dateOfBirth>
    </tns:verifyIdentityRequest>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>'
```

Expected SOAP response:
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <ns2:verifyIdentityResponse xmlns:ns2="http://insurance.com/identity">
      <ns2:verificationStatus>VERIFIED</ns2:verificationStatus>
      <ns2:verificationCode>VC-748291</ns2:verificationCode>
      <ns2:message>Identity successfully verified.</ns2:message>
    </ns2:verifyIdentityResponse>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

**Test PENDING outcome** (policy format does not match `POL-[0-9]{6}`):

```bash
curl -X POST http://localhost:8082/ws/identity \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: \"\"" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
  <SOAP-ENV:Body>
    <tns:verifyIdentityRequest xmlns:tns="http://insurance.com/identity">
      <tns:policyNumber>CUSTOM-POLICY-XYZ</tns:policyNumber>
      <tns:claimantName>Jean Dupont</tns:claimantName>
      <tns:dateOfBirth>1985-07-22</tns:dateOfBirth>
    </tns:verifyIdentityRequest>
  </SOAP-ENV:Body>
</SOAP-ENV:Envelope>'
```

Returns `verificationStatus: PENDING` and an empty `verificationCode`.

**Test FAILED outcome** (blank policy number):

```bash
# Send empty policyNumber element
<tns:policyNumber></tns:policyNumber>
```

Returns `verificationStatus: FAILED`.

**Using SoapUI:** Import the WSDL from `http://localhost:8082/ws/identity.wsdl`. SoapUI will auto-generate a request template. Fill in the three fields and click Send.

---

### 8.4 gRPC — fraud-detection (port 9090)

The fraud-detection service runs a pure gRPC server over HTTP/2 with no REST adapter. You need either `grpcurl` or a gRPC client library.

**List available services**

```bash
grpcurl -plaintext localhost:9090 list
```

```
com.insurance.fraud.FraudDetectionService
```

**Describe the service**

```bash
grpcurl -plaintext localhost:9090 describe com.insurance.fraud.FraudDetectionService
```

**AssessFraudRisk (unary RPC) — standard claim**

```bash
grpcurl -plaintext -d '{
  "claim_id": "CLM-001",
  "policy_number": "POL-123456",
  "estimated_amount": 8500.0,
  "claim_type": "AUTO",
  "incident_date": "2024-01-15",
  "previous_claims_count": 1
}' localhost:9090 com.insurance.fraud.FraudDetectionService/AssessFraudRisk
```

Expected response (LOW risk — amount $8,500 is between $0 and $10,000):
```json
{
  "claimId": "CLM-001",
  "riskLevel": "LOW",
  "riskScore": 0.1,
  "assessmentReason": "Standard claim",
  "requiresInvestigation": false
}
```

**AssessFraudRisk — moderate amount (MEDIUM risk)**

```bash
grpcurl -plaintext -d '{
  "claim_id": "CLM-002",
  "policy_number": "POL-654321",
  "estimated_amount": 25000.0,
  "claim_type": "HOME",
  "incident_date": "2024-02-01",
  "previous_claims_count": 2
}' localhost:9090 com.insurance.fraud.FraudDetectionService/AssessFraudRisk
```

```json
{
  "claimId": "CLM-002",
  "riskLevel": "MEDIUM",
  "riskScore": 0.4,
  "assessmentReason": "Moderate claim amount",
  "requiresInvestigation": false
}
```

**AssessFraudRisk — high-value claim (CRITICAL risk)**

```bash
grpcurl -plaintext -d '{
  "claim_id": "CLM-003",
  "policy_number": "POL-999999",
  "estimated_amount": 150000.0,
  "claim_type": "LIFE",
  "incident_date": "2024-03-01",
  "previous_claims_count": 0
}' localhost:9090 com.insurance.fraud.FraudDetectionService/AssessFraudRisk
```

```json
{
  "claimId": "CLM-003",
  "riskLevel": "CRITICAL",
  "riskScore": 0.95,
  "assessmentReason": "Extremely high claim amount",
  "requiresInvestigation": true
}
```

**StreamRiskUpdates (server-streaming RPC)**

```bash
grpcurl -plaintext -d '{
  "claim_id": "CLM-001",
  "policy_number": "POL-123456",
  "estimated_amount": 8500.0,
  "claim_type": "AUTO",
  "incident_date": "2024-01-15",
  "previous_claims_count": 1
}' localhost:9090 com.insurance.fraud.FraudDetectionService/StreamRiskUpdates
```

The server streams three messages with 500 ms pauses between each:
```json
{"message": "[Stage 1/3] Claim CLM-001 received. Validating data integrity ...", "currentScore": 0.1}
{"message": "[Stage 2/3] Claim CLM-001 — analysing historical patterns. Amount: 8500.00 | Previous claims: 1 | Intermediate score: 0.06", "currentScore": 0.06}
{"message": "[Stage 3/3] Claim CLM-001 — analysis complete. Final risk level: LOW | Score: 0.10 | Requires investigation: false", "currentScore": 0.1}
```

**Risk scoring thresholds summary:**

| Condition | Risk Level | Score | Investigation Required |
|---|---|---|---|
| estimatedAmount > $100,000 | CRITICAL | 0.95 | Yes |
| estimatedAmount > $50,000 | HIGH | 0.80 | Yes |
| previousClaimsCount > 3 | MEDIUM | 0.60 | No |
| estimatedAmount > $10,000 | MEDIUM | 0.40 | No |
| (default) | LOW | 0.10 | No |

---

### 8.5 REST — eligibility (port 8084)

**Check eligibility for an eligible claim**

```bash
curl -X POST http://localhost:8084/eligibility/check \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "policyNumber": "POL-123456",
    "estimatedAmount": 8500.00,
    "claimType": "AUTO"
  }'
```

Expected response:
```json
{
  "eligible": true,
  "eligibilityScore": 0.95,
  "message": "Claim amount is within the maximum coverage limit of $50000.0. Claim is eligible for processing.",
  "maxCoverageAmount": 50000.0
}
```

**Check eligibility for an ineligible claim**

```bash
curl -X POST http://localhost:8084/eligibility/check \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-003",
    "policyNumber": "POL-999999",
    "estimatedAmount": 75000.00,
    "claimType": "HOME"
  }'
```

```json
{
  "eligible": false,
  "eligibilityScore": 0.3,
  "message": "Claim amount of $75000.0 exceeds the maximum coverage limit of $50000.0. Manual review required.",
  "maxCoverageAmount": 50000.0
}
```

---

### 8.6 GraphQL — document-review (port 8085)

The GraphQL endpoint is at `http://localhost:8085/graphql`. A browser-based IDE (GraphiQL) is available at `http://localhost:8085/graphiql`.

**Submit a document (mutation)**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { submitDocument(claimId: \"CLM-001\", documentType: POLICE_REPORT, fileName: \"police_report_2024.pdf\") { id claimId documentType fileName status valid } }"
  }'
```

Expected response:
```json
{
  "data": {
    "submitDocument": {
      "id": "doc-001",
      "claimId": "CLM-001",
      "documentType": "POLICE_REPORT",
      "fileName": "police_report_2024.pdf",
      "status": "PENDING_REVIEW",
      "valid": true
    }
  }
}
```

> POLICE_REPORT and MEDICAL_RECORD documents are automatically marked as `valid: true`. All other types start as `valid: false`.

**Submit additional documents**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { submitDocument(claimId: \"CLM-001\", documentType: REPAIR_ESTIMATE, fileName: \"repair_quote_garage_central.pdf\") { id documentType status valid } }"
  }'
```

**Review a document (mutation)**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { reviewDocument(id: \"doc-001\", status: APPROVED, reviewNotes: \"Police report verified. Consistent with claim description.\") { id status reviewNotes reviewedAt valid } }"
  }'
```

**Query all documents for a claim**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ getDocumentsByClaimId(claimId: \"CLM-001\") { id documentType fileName status valid reviewNotes reviewedAt } }"
  }'
```

**Query a single document by ID**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getDocument(id: \"doc-001\") { id claimId documentType status valid } }"}'
```

**Get all pending documents**

```bash
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getPendingDocuments { id claimId documentType fileName } }"}'
```

**Available DocumentType values:** `POLICE_REPORT`, `MEDICAL_RECORD`, `REPAIR_ESTIMATE`, `IDENTITY_PROOF`, `INSURANCE_CARD`, `PHOTO_EVIDENCE`

**Available DocumentStatus values:** `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `REQUIRES_RESUBMISSION`

---

### 8.7 REST — expert-assessment (port 8086)

**Standard claim assessment (amount <= $50,000 — approved at 90%)**

```bash
curl -X POST http://localhost:8086/assessments \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "claimType": "AUTO",
    "estimatedAmount": 8500.00,
    "description": "Vehicle damage after collision. Repair quote obtained from certified garage."
  }'
```

Expected response (HTTP 201):
```json
{
  "assessmentId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "claimId": "CLM-001",
  "approvedAmount": 7650.00,
  "assessorName": "Dr. Marie Dupont",
  "recommendation": "APPROVE",
  "notes": "Standard claim approved at 90% rate: $7650.00."
}
```

**Medium-value claim (amount > $50,000 — approved at 80%)**

```bash
curl -X POST http://localhost:8086/assessments \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-002",
    "claimType": "PROPERTY_DAMAGE",
    "estimatedAmount": 72000.00,
    "description": "Severe water damage to residential property."
  }'
```

```json
{
  "approvedAmount": 57600.00,
  "recommendation": "APPROVE",
  "notes": "Claim amount of $72000.0 is above $50000.0. Approved at 80% rate: $57600.00."
}
```

**High-value claim (amount > $100,000 — flagged for committee review)**

```bash
curl -X POST http://localhost:8086/assessments \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-003",
    "claimType": "LIABILITY",
    "estimatedAmount": 150000.00,
    "description": "Third-party liability from industrial accident."
  }'
```

```json
{
  "approvedAmount": 0.0,
  "recommendation": "REVIEW",
  "notes": "Claim amount of $150000.0 exceeds the high-value threshold of $100000.0. Requires committee review before approval."
}
```

**Assessment tier summary:**

| Estimated Amount | Recommendation | Approved Amount |
|---|---|---|
| <= $50,000 | APPROVE | 90% of estimated |
| $50,001 – $100,000 | APPROVE | 80% of estimated |
| > $100,000 | REVIEW | $0 (committee required) |

---

### 8.8 REST — compensation (port 8087)

**Calculate compensation**

```bash
curl -X POST http://localhost:8087/compensation/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "approvedAmount": 7650.00,
    "claimType": "AUTO",
    "deductible": 500.00
  }'
```

Expected response:
```json
{
  "claimId": "CLM-001",
  "grossAmount": 7650.00,
  "deductible": 500.00,
  "netAmount": 7150.00,
  "taxAmount": 143.00,
  "totalPayment": 7007.00
}
```

**Calculation formula:**
```
grossAmount  = approvedAmount            = 7650.00
netAmount    = grossAmount - deductible  = 7650.00 - 500.00  = 7150.00
taxAmount    = netAmount   * 0.02        = 7150.00 * 0.02    =  143.00
totalPayment = netAmount   - taxAmount   = 7150.00 - 143.00  = 7007.00
```

**Zero deductible example:**

```bash
curl -X POST http://localhost:8087/compensation/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-004",
    "approvedAmount": 20000.00,
    "claimType": "MEDICAL",
    "deductible": 0.0
  }'
```

```json
{
  "grossAmount": 20000.00,
  "deductible": 0.0,
  "netAmount": 20000.00,
  "taxAmount": 400.00,
  "totalPayment": 19600.00
}
```

> An HTTP 400 is returned if `deductible > approvedAmount`.

---

### 8.9 REST — payment-authorization (port 8088)

**Authorize a valid payment**

```bash
curl -X POST http://localhost:8088/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "totalPayment": 7007.00,
    "policyNumber": "POL-123456",
    "bankAccount": "FR7630006000011234567890189"
  }'
```

Expected response:
```json
{
  "authorizationId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "claimId": "CLM-001",
  "status": "AUTHORIZED",
  "authorizationCode": "AUTH-045231",
  "message": "Payment of $7007.00 authorized for claim CLM-001. Funds will be transferred to account ending in *********************0189."
}
```

**Payment denial — zero amount**

```bash
curl -X POST http://localhost:8088/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "totalPayment": 0.0,
    "policyNumber": "POL-123456",
    "bankAccount": "FR7630006000011234567890189"
  }'
```

```json
{
  "status": "DENIED",
  "authorizationCode": "N/A",
  "message": "Payment denied: total payment amount must be greater than zero."
}
```

**Authorization rules:**
- `totalPayment > 0` AND `bankAccount` is non-blank → `AUTHORIZED`
- Any other combination → `DENIED`

---

### 8.10 REST — notification (port 8089)

**Send an APPROVED notification**

```bash
curl -X POST http://localhost:8089/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "recipientEmail": "jean.dupont@example.com",
    "notificationType": "APPROVED",
    "message": "Your claim CLM-001 has been approved. Payment of $7,007.00 will be transferred within 5 business days."
  }'
```

Expected response:
```json
{
  "notificationId": "e5f6a7b8-c9d0-1234-ef01-345678901234",
  "status": "SENT",
  "channel": "EMAIL",
  "timestamp": "2024-01-15T14:32:00"
}
```

**Send a PAYMENT_SENT notification**

```bash
curl -X POST http://localhost:8089/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "recipientEmail": "jean.dupont@example.com",
    "notificationType": "PAYMENT_SENT",
    "message": "Your payment of $7,007.00 has been transferred to your bank account ending in 0189."
  }'
```

**Test delivery failure** (invalid email):

```bash
curl -X POST http://localhost:8089/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "CLM-001",
    "recipientEmail": "not-a-valid-email",
    "notificationType": "SUBMITTED",
    "message": "Your claim has been submitted."
  }'
```

```json
{"status": "FAILED", "channel": "EMAIL"}
```

**Valid notificationType values:** `SUBMITTED`, `APPROVED`, `REJECTED`, `PAYMENT_SENT`

---

### 8.11 GraphQL — claim-tracking (port 8090)

The GraphQL endpoint is at `http://localhost:8090/graphql`. GraphiQL IDE is available at `http://localhost:8090/graphiql`.

**Initialize tracking for a new claim (mutation)**

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { initializeClaim(claimId: \"CLM-001\") { claimId currentStatus lastUpdated estimatedCompletionDate statusHistory { status timestamp description updatedBy } } }"
  }'
```

Expected response:
```json
{
  "data": {
    "initializeClaim": {
      "claimId": "CLM-001",
      "currentStatus": "SUBMITTED",
      "lastUpdated": "2024-01-15T10:30:00",
      "estimatedCompletionDate": "2024-01-22",
      "statusHistory": [
        {
          "status": "SUBMITTED",
          "timestamp": "2024-01-15T10:30:00",
          "description": "Claim successfully submitted.",
          "updatedBy": "SYSTEM"
        }
      ]
    }
  }
}
```

**Advance claim status (mutation)**

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation { updateClaimStatus(claimId: \"CLM-001\", status: IDENTITY_VERIFIED, description: \"Identity verified via SOAP service. Verification code: VC-748291.\", updatedBy: \"IDENTITY_SERVICE\") { claimId currentStatus lastUpdated statusHistory { status timestamp description updatedBy } } }"
  }'
```

**Track a claim — full status history**

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ trackClaim(claimId: \"CLM-001\") { claimId currentStatus lastUpdated estimatedCompletionDate statusHistory { status timestamp description updatedBy } } }"
  }'
```

**Filter claims by status**

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getClaimsByStatus(status: FRAUD_CHECKED) { claimId currentStatus lastUpdated } }"}'
```

**Get all tracked claims**

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getAllClaims { claimId currentStatus lastUpdated } }"}'
```

**Available Status enum values (in pipeline order):**

| Status | Pipeline Step |
|---|---|
| `SUBMITTED` | Claim received by claim-submission |
| `IDENTITY_VERIFIED` | Identity confirmed by identity-verification |
| `POLICY_VALIDATED` | Policy active per policy-validation |
| `FRAUD_CHECKED` | Fraud risk scored by fraud-detection |
| `ELIGIBILITY_CONFIRMED` | Coverage limit confirmed by eligibility |
| `DOCUMENTS_REVIEWED` | Documents approved by document-review |
| `EXPERT_ASSESSED` | Approved amount set by expert-assessment |
| `COMPENSATION_CALCULATED` | Net payment computed by compensation |
| `PAYMENT_AUTHORIZED` | Transfer authorized by payment-authorization |
| `NOTIFIED` | Claimant notified by notification |
| `COMPLETED` | All steps finished (terminal state) |
| `REJECTED` | Claim rejected at any step (terminal state) |

---

## 9. End-to-End Walkthrough

This walkthrough processes a single AUTO claim through all eleven services in sequence, using the outputs from each step as inputs to the next.

### Step 1 — Submit the Claim

```bash
RESPONSE=$(curl -s -X POST http://localhost:8081/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-123456",
    "claimantName": "Jean Dupont",
    "incidentDate": "2024-01-15",
    "description": "Vehicle damage after rear-end collision.",
    "estimatedAmount": 8500.00,
    "claimType": "AUTO"
  }')
echo $RESPONSE
# Note the claimId in the response
CLAIM_ID=$(echo $RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['claimId'])")
```

### Step 2 — Initialize Tracking

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"mutation { initializeClaim(claimId: \\\"$CLAIM_ID\\\") { claimId currentStatus } }\"}"
```

### Step 3 — Verify Identity (SOAP)

```bash
curl -s -X POST http://localhost:8082/ws/identity \
  -H "Content-Type: text/xml;charset=UTF-8" \
  -H "SOAPAction: \"\"" \
  -d "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">
        <SOAP-ENV:Body>
          <tns:verifyIdentityRequest xmlns:tns=\"http://insurance.com/identity\">
            <tns:policyNumber>POL-123456</tns:policyNumber>
            <tns:claimantName>Jean Dupont</tns:claimantName>
            <tns:dateOfBirth>1985-07-22</tns:dateOfBirth>
          </tns:verifyIdentityRequest>
        </SOAP-ENV:Body>
      </SOAP-ENV:Envelope>"
```

### Step 4 — Validate Policy (REST)

```bash
curl -s -X POST http://localhost:8083/policies/validate \
  -H "Content-Type: application/json" \
  -d '{"policyNumber": "POL-123456", "claimantName": "Jean Dupont", "claimType": "VEHICLE"}'
```

### Step 5 — Assess Fraud Risk (gRPC)

```bash
grpcurl -plaintext -d "{
  \"claim_id\": \"$CLAIM_ID\",
  \"policy_number\": \"POL-123456\",
  \"estimated_amount\": 8500.0,
  \"claim_type\": \"AUTO\",
  \"incident_date\": \"2024-01-15\",
  \"previous_claims_count\": 1
}" localhost:9090 com.insurance.fraud.FraudDetectionService/AssessFraudRisk
```

### Step 6 — Check Eligibility (REST)

```bash
curl -s -X POST http://localhost:8084/eligibility/check \
  -H "Content-Type: application/json" \
  -d "{\"claimId\": \"$CLAIM_ID\", \"policyNumber\": \"POL-123456\", \"estimatedAmount\": 8500.00, \"claimType\": \"AUTO\"}"
```

### Step 7 — Submit and Review Document (GraphQL)

```bash
# Submit document
curl -s -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"mutation { submitDocument(claimId: \\\"$CLAIM_ID\\\", documentType: POLICE_REPORT, fileName: \\\"police_report.pdf\\\") { id status valid } }\"}"

# Review it (use doc-id from above response)
curl -s -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { reviewDocument(id: \"doc-001\", status: APPROVED, reviewNotes: \"Document verified.\") { id status } }"}'
```

### Step 8 — Expert Assessment (REST)

```bash
curl -s -X POST http://localhost:8086/assessments \
  -H "Content-Type: application/json" \
  -d "{\"claimId\": \"$CLAIM_ID\", \"claimType\": \"AUTO\", \"estimatedAmount\": 8500.00, \"description\": \"Vehicle damage after rear-end collision.\"}"
# approvedAmount = 8500.00 * 0.90 = 7650.00
```

### Step 9 — Calculate Compensation (REST)

```bash
curl -s -X POST http://localhost:8087/compensation/calculate \
  -H "Content-Type: application/json" \
  -d "{\"claimId\": \"$CLAIM_ID\", \"approvedAmount\": 7650.00, \"claimType\": \"AUTO\", \"deductible\": 500.00}"
# totalPayment = (7650.00 - 500.00) * 0.98 = 7007.00
```

### Step 10 — Authorize Payment (REST)

```bash
curl -s -X POST http://localhost:8088/payments/authorize \
  -H "Content-Type: application/json" \
  -d "{\"claimId\": \"$CLAIM_ID\", \"totalPayment\": 7007.00, \"policyNumber\": \"POL-123456\", \"bankAccount\": \"FR7630006000011234567890189\"}"
```

### Step 11 — Send Notification (REST)

```bash
curl -s -X POST http://localhost:8089/notifications/send \
  -H "Content-Type: application/json" \
  -d "{\"claimId\": \"$CLAIM_ID\", \"recipientEmail\": \"jean.dupont@example.com\", \"notificationType\": \"PAYMENT_SENT\", \"message\": \"Your payment of 7007.00 EUR has been authorized.\"}"
```

### Step 12 — Mark Claim Completed and Review Full History

```bash
# Mark as COMPLETED
curl -s -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"mutation { updateClaimStatus(claimId: \\\"$CLAIM_ID\\\", status: COMPLETED, description: \\\"All pipeline steps completed. Payment authorized.\\\", updatedBy: \\\"BONITA_BPM\\\") { claimId currentStatus } }\"}"

# View complete audit trail
curl -s -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"{ trackClaim(claimId: \\\"$CLAIM_ID\\\") { claimId currentStatus statusHistory { status timestamp description updatedBy } } }\"}"
```

---

## 10. Bonita BPM Integration

Bonita BPM Community edition orchestrates the claim pipeline by calling each microservice in sequence through REST connectors configured within the BPMN process.

### Setup Instructions

Full step-by-step instructions are documented in `workflow/bonita-setup.md`.

### Quick Start

1. **Download Bonita Community 2021.2-u0** from [https://www.bonitasoft.com/downloads](https://www.bonitasoft.com/downloads)

2. **Start Bonita Studio** and open the workspace.

3. **Import the process definition:**
   - File > Import > Bonita 7.x .bos file
   - Select `workflow/insurance_claim.bpmn`

4. **Configure REST connectors** for each service call. Each BPMN service task maps to one microservice:

| BPMN Task Name | Service URL | HTTP Method | Content-Type |
|---|---|---|---|
| Submit Claim | `http://localhost:8081/claims` | POST | application/json |
| Verify Identity | `http://localhost:8082/ws/identity` | POST | text/xml |
| Validate Policy | `http://localhost:8083/policies/validate` | POST | application/json |
| Check Eligibility | `http://localhost:8084/eligibility/check` | POST | application/json |
| Request Assessment | `http://localhost:8086/assessments` | POST | application/json |
| Calculate Compensation | `http://localhost:8087/compensation/calculate` | POST | application/json |
| Authorize Payment | `http://localhost:8088/payments/authorize` | POST | application/json |
| Send Notification | `http://localhost:8089/notifications/send` | POST | application/json |

> The fraud-detection (gRPC) and document-review / claim-tracking (GraphQL) services require custom connector implementations or a REST-to-gRPC/GraphQL adapter layer when using Bonita. Refer to `workflow/bonita-setup.md` for adapter configuration.

5. **Deploy and run** the process from Bonita Portal.

### BPMN Process Overview

The process models a linear pipeline with decision gateways at key risk checkpoints:

```
[Start] -> [Submit Claim] -> [Verify Identity] --FAILED--> [Reject & Notify]
                                    |
                                 VERIFIED
                                    |
                          [Validate Policy] --EXPIRED--> [Reject & Notify]
                                    |
                                  ACTIVE
                                    |
                          [Assess Fraud Risk] --CRITICAL--> [Manual Review]
                                    |
                                  LOW/MEDIUM
                                    |
                          [Check Eligibility] --INELIGIBLE--> [Reject & Notify]
                                    |
                                 ELIGIBLE
                                    |
                          [Review Documents] -> [Expert Assessment]
                                                       |
                                               [Calculate Compensation]
                                                       |
                                               [Authorize Payment]
                                                       |
                                               [Send Notification]
                                                       |
                                                    [End]
```

---

## 11. API Documentation

All canonical API specifications are stored under `api-docs/`:

### OpenAPI 3.1 Specifications (REST services)

| File | Service | Port |
|---|---|---|
| `api-docs/openapi/policy-validation.yaml` | policy-validation | 8083 |
| `api-docs/openapi/eligibility.yaml` | eligibility | 8084 |
| `api-docs/openapi/expert-assessment.yaml` | expert-assessment | 8086 |
| `api-docs/openapi/compensation.yaml` | compensation | 8087 |
| `api-docs/openapi/payment-authorization.yaml` | payment-authorization | 8088 |
| `api-docs/openapi/notification.yaml` | notification | 8089 |

Render any spec with Swagger UI:

```bash
# Using Docker
docker run -p 8080:8080 \
  -e SWAGGER_JSON=/specs/compensation.yaml \
  -v $(pwd)/api-docs/openapi:/specs \
  swaggerapi/swagger-ui
# Open http://localhost:8080
```

### SOAP WSDL

- **Canonical spec:** `api-docs/wsdl/identity.wsdl`
- **Live endpoint:** `http://localhost:8082/ws/identity.wsdl` (auto-served by Spring-WS)
- **Namespace:** `http://insurance.com/identity`
- **Style:** document/literal (WS-I Basic Profile compliant)
- **Operation:** `VerifyIdentity` (synchronous request/response)

### Protocol Buffers (gRPC)

- **Canonical spec:** `api-docs/proto/fraud.proto`
- **Service package:** `com.insurance.fraud`
- **Java package:** `com.insurance.fraud.proto`
- **RPCs:** `AssessFraudRisk` (unary), `StreamRiskUpdates` (server-streaming)

Compile the proto independently:

```bash
protoc \
  --proto_path=api-docs/proto \
  --java_out=./generated \
  --grpc-java_out=./generated \
  api-docs/proto/fraud.proto
```

### GraphQL Schemas

| File | Service | Port | Operations |
|---|---|---|---|
| `api-docs/graphql/document-review.graphqls` | document-review | 8085 | 3 Queries + 2 Mutations |
| `api-docs/graphql/claim-tracking.graphqls` | claim-tracking | 8090 | 3 Queries + 2 Mutations |

Live schema introspection:

```bash
# Introspect document-review schema
curl -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __schema { types { name kind } } }"}'
```

---

## 12. Business Rules Reference

### Identity Verification (SOAP, port 8082)

| Policy Number Input | Result Status | Verification Code | Notes |
|---|---|---|---|
| null or blank | FAILED | (empty) | Immediate rejection |
| Matches `POL-[0-9]{6}` | VERIFIED | VC-XXXXXX (random 6 digits, SecureRandom) | Auto-approved |
| Any other non-blank value | PENDING | (empty) | Manual review queue |

### Policy Validation (REST, port 8083)

| Policy Number | Status | Coverage Amount |
|---|---|---|
| Matches `POL-[0-9]{6}` | ACTIVE | $50,000.00 |
| Any other value | EXPIRED | $0.00 |

### Fraud Detection (gRPC, port 9090)

| Condition (evaluated in order) | Risk Level | Score | Investigation |
|---|---|---|---|
| `estimatedAmount > 100,000` | CRITICAL | 0.95 | Required |
| `estimatedAmount > 50,000` | HIGH | 0.80 | Required |
| `previousClaimsCount > 3` | MEDIUM | 0.60 | Not required |
| `estimatedAmount > 10,000` | MEDIUM | 0.40 | Not required |
| Default | LOW | 0.10 | Not required |

### Eligibility (REST, port 8084)

| Claim Amount | Eligible | Score | Max Coverage |
|---|---|---|---|
| <= $50,000 | Yes | 0.95 | $50,000 |
| > $50,000 | No | 0.30 | $50,000 |

### Expert Assessment (REST, port 8086)

| Estimated Amount | Recommendation | Approved Amount | Example (input $80,000) |
|---|---|---|---|
| <= $50,000 | APPROVE | 90% of estimated | N/A |
| $50,001 – $100,000 | APPROVE | 80% of estimated | $64,000 |
| > $100,000 | REVIEW | $0 (committee) | N/A |

### Compensation (REST, port 8087)

```
grossAmount  = approvedAmount
netAmount    = grossAmount - deductible
taxAmount    = netAmount * 0.02
totalPayment = netAmount - taxAmount
```

Constraint: `deductible` must not exceed `approvedAmount`. Violation returns HTTP 400.

### Payment Authorization (REST, port 8088)

| totalPayment | bankAccount | Result |
|---|---|---|
| > 0 | Non-blank | AUTHORIZED (AUTH-XXXXXX) |
| <= 0 | Any | DENIED |
| > 0 | Blank | DENIED |
| <= 0 | Blank | DENIED |

### Notification (REST, port 8089)

| recipientEmail | Result | Notes |
|---|---|---|
| Contains `@` | SENT | Simulates successful delivery |
| Does not contain `@` | FAILED | Invalid email format |

---

## 13. Technology Comparison

This system was explicitly designed to place different communication paradigms side-by-side. The following table captures the trade-offs observed across the 11 services.

| Dimension | REST (8 services) | SOAP/WSDL (1 service) | gRPC (1 service) | GraphQL (2 services) |
|---|---|---|---|---|
| **Transport** | HTTP/1.1 | HTTP/1.1 | HTTP/2 | HTTP/1.1 |
| **Data Format** | JSON (text) | XML (text, verbose) | Protobuf (binary) | JSON (text) |
| **Schema Enforcement** | Optional (OpenAPI) | Strict (XSD in WSDL) | Strict (.proto IDL) | Strict (.graphqls) |
| **Contract First?** | No (code first) | Yes (WSDL required) | Yes (.proto required) | Yes (schema required) |
| **Payload Size** | Medium | Large (XML overhead) | Small (binary) | Client-controlled |
| **Human Readable** | Yes | Yes (verbose) | No | Yes |
| **Streaming Support** | No (polling only) | No | Yes (4 patterns) | Subscriptions |
| **Code Generation** | Optional | Strong (wsimport) | Strong (protoc) | Optional |
| **Browser Testable** | Yes (curl/Postman) | Partial (SoapUI) | No (needs grpcurl) | Yes (GraphiQL) |
| **Over-fetching** | Possible | Common | Not applicable | Eliminated by design |
| **Under-fetching** | Possible | Common | Not applicable | Eliminated by design |
| **Best Suited For** | Standard CRUD APIs, mobile, microservices | Enterprise integration, auditing, legacy interop | High-performance internal services, streaming | Complex nested data, flexible clients |
| **Used Here For** | 8 general pipeline steps | Identity verification (formal audit contract) | Fraud detection (high-throughput, streaming) | Document/claim graphs with selective field fetching |

### Key Architectural Observations

**REST** is the default choice for 8 of 11 services because it maps naturally to the request/response semantics of each pipeline step, requires no additional tooling beyond `curl`, and produces JSON that is easy to inspect and debug.

**SOAP** is used for identity verification because the operation requires a legally defensible, schema-validated audit trail. The XSD in the WSDL enforces the exact fields accepted and returned; any deviation causes a binding-level fault before business logic is reached.

**gRPC** is used for fraud detection because: (a) the service will run CPU-intensive scoring; (b) Protobuf binary encoding is 3–10x smaller than equivalent JSON for numeric payloads; (c) the `StreamRiskUpdates` RPC demonstrates how server-streaming over HTTP/2 eliminates polling for long-running computations.

**GraphQL** is used for document-review and claim-tracking because both services expose graphs of nested objects (a claim has many documents; a claim has a history of many status entries). GraphQL lets Bonita or a front-end request exactly the fields needed without separate REST endpoints for different projection needs.

---

## 14. Troubleshooting

### Service Fails to Start — Port Already in Use

```bash
# Find which process is using a port (example: 8081)
lsof -i :8081
# Kill it
kill -9 <PID>
```

### fraud-detection Fails to Build — protoc Not Found

The `fraud-detection` build downloads `protoc` automatically using the `os-maven-plugin`. If you are behind a corporate proxy:

```bash
mvn clean package -DskipTests \
  -Dhttps.proxyHost=your.proxy.host \
  -Dhttps.proxyPort=8080
```

Alternatively, install `protoc` manually and ensure it is on `$PATH`.

### gRPC Connection Refused

The fraud-detection service is a standalone Java process (not Spring Boot). It must be started separately if you are not using Docker Compose:

```bash
java -jar services/fraud-detection/target/fraud-detection.jar
```

Confirm it is listening:
```bash
grpcurl -plaintext localhost:9090 list
```

### SOAP Returns HTTP 500 — Namespace Mismatch

Ensure the `xmlns:tns` attribute in your request body exactly matches the WSDL namespace:
```
xmlns:tns="http://insurance.com/identity"
```
Any variation (trailing slash, different case, different URI) causes a dispatch failure.

### GraphQL Returns `null` Data with No Errors

This typically means the requested `claimId` does not exist in the in-memory repository. The services use in-memory stores that are reset on restart. Re-run the initialization mutation first:

```bash
curl -X POST http://localhost:8090/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "mutation { initializeClaim(claimId: \"CLM-001\") { claimId currentStatus } }"}'
```

### Compensation Returns HTTP 400 — Deductible Exceeds Approved Amount

The compensation service enforces `deductible <= approvedAmount`. Reduce the deductible or use the exact approved amount from the expert-assessment response.

### Docker Compose Build Fails — Out of Memory During protoc Code Generation

The fraud-detection service is memory-intensive during the Maven protoc phase. Increase Docker's memory allocation to at least 4 GB in Docker Desktop settings.

### Spring Boot Services Return 404 on All Endpoints

Verify the `Content-Type: application/json` header is present on POST requests. Spring MVC returns 415 (Unsupported Media Type) — not 404 — without it, but misconfigured reverse proxies may translate this. Run `curl -v` to see raw status codes.

### Checking Service Logs

```bash
# Docker Compose
docker-compose logs claim-submission
docker-compose logs fraud-detection
docker-compose logs document-review

# Individual service log level can be adjusted via application.properties:
# logging.level.com.insurance=DEBUG
```

---

## References

- Spring Boot 3.2 Documentation: https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/
- Spring Web Services (Spring-WS): https://docs.spring.io/spring-ws/docs/current/reference/
- Spring for GraphQL: https://docs.spring.io/spring-graphql/docs/current/reference/html/
- gRPC Java Documentation: https://grpc.io/docs/languages/java/
- Protocol Buffers Language Guide (proto3): https://protobuf.dev/programming-guides/proto3/
- GraphQL Specification: https://spec.graphql.org/
- WSDL 1.1 Specification: https://www.w3.org/TR/wsdl
- OpenAPI 3.1 Specification: https://spec.openapis.org/oas/v3.1.0
- Bonita BPM Community: https://documentation.bonitasoft.com/
- grpcurl: https://github.com/fullstorydev/grpcurl
