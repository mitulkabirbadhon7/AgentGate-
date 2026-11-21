# 📄 Product Requirements Document (PRD) — AgentGate

---

## 1. Document Control & Metadata
- **Product Name**: AgentGate
- **Project Lead**: Mitul Kabir Badhon
- **Course**: CSE 444 — Software Architecture and Design Patterns
- **Version**: 1.0.0
- **Status**: APPROVED / ACTIVE
- **Target Release**: Production Single-Tenant Governance Gateway

---

## 2. Executive Summary & Vision

Autonomous AI agents (powered by leading large language models and open-source models) increasingly possess the autonomy to invoke tools, interact with databases, trigger financial transactions, and modify production records via protocols such as the **Model Context Protocol (MCP)** and REST APIs.

Without runtime governance, deploying autonomous agents exposes organizations to catastrophic risks:
1. **Prompt Injection & Tool Hijacking**: Malicious users manipulating agent prompts to invoke unauthorized tools.
2. **Denial-of-Wallet & Cascade Spends**: Runaway loops exhausting third-party API budgets and internal resource limits.
3. **Unsanitized Data Leakage**: Sensitive personally identifiable information (PII, National IDs, phone numbers) leaking to external LLM providers.
4. **Untracked / Repudiated Actions**: Inability to reconstruct who authorized a destructive agent action.

**AgentGate** is a zero-trust, inline security and governance gateway that mediates all tool calls between external AI agents and sensitive backend execution targets. It provides real-time schema validation, declarative version-pinned policies, atomic spend quotas, human-in-the-loop approvals, emergency kill switches, and cryptographically chained tamper-evident audit logs.

---

## 3. Target Personas

| Persona | Role | Core Objectives & Pain Points |
|---|---|---|
| **Alex (AI Security Engineer)** | Enterprise SecOps | Needs to ensure AI agents cannot exceed their blast radius, steal credentials, or leak sensitive PII. Needs instant emergency kill switches and tamper-evident audit logs. |
| **Samantha (Platform Architect)** | Core Infrastructure | Demands sub-30ms p95 gateway overhead, high reliability, zero dual-write inconsistencies, and circuit breakers that protect backend services from agent loops. |
| **David (Compliance & Risk Officer)** | Regulatory Compliance | Requires non-repudiable audit trails of every tool execution, guaranteed human sign-off on financial or high-risk writes, and proof that data was not altered retroactively. |
| **Priya (AI Application Developer)** | Product Engineer | Wants to onboard new internal REST and MCP tools in under 10 minutes without writing custom governance boilerplate or complex middleware. |

---

## 4. Scope & Boundary

### 4.1 In-Scope
- Inbound **Model Context Protocol (MCP)** tool discovery and tool execution gateway (`/mcp`).
- Inbound **Admin REST API** for governance control (`/api/v1/*`).
- OAuth2 Bearer token authentication and trusted on-behalf-of end-user context injection (`ContextSnapshot`).
- 6-Stage pluggable **Governance Pipeline**:
  1. Strict JSON Schema argument validation and normalization.
  2. Sub-10ms emergency kill switches (Global Write, Agent-level, Tool-level).
  3. Declarative, version-pinned policy evaluations with default-deny semantics.
  4. Atomic sliding-window rate limits and UTC daily spend budget quotas.
  5. Human-in-the-loop approvals with argument modification revalidation.
  6. Two-way PII redaction (email, Bangladesh phone numbers, National IDs) and payload truncation.
- Cryptographically chained, append-only audit trail serialized via database advisory locks.
- Transactional outbox pattern for reliable asynchronous event publication.
- Human-crafted, responsive administration dashboard built with Thymeleaf.

### 4.2 Out-of-Scope
- Training or hosting generative LLM models (agents are external clients).
- Multi-tenant enterprise tenant isolation (initial scope is single-tenant modular monolith).
- Direct end-user chat interfaces (AgentGate governs machine-to-machine tool calls).

---

## 5. Functional Requirements (FR)

