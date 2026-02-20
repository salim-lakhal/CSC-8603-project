# Bonita BPM Setup Guide — Insurance Claim Processing Workflow

**Target version:** BonitaCommunity-2021.2-u0
**Course:** TELECOM SudParis — Service-Oriented Communication Technologies

---

## 1. Installation

### Download

Download **BonitaCommunity-2021.2-u0** from the official Bonita website:
https://www.bonitasoft.com/downloads

Select the installer matching your operating system.

### Linux

Run the installer from a terminal:

```bash
chmod +x BonitaBPMCommunity64-linux
./BonitaBPMCommunity64-linux
```

Follow the graphical installation wizard. Accept the default workspace location or
specify a custom path. The installer bundles an embedded Tomcat server and an
embedded H2 database — no separate database setup is required for development.

### Windows

Run `BonitaBPMCommunity64.exe` as administrator. Follow the installation wizard.
After installation, launch **Bonita Studio** from the Start Menu or the desktop
shortcut.

### Known Issue: Fedora / RHEL-based Linux

Bonita Studio uses an embedded browser widget (SWT) that may fail to render on
Fedora due to missing GTK/WebKit libraries. If the Studio UI shows a blank panel
or crashes on startup, open the `.ini` file located in the Bonita Studio
installation directory (e.g., `BonitaStudioCommunity64.ini`) and add the
following JVM argument on its own line:

```
-Dorg.eclipse.SWT.browser.Default=mozilla
```

Save the file and restart Bonita Studio.

---

## 2. Import the BPMN

Two methods are available to load the `insurance_claim.bpmn` process into
Bonita Studio.

**Method A — File menu:**

1. Open Bonita Studio.
2. Go to **File → Open**.
3. In the file chooser dialog, navigate to the `workflow/` directory.
4. Select `insurance_claim.bpmn` and click **Open**.
5. Bonita Studio will parse the file, render both pools on the canvas, and
   populate the process configuration panel on the right.

**Method B — Drag and drop:**

1. Open Bonita Studio so the canvas is visible.
2. Open your file manager, navigate to the `workflow/` directory.
3. Drag `insurance_claim.bpmn` and drop it directly onto the Bonita Studio
   canvas.
4. Bonita Studio will import the process immediately.

After import, both pools ("Customer Process" and "Insurance Processing") should
be visible on the canvas connected by dashed message flow arrows.

---

## 3. Process Architecture — Two-Pool Design

The workflow uses a **collaboration** with two separate pools. This mirrors the
real-world separation between the customer-facing interface and the back-end
insurance processing engine.

### Customer Pool — "Customer Process"

Responsibilities:
- Initiates the insurance claim by presenting a form to the customer.
- Waits for the final decision from the insurance back end.
- Displays the outcome to the customer.

Flow summary:
```
[Start Event: Submit Claim Request]
    → [User Task: Fill Claim Form]
    → [Intermediate Throw Message: Send Claim]
    → [Intermediate Catch Message: Await Decision]
    → [End Event: Claim Processed]
```

### Insurance Processing Pool — "Insurance Processing"

Responsibilities:
- Receives the claim submitted by the customer.
- Orchestrates all verification, fraud detection, eligibility, document review,
  expert assessment, compensation calculation, payment authorization, and
  notification steps.
- Sends the final result back to the Customer Pool.

Flow summary:
```
[Start Message Event: Receive Claim]
    → [Service Task: Verify Identity]      ← SOAP connector
    → [XOR Gateway: Identity Valid?]
         Yes → [Service Task: Validate Policy]    ← REST connector
              → [Service Task: Detect Fraud]       ← REST connector (gRPC proxy)
              → [Service Task: Check Eligibility]  ← REST connector
              → [XOR Gateway: Eligible?]
                   Yes → [Service Task: Review Documents]      ← GraphQL service task
                        → [Service Task: Expert Assessment]    ← REST connector
                        → [Service Task: Calculate Compensation] ← REST connector
                        → [Service Task: Authorize Payment]    ← REST connector
                        → [Service Task: Send Notification]    ← REST connector
                        → [Intermediate Throw Message: Send Result]
                        → [End Event: Claim Completed]
                   No  → [Error End Event: Claim Rejected]
         No  → [Error End Event: Claim Rejected]
```

