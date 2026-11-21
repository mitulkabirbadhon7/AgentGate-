# AgentGate — Operational Runbook

## Emergency Procedures

### 1. Activating Global Writes Kill Switch
If an agent compromises credentials or begins abnormal mutation requests:
1. Navigate to **Emergency Kill Switches** in the Admin Console (`/killswitches`).
2. Select Scope: `GLOBAL_WRITES`.
3. Provide an incident reason (e.g. `Incident INC-402: Anomalous write activity detected`).
4. Click **Activate Switch**.
5. *API Alternative*:
   - *PowerShell*:
     ```powershell
     $headers = @{ "Authorization" = "Bearer <ADMIN_TOKEN>"; "Content-Type" = "application/json" }
     $body = '{"scope":"GLOBAL_WRITES","reason":"Emergency containment","activatedBy":"secops"}'
     Invoke-RestMethod -Uri "http://localhost:8080/api/v1/killswitches" -Method Post -Headers $headers -Body $body
     ```
   - *Bash / curl.exe*:
     ```bash
     curl.exe -X POST http://localhost:8080/api/v1/killswitches \
       -H "Authorization: Bearer <ADMIN_TOKEN>" \
       -H "Content-Type: application/json" \
       -d '{"scope":"GLOBAL_WRITES","reason":"Emergency containment","activatedBy":"secops"}'
     ```
*Effect*: All state-changing (POST, PUT, DELETE, PATCH) tool invocations across all agents are blocked within <1 millisecond.

### 2. Investigating an Audit Chain Tampering Incident
1. Open the **Audit Chain** console (`/audit`).
2. Click **Verify Hash Chain Integrity** (`/audit?verify=true`).
3. If tampering is reported:
   - Identify the failing sequence number.
   - Inspect the PostgreSQL table `audit_event` at `sequence_number`.
   - Cross-reference `payload_hash` against the `raw_payload`.
   - Quarantine the affected agent client immediately using an Agent-level kill switch.

### 3. Recovering From Circuit Breakers
- When downstream failure rate breaches the threshold, the tool enters `OPEN` circuit state and is hidden from MCP discovery (`tools/list`).
- Wait 10 seconds for the breaker to transition to `HALF_OPEN`.
- Once downstream recovers, the next successful probe invocation automatically resets the circuit to `CLOSED`.

---

## Health & Metrics Endpoints
- Gateway Health: `GET /actuator/health`
- Prometheus Metrics: `GET /actuator/prometheus`
- Live Admin Console: `GET /dashboard`