### FR1: Tool Catalog & Model Context Protocol Discovery
- **FR1.1**: The system shall expose a unified MCP endpoint (`/mcp`) supporting JSON-RPC 2.0 over Streamable HTTP and stdio.
- **FR1.2**: Agents shall only discover tools that are registered, enabled, healthy, and explicitly permitted by their assigned policies.
- **FR1.3**: Downstream targets can be native MCP servers or REST APIs imported via OpenAPI 3.0 specifications.
- **FR1.4**: OpenAPI ingestion must enforce strict SSRF defenses, rejecting private IP ranges (RFC 1918), loopback (`127.0.0.1`), and AWS/GCP link-local metadata addresses (`169.254.169.254`).

### FR2: Identity, OAuth2 & On-Behalf-Of Trust
- **FR2.1**: All inbound agent requests must be authenticated via an OAuth2 Resource Server validating JWT signatures, issuers, audiences, and scopes.
- **FR2.2**: The agent identity must be derived exclusively from verified cryptographic token claims, never from client-supplied HTTP headers or JSON bodies.
- **FR2.3**: If an agent acts on behalf of a human end-user, the user identity must be extracted from verified token claims (`_meta`) and mapped to an immutable, thread-safe `ContextSnapshot`.

### FR3: Declarative Governance Policies & Versioning
- **FR3.1**: The system must enforce **default-deny**: if no applicable policy explicitly allows an invocation, the request is blocked.
- **FR3.2**: Published policies (`POLICY_VERSION`) must be strictly **immutable**. Subsequent updates create a new version with an incremented version number and SHA-256 content hash.
- **FR3.3**: Every tool invocation must pin the exact policy version evaluated to ensure 100% deterministic post-incident auditability.
- **FR3.4**: Policy bindings support 4 scopes with explicit priority ordering: `GLOBAL` (lowest priority) < `AGENT` < `TOOL` < `AGENT_TOOL` (highest priority).
- **FR3.5**: Policy changes must propagate across all gateway instances within 10 seconds via Redis Pub/Sub.

### FR4: Human-in-the-Loop Approval State Machine
- **FR4.1**: Invocations designated by policy as high-risk or requiring human sign-off must be paused in state `PENDING_APPROVAL`.
- **FR4.2**: The gateway must persist an `APPROVAL_REQUEST` cryptographically bound to the SHA-256 digest of the request arguments (`arguments_hash`).
- **FR4.3**: Operators can review, reject, approve, or **edit arguments** via the Admin UI.
- **FR4.4**: If arguments are edited, the system must force a mandatory revalidation through Schema, Policy, Kill Switch, and Quota guards.
- **FR4.5**: Requests exceeding a configurable timeout must transition to `EXPIRED` and fail closed.

### FR5: Schema Validation, PII Redaction & Output Truncation
- **FR5.1**: Request arguments must strictly conform to the tool's registered JSON Schema (Draft 7 / 2020-12). Type coercion is permitted only for safe standard types.
- **FR5.2**: The gateway must sanitize both inbound arguments and outbound responses for PII:
  - Email addresses (masked to `u***@d***.com`).
  - Bangladesh phone numbers (`+8801XXXXXXXXX` masked to `+8801*****XXXX`).
  - National ID numbers (NID 10/13/17 digits masked to `NID-*****XXXX`).
- **FR5.3**: Downstream payloads exceeding size limits (e.g. 100KB) must be truncated, appending an explicit machine-readable notice: `[TRUNCATED: Response exceeded maximum allowed size]`.

### FR6: Atomic Spend Quotas & Sliding-Window Rate Limits
- **FR6.1**: The system must enforce high-frequency rate limits (e.g., requests per 60s) per agent and per tool.
- **FR6.2**: The system must track daily cumulative spend/token budgets reset strictly at `00:00:00 UTC`.
- **FR6.3**: Quota checks and increments must execute atomically via Redis Lua scripts synced to `Redis TIME` to eliminate race conditions.
- **FR6.4**: Breaching a rate or quota limit must immediately emit a `SECURITY_ALERT` and return a standard `RATE_LIMIT_EXCEEDED` error code.

### FR7: Tamper-Evident Cryptographic Audit Logging
- **FR7.1**: Every lifecycle transition (`RECEIVED`, `VALIDATING`, `APPROVED`, `EXECUTING`, `COMPLETED`, `BLOCKED`, `REJECTED`) must generate an append-only `AUDIT_EVENT`.
- **FR7.2**: Audit events form an unbroken SHA-256 hash chain:
  $$\text{curr\_hash} = \text{SHA256}(\text{prev\_hash} + \text{sequence\_number} + \text{canonical\_payload})$$