### Message Flow Between Pools

Two BPMN 2.0 messages coordinate the two pools:

| Message Name              | Direction              | Payload                                                   |
|---------------------------|------------------------|-----------------------------------------------------------|
| `claim_request_message`   | Customer → Insurance   | policyNumber, claimantName, estimatedAmount, claimType    |
| `claim_result_message`    | Insurance → Customer   | claimId, approvedAmount, authorizationCode, status        |

The dashed arrows on the Bonita canvas represent these message flows. They are
not sequence flows — they cross pool boundaries and are triggered by the throw
events, then caught by the corresponding catch events.

---

## 4. Process Variables

All variables below are defined at the **pool level** of the
**Insurance Processing Pool**. To view or edit them in Bonita Studio:

1. Click on an empty area inside the "Insurance Processing" pool (not on any
   element).
2. In the bottom panel, select the **Data** tab.
3. Click **Add** to create each variable.

| Variable Name        | Type    | Description                                      |
|----------------------|---------|--------------------------------------------------|
| `claimId`            | Text    | Unique identifier assigned by the claim service  |
| `policyNumber`       | Text    | Policy number submitted by the customer          |
| `claimantName`       | Text    | Full name of the claimant                        |
| `estimatedAmount`    | Double  | Estimated claim amount submitted by customer     |
| `claimType`          | Text    | Type of claim (e.g., AUTO, HOME, HEALTH)         |
| `verificationStatus` | Text    | Result of SOAP identity verification             |
| `fraudRiskLevel`     | Text    | Fraud risk score returned by fraud-detection     |
| `isEligible`         | Boolean | Whether the claimant is eligible for coverage    |
| `documentStatus`     | Text    | Status returned by document review (GraphQL)     |
| `approvedAmount`     | Double  | Amount approved after expert assessment          |
| `authorizationCode`  | Text    | Payment authorization code                       |
| `notificationStatus` | Text    | Status of the notification dispatch              |

For the **Customer Process** pool, define the following input variables (used
in the form on the "Fill Claim Form" user task):

| Variable Name     | Type   |
|-------------------|--------|
| `policyNumber`    | Text   |
| `claimantName`    | Text   |
| `estimatedAmount` | Double |
| `claimType`       | Text   |

---

## 5. Actor Mapping

Bonita requires every User Task and every pool that contains human steps to have
an **actor** defined and mapped to real users or roles in Bonita Portal.

### Customer Pool — "customer" actor

