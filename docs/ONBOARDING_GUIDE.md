# AgentGate — Developer & Classmate Onboarding Guide

## Introduction
AgentGate is a Zero-Trust Governance Gateway for AI Agent tool calls. It intercepts tool calls over standard Model Context Protocol (MCP) and REST, enforcing strict identity verification, input schema validation, deterministic policy rules, rate limits, and cryptographic append-only audit chaining.

---

## 5-Minute Quickstart

### Prerequisites
- Java 21+
- Apache Maven 3.9+ (or use included `./mvnw`)
- SQLite (built-in zero-setup file database) OR Docker (optional for PostgreSQL & Redis)

### Step 1: Start AgentGate in Zero-Setup SQLite Mode
For local evaluation, coursework, and zero manual setup:
```bash
# Clone the repository
git clone https://github.com/mitulkabirbadhon7/AgentGate-.git
cd AgentGate-

# Launch application with SQLite profile (automated, zero external daemon needed)
./mvnw spring-boot:run -Dspring-boot.run.profiles=sqlite
```
The gateway automatically creates and configures the SQLite database file at `./data/agentgate.db`.

Access Points:
- Gateway Base: `http://localhost:8080`
- Admin Dashboard: `http://localhost:8080/admin` (or `/dashboard`)
  - Default Admin Credentials: `admin` / `admin_gate_change_me`
- MCP Gateway Endpoint: `http://localhost:8080/mcp`
- Health Check: `http://localhost:8080/actuator/health`

Alternatively, to run with PostgreSQL & Redis via Docker:
```bash
docker compose up -d
./mvnw spring-boot:run
```

---

## Onboarding a New REST Tool in Under 10 Minutes

### 1. Register Downstream Target Server
```bash
POST /api/v1/servers
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>

{
  "name": "billing-service",
  "type": "REST",
  "endpointUrl": "https://api.example.com/v1",
  "timeoutSeconds": 15,
  "circuitOpenThreshold": 5
}
```

### 2. Catalog Your Tool
```bash
POST /api/v1/tools
Content-Type: application/json
Authorization: Bearer <ADMIN_TOKEN>

{
  "serverId": 1,
  "name": "refund_customer",
  "description": "Process a customer refund",
  "inputSchema": "{\"type\":\"object\",\"properties\":{\"amount\":{\"type\":\"number\"},\"reason\":{\"type\":\"string\"}},\"required\":[\"amount\"]}",
  "riskLevel": "HIGH",
  "routingHint": "POST /refunds"
}
```

### 3. Bind a Governance Policy
Create a policy requiring human review for refunds over $500:
```json
{
  "rules": [
    {
      "id": "rule-refund-auto",
      "effect": "ALLOW",
      "condition": { "field": "arguments.amount", "op": "LTE", "value": 500 },
      "reason": "Standard refund auto-approved"
    },
    {
      "id": "rule-refund-review",
      "effect": "REQUIRE_APPROVAL",
      "condition": { "field": "arguments.amount", "op": "GT", "value": 500 },
      "reason": "Large refund requires SecOps review"
    }
  ]
}
```

### 4. Connect Your AI Agent via MCP
Point your AI agent's MCP client configuration to `http://localhost:8080/mcp`.
The agent will discover available tools via `tools/list` and dispatch calls via `tools/call` safely through AgentGate's zero-trust governance pipeline.