- **FR7.3**: Concurrent appends must be serialized via PostgreSQL transaction-level advisory locks.
- **FR7.4**: Database permissions must strictly prohibit `UPDATE` and `DELETE` on the audit table.
- **FR7.5**: The Admin UI must provide one-click audit verification that validates the cryptographic integrity of the entire chain.

### FR8: Multi-Scope Emergency Kill Switches
- **FR8.1**: Administrators must be able to instantly trigger an emergency stop across three granular scopes:
  - `GLOBAL_WRITES`: Halts all state-modifying tool calls across the entire gateway.
  - `AGENT`: Halts all tool calls from a specific compromised agent.
  - `TOOL`: Halts invocations of a specific misbehaving downstream tool.
- **FR8.2**: Kill switch state must be mirrored in Redis to allow sub-10ms evaluation.
- **FR8.3**: Tripping a kill switch must immediately cancel all matching pending approvals.

### FR9: Policy Playground & Dry-Run Simulator
- **FR9.1**: Administrators must be able to test prospective policy rules against historical or synthetic tool calls without affecting live traffic.
- **FR9.2**: Dry-run invocations must log the would-be decision (`ALLOW`, `DENY`, `REQUIRE_APPROVAL`) without dispatching downstream side effects.

### FR10: Downstream Health Management & Fault Isolation
- **FR10.1**: Each downstream target must be protected by a Resilience4j circuit breaker and bounded execution timeout.
- **FR10.2**: If consecutive downstream failures exceed the configured threshold, the circuit breaker opens, immediately fast-failing subsequent calls.
- **FR10.3**: Unhealthy or circuit-open tools must be automatically hidden from agent MCP tool discovery.
- **FR10.4**: If a downstream write times out, it must be marked `UNKNOWN_OUTCOME` and never blindly retried.

---

## 6. Non-Functional Quality Attributes (QA)

| ID | Quality Attribute | Target Metric & Acceptance Criteria |
|---|---|---|
| **QA1** | **Security & Detection** | In a test harness of 30 malicious tool injection calls and 200 legitimate calls: 100% of malicious calls must be blocked or held; false-positive blocks among legitimate calls must be $\le 5\%$. |
| **QA2** | **Performance & Latency** | Gateway overhead (excluding approval wait and downstream network latency) must achieve $\le 30\text{ ms}$ p95 at $100\text{ calls/second}$. |
| **QA3** | **Tamper Evidence** | Any manual tampering, deletion, or insertion in the audit log must be cryptographically detected by the verification engine in $\le 60\text{ seconds}$. |
| **QA4** | **Availability & Fail-Safe** | During a policy-store outage: medium and high-risk tool calls must fail closed immediately; low-risk tool calls may use last-known cached policy for at most 5 minutes. |
| **QA5** | **Developer Usability** | A developer following the runbook must successfully onboard and govern a new REST API tool in $\le 10\text{ minutes}$ without code modifications. |

---

## 7. User Interface Design & Aesthetics

The administration portal is designed to feel **deliberate, professional, and human-crafted**, deliberately avoiding generic "AI template" styles:
- **Design Philosophy**: Minimalist, content-first layout with generous whitespace, crisp typographic contrast, and zero visual clutter.
- **Color Palette**:
  - Background: Deep slate (`#0b0f19`) and dark navy canvas (`#0f172a`).
  - Containers & Cards: Layered slate (`#1e293b`) with subtle borders (`#334155`).
  - Primary Accents: Electric Indigo (`#6366f1`) and Cyan (`#06b6d4`).
  - Status Indicators: Emerald (`#10b981`) for passing checks, Amber (`#f59e0b`) for pending review, Rose (`#f43f5e`) for emergency stops and blocks.
- **Animations & Micro-interactions**: Smooth 150ms ease-out transitions on hover, animated pill badges for real-time status changes, and non-intrusive toast notifications powered by Server-Sent Events (SSE).
- **Usability Focus**: One-click quick actions for approvers (Approve, Edit, Reject), inline JSON diffs for argument modifications, and visual cryptographic hash chain status.