1. Click the "Fill Claim Form" user task on the canvas.
2. In the bottom panel, select the **General** tab → **Actors** section.
3. The actor name should be `customer`. If not, create it.
4. In Bonita Portal (http://localhost:8080/bonita), go to
   **Organization → Roles** and ensure a role named **User** exists.
5. Go to **Configuration → Actor mapping** for the Insurance Claim process.
6. Map the `customer` actor to the **User** role (or to a specific user for
   demo purposes).

### Insurance Processing Pool — "insurance_agent" actor

1. Click the "Insurance Processing" pool background.
2. In the **Actors** tab, confirm the actor is named `insurance_agent`.
3. In Bonita Portal, ensure a role named **Insurance** exists under
   **Organization → Roles**.
4. In **Configuration → Actor mapping**, map `insurance_agent` to the
   **Insurance** role.

For a quick demo with a single user (admin), you may map both actors to the
`admin` user directly. Go to **Actor mapping → Users** and add `admin` to
both actors.

---

## 6. Installing Connectors from Bonita Marketplace

The workflow requires two connectors that are not bundled with Bonita Studio by
default: the **REST connector** and the **Webservice SOAP 1.2** connector.
Both are available free of charge on the Bonita Marketplace.

### Steps (same for both connectors)

1. In Bonita Studio, go to the menu bar: **Help → Bonita Marketplace**.
2. The Marketplace browser opens inside Bonita Studio.
3. In the search box, type **REST** and press Enter.
4. Locate **"REST Connector"** in the results list.
5. Click **Install**. Bonita Studio will download and register the connector.
6. Repeat: search for **Web Service** (or **SOAP**).
7. Locate **"Webservice SOAP 1.2"** in the results.
8. Click **Install**.
9. Restart Bonita Studio when prompted.

After restarting, the connectors will appear in the connector palette when you
click a service task and go to **General → Connectors IN**.

---

## 7. Configuring REST Connectors

For each service task listed below, perform these steps:

1. Click the service task on the canvas.
2. In the bottom panel, select **General → Connectors IN**.
3. Click **Add**, then select **REST** from the connector type list.
4. Name the connector configuration as specified below.
5. Fill in the fields as described.
6. In the **Output** tab, map response fields to process variables using the
   output operation expressions described below.
7. Click **Finish**.

---

### claim-submission connector

**Service task:** "Submit Claim"
**Connector name:** `claim-submission`

| Field         | Value                                |
|---------------|--------------------------------------|
| Method        | POST                                 |
| URL           | `http://localhost:8081/claims`       |
| Content-Type  | `application/json`                   |

**Request body (Groovy script expression):**

```groovy
'{"policyNumber": "' + policyNumber + '", ' +
'"claimantName": "' + claimantName + '", ' +
'"estimatedAmount": ' + estimatedAmount + ', ' +
'"claimType": "' + claimType + '"}'
```

**Output operations:**

| Process variable | Expression                     |
|------------------|--------------------------------|
| `claimId`        | `bodyAsObject.claimId`         |
| `status`         | `bodyAsObject.status`          |

---

### policy-validation connector

**Service task:** "Validate Policy"
**Connector name:** `policy-validation`

| Field         | Value                                        |
|---------------|----------------------------------------------|
| Method        | POST                                         |
| URL           | `http://localhost:8083/policies/validate`    |
| Content-Type  | `application/json`                           |

**Request body:**

```groovy
'{"policyNumber": "' + policyNumber + '", ' +
'"claimantName": "' + claimantName + '"}'
```

**Output operations:**

| Process variable | Expression                          |
|------------------|-------------------------------------|
| `isEligible`     | `bodyAsObject.valid`                |
| (local)          | `bodyAsObject.coverageAmount`       |

---

### eligibility connector

**Service task:** "Check Eligibility"
**Connector name:** `eligibility`

| Field         | Value                                      |
|---------------|--------------------------------------------|
| Method        | POST                                       |
| URL           | `http://localhost:8084/eligibility/check`  |
| Content-Type  | `application/json`                         |

**Request body:**

```groovy
'{"claimId": "' + claimId + '", ' +
'"policyNumber": "' + policyNumber + '", ' +
'"claimType": "' + claimType + '"}'
```

**Output operations:**

| Process variable | Expression               |
|------------------|--------------------------|
| `isEligible`     | `bodyAsObject.eligible`  |

---

### expert-assessment connector

**Service task:** "Expert Assessment"
**Connector name:** `expert-assessment`

| Field         | Value                              |
|---------------|------------------------------------|
| Method        | POST                               |
| URL           | `http://localhost:8086/assessments`|
| Content-Type  | `application/json`                 |

**Request body:**

```groovy
'{"claimId": "' + claimId + '", ' +
'"estimatedAmount": ' + estimatedAmount + ', ' +
'"claimType": "' + claimType + '", ' +
'"fraudRiskLevel": "' + fraudRiskLevel + '"}'
```

**Output operations:**

| Process variable  | Expression                      |
|-------------------|---------------------------------|
| `approvedAmount`  | `bodyAsObject.approvedAmount`   |

---

### compensation connector

**Service task:** "Calculate Compensation"
**Connector name:** `compensation`

| Field         | Value                                          |
|---------------|------------------------------------------------|
| Method        | POST                                           |
| URL           | `http://localhost:8087/compensation/calculate` |
| Content-Type  | `application/json`                             |

**Request body:**

```groovy
'{"claimId": "' + claimId + '", ' +
'"approvedAmount": ' + approvedAmount + ', ' +
'"claimType": "' + claimType + '"}'
```

**Output operations:**

| Process variable | Expression                    |
|------------------|-------------------------------|
| (local)          | `bodyAsObject.totalPayment`   |

---

### payment-authorization connector

**Service task:** "Authorize Payment"
**Connector name:** `payment-authorization`

| Field         | Value                                    |
|---------------|------------------------------------------|
| Method        | POST                                     |
| URL           | `http://localhost:8088/payments/authorize` |
| Content-Type  | `application/json`                       |

**Request body:**

```groovy
'{"claimId": "' + claimId + '", ' +
'"approvedAmount": ' + approvedAmount + ', ' +
'"policyNumber": "' + policyNumber + '"}'
```

**Output operations:**

| Process variable    | Expression                        |
|---------------------|-----------------------------------|
| `authorizationCode` | `bodyAsObject.authorizationCode`  |

---

### notification connector

**Service task:** "Send Notification"
**Connector name:** `notification`

| Field         | Value                                    |
|---------------|------------------------------------------|
| Method        | POST                                     |
| URL           | `http://localhost:8089/notifications/send` |
| Content-Type  | `application/json`                       |

**Request body:**

```groovy
'{"claimId": "' + claimId + '", ' +
'"claimantName": "' + claimantName + '", ' +
'"approvedAmount": ' + approvedAmount + ', ' +
'"authorizationCode": "' + authorizationCode + '"}'
```

**Output operations:**

| Process variable     | Expression              |
|----------------------|-------------------------|
| `notificationStatus` | `bodyAsObject.status`   |

---

## 8. Configuring the SOAP Connector (Identity Verification)

The "Verify Identity" service task uses a SOAP/WSDL connector to call a web
service that runs at `http://localhost:8082`.

### Steps

1. Click the **"Verify Identity"** service task on the canvas.
2. In the bottom panel: **General → Connectors IN → Add**.
3. In the connector type list, select **"Webservice SOAP 1.2"**
   (installed from Marketplace in section 6).
4. Name the connector `identity-verification`.

### WSDL Introspection

5. In the **WSDL URL** field, enter:
   ```
   http://localhost:8082/ws/identity.wsdl
   ```
6. Click the **"Introspect"** button. Bonita Studio will fetch and parse the
   WSDL, populating the service, port, and operation dropdowns automatically.
7. From the **Operation** dropdown, select `verifyIdentity`.

### Input Parameter Mapping

8. In the **Input parameters** table, map the SOAP request fields:

   | SOAP parameter  | Process variable expression |
   |-----------------|-----------------------------|
   | `policyNumber`  | `policyNumber`              |
   | `claimantName`  | `claimantName`              |

### Output Parsing (Groovy Script)

The SOAP response is a DOM Document. Use the following Groovy script to extract
the verification status from the XML response body.

9. In the **Output operations** section, click **Add**.
10. Set the target process variable to `verificationStatus`.
11. For the expression, select **Script (Groovy)** and paste the following:

```groovy
import org.w3c.dom.*;

responseDocumentBody.normalizeDocument();

NodeList resultList = responseDocumentBody.getElementsByTagNameNS(
    "*",
    "verificationStatus"
);

Element el = (Element) resultList.item(0);

return el.getTextContent();
```

12. Click **Finish**.

**Note:** The variable `responseDocumentBody` is the standard output variable
provided by the "Webservice SOAP 1.2" connector in Bonita. It is an
`org.w3c.dom.Document` instance containing the SOAP response body.

---

## 9. XOR Gateway Configuration

### Gateway 1: "Identity Valid?" (after "Verify Identity")

This gateway routes the flow based on the result of the SOAP identity check.

**To configure in Bonita Studio:**

1. Click the sequence flow arrow that leads from "Identity Valid?" toward
   "Validate Policy".
2. In the bottom panel, open the **General** tab.
3. Under **Condition**, enter the following expression:

   ```groovy
   verificationStatus == "VERIFIED"
   ```

4. Click the sequence flow arrow that leads from "Identity Valid?" toward
   "Claim Rejected" (the error end event).
5. Check the **Default flow** checkbox (this path is taken if no other
   condition is true).

**Routing logic:**

| Condition                              | Target                        |
|----------------------------------------|-------------------------------|
| `verificationStatus == "VERIFIED"`     | Validate Policy (service task)|
| Default (all other values)             | Claim Rejected (error end)    |

---

### Gateway 2: "Eligible?" (after "Check Eligibility")

This gateway routes the flow based on the eligibility check result.

1. Click the sequence flow from "Eligible?" toward "Review Documents".
2. Set the condition:

   ```groovy
   isEligible == true
   ```

3. Click the sequence flow from "Eligible?" toward "Claim Rejected".
4. Check the **Default flow** checkbox.

**Routing logic:**

| Condition          | Target                          |
|--------------------|---------------------------------|
| `isEligible == true` | Review Documents (service task)|
| Default            | Claim Rejected (error end)      |

---

## 10. Running the Workflow

### Step 1: Start all backend services

The workflow depends on 11 Java microservices. Start them all before running
the process. Using Docker Compose (from the project root):

```bash
docker-compose up --build
```

Or start each service individually:

| Port  | Service                  |
|-------|--------------------------|
| 8081  | claim-submission-service |
| 8082  | identity-verification-service (SOAP/WSDL) |
| 8083  | policy-validation-service |
| 8084  | eligibility-service      |
| 8085  | fraud-detection-service (gRPC) |
| 8086  | expert-assessment-service|
| 8087  | compensation-service     |
| 8088  | payment-authorization-service |
| 8089  | notification-service     |
| 8090  | document-review-service (GraphQL) |

Verify each service is healthy before starting Bonita:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
# etc.
```

### Step 2: Start Bonita embedded server

In Bonita Studio, click the **Run** button (the green play triangle in the
toolbar). Bonita Studio will:
- Build and deploy the process to the embedded Tomcat server.
- Start the embedded server on port 8080.
- Open a browser tab pointing to Bonita Portal.

Wait for the console at the bottom of Bonita Studio to show:
```
Server startup in XXXX ms
```

### Step 3: Open Bonita Portal

Navigate to:
```
http://localhost:8080/bonita
```

### Step 4: Log in

Use the default administrator credentials:

| Field    | Value   |
|----------|---------|
| Username | `admin` |
| Password | `admin` |

### Step 5: Start a new case

1. In the Bonita Portal home screen, locate the **Insurance Claim** process.
2. Click **"Start a new case"**.
3. The form for the "Fill Claim Form" user task will open.

### Step 6: Fill in the claim form

Enter test data in the form fields:

| Field             | Example value          |
|-------------------|------------------------|
| Policy Number     | `POL-2024-001`         |
| Claimant Name     | `Jean Dupont`          |
| Estimated Amount  | `5000.00`              |
| Claim Type        | `AUTO`                 |

Click **Submit**.

### Step 7: Follow tasks in the inbox

1. After submitting the form, Bonita sends the `claim_request_message` to the
   Insurance Processing pool, triggering the back-end workflow automatically.
2. All service tasks execute automatically via their connectors.
3. When the Insurance pool completes, it sends `claim_result_message` back to
   the Customer pool.
4. The "Await Decision" catch event in the Customer pool receives the message.
5. The process ends at "Claim Processed".

To monitor task execution, go to:
- **Bonita Portal → Processes → Insurance Claim → Cases**

To view connector execution logs, check the Bonita Studio console at the
bottom of the IDE.

---

## Troubleshooting

| Symptom | Solution |
|---------|----------|
| SOAP connector cannot fetch WSDL | Ensure identity-verification-service is running on port 8082 before introspecting |
| REST connector returns 500 | Check service logs; verify request body JSON syntax |
| XOR gateway takes wrong path | Verify variable spelling matches exactly (case-sensitive) |
| Bonita Portal shows blank page | Add `-Dorg.eclipse.SWT.browser.Default=mozilla` to .ini (Fedora) |
| "Actor not mapped" error | Complete actor mapping for both pools in Bonita Portal configuration |
| Message not received by Insurance pool | Verify message names match exactly in throw and catch events |
