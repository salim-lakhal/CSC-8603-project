# How to Test

## Project 1 — Code Examples

### Prerequisites
```bash
pip3 install flask requests zeep lxml grpcio grpcio-tools
cd project1/examples/graphql && npm install
```

### Run each example (each starts its own server automatically)
```bash
python3 project1/examples/soap/client.py      # SOAP
python3 project1/examples/rest/client.py      # REST
node project1/examples/graphql/client.js      # GraphQL
python3 project1/examples/grpc/client.py      # gRPC
```
Each script prints a series of requests/responses then exits cleanly.

---

## Project 2 — Insurance Claim System

### Prerequisites
- Java 21 + Maven 3.9
- Docker + Docker Compose

### Build all services
```bash
cd project2
for svc in services/*/; do
  mvn -f "$svc/pom.xml" clean package -DskipTests -q
done
```

### Start all 11 services
```bash
cd project2
docker compose up --build
```

### Quick smoke tests (run while services are up)

**REST**
```bash
curl -s -X POST http://localhost:8081/claims \
  -H "Content-Type: application/json" \
  -d '{"policyNumber":"POL-123456","claimantName":"Jean Dupont","incidentDate":"2024-01-15","description":"Car accident","estimatedAmount":8500,"claimType":"AUTO"}'
```

**SOAP**
```bash
curl http://localhost:8082/ws/identity.wsdl   # should return XML
```

**gRPC** (requires [grpcurl](https://github.com/fullstorydev/grpcurl/releases))
```bash
grpcurl -plaintext -d '{"claim_id":"CLM-001","policy_number":"POL-123456","estimated_amount":8500,"claim_type":"AUTO","incident_date":"2024-01-15","previous_claims_count":1}' \
  localhost:9090 com.insurance.fraud.FraudDetectionService/AssessFraudRisk
```

**GraphQL**
```bash
curl -s -X POST http://localhost:8085/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ getPendingDocuments { id claimId documentType status } }"}'
```

### Stop
```bash
docker compose down
```

---

## Verification (20/02/2026)

We cloned this repository from scratch on a clean machine to verify everything works end-to-end:

1. `git clone https://github.com/salim-lakhal/CSC-8603-project.git`
2. Installed Python/Node.js dependencies
3. Ran all 4 Project 1 examples (SOAP, REST, GraphQL, gRPC) — all passed
4. Built all 11 Project 2 Java services with `mvn clean package` — all 11 BUILD SUCCESS
5. Verified docker-compose configuration

All tests passed successfully on 20/02/2026.
