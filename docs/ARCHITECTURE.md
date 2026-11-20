# AgentGate — Architecture Specification

## Overview & Style
AgentGate is architected as a **Spring Boot 4.x Modular Monolith**. It serves as an inline zero-trust governance gateway for AI agent tool invocations, mediating between external AI clients and sensitive downstream systems (MCP servers and REST APIs). Microservice decomposition is prohibited without an accepted Architecture Decision Record (ADR).

---

## 🏛️ System Architecture Diagram

```mermaid
flowchart TD
    classDef agent fill:#312e81,stroke:#6366f1,stroke-width:1.5px,color:#e0e7ff,font-weight:bold;
    classDef security fill:#0c4a6e,stroke:#0284c7,stroke-width:1.5px,color:#bae6fd,font-weight:bold;
    classDef ingress fill:#1e3a8a,stroke:#3b82f6,stroke-width:1.5px,color:#bfdbfe,font-weight:bold;
    classDef domain fill:#111827,stroke:#4b5563,stroke-width:1.5px,color:#f3f4f6;
    classDef guard fill:#292524,stroke:#d97706,stroke-width:1.5px,color:#fef3c7;
    classDef infra fill:#0f172a,stroke:#64748b,stroke-width:1.5px,color:#f8fafc;
    classDef downstream fill:#064e3b,stroke:#10b981,stroke-width:1.5px,color:#a7f3d0,font-weight:bold;
    classDef frontend fill:#4c0519,stroke:#e11d48,stroke-width:1.5px,color:#ffe4e6,font-weight:bold;

    subgraph ExternalAgents ["🤖 AI Agents (External Consumers)"]
        RedTeam["Red-Team Client<br/>(Security Probe)"]:::agent
        SalesAgent["Sales Agent<br/>(Autonomous CRM)"]:::agent
        SupportBot["Support Bot<br/>(Customer Assistant)"]:::agent
    end

    subgraph AgentGate ["🛡️ AgentGate Core Gateway — Spring Boot 4.x Modular Monolith"]
        subgraph SecurityLayer ["Security & Ingress Layer"]
            OAuth2["OAuth2 Resource Server<br/>(JWT / Token Introspection)"]:::security
            PrincipalInjector["Principal Injector<br/>(_meta ➔ ContextSnapshot)"]:::security
            MCPEndpoint["MCP Endpoint<br/>(/mcp)"]:::ingress
            AdminAPI["Admin REST API<br/>(/api/v1/*)"]:::ingress
        end

        subgraph DomainServices ["Domain Services & Orchestration"]
            InvocationService["Invocation Service<br/>(Lifecycle Orchestrator)"]:::domain
            AuditService["Audit Service<br/>(Advisory Lock Append)"]:::domain
            AlertService["Alert Service<br/>(Breach / Security / Trip)"]:::domain
            KillSwitchService["Kill Switch Service<br/>(Redis Mirrored &lt;10ms)"]:::domain
            PolicyService["Policy Service<br/>(Binding Resolution &amp; Playground)"]:::domain
            ToolRegistry["Tool Registry<br/>(Per-Server Health &amp; SSRF Defense)"]:::domain
            HealthManager["Health Manager<br/>(Shared Circuit State)"]:::domain
        end

        subgraph GovernancePipeline ["🔒 Governance Pipeline (Sequential Guards)"]
            G1["1. SchemaValidationGuard<br/>(Strict JSON Schema &amp; Normalize)"]:::guard
            G2["2. KillSwitchGuard<br/>(AGENT / TOOL / ALL_WRITE)"]:::guard
            G3["3. PolicyEvaluator<br/>(Version Pinned &amp; Default Deny)"]:::guard
            G4["4. RateLimit + Quota Guard<br/>(Redis Lua + UTC Daily Budget)"]:::guard
            G5["5. ApprovalGuard<br/>(JPA StateMachine &amp; Approver Flow)"]:::guard
            G6["6. RedactionGuard<br/>(Mask PII/NID + Truncation Notice)"]:::guard
        end

        subgraph FrontendLayer ["🖥️ Frontend Layer (Thymeleaf UI)"]
            AdminUI["Admin Web Dashboard<br/>(Approvals / Policies / Audit / Kill Switch / Alerts)"]:::frontend
        end

        subgraph InfrastructureLayer ["💾 Infrastructure & Data Layer"]
            Outbox["Outbox Dispatcher<br/>(attempt_count / backoff / replay)"]:::infra
            Redis["Redis (In-Memory Tier)<br/>• Rate-limit counters (Lua)<br/>• Policy cache + Pub/Sub<br/>• Circuit state + Kill switch mirror<br/>• Tools list cache"]:::infra
            PostgreSQL["PostgreSQL (Durable Relational Store)<br/>• Invocations &amp; Chained Audit Events<br/>• Policy Bindings &amp; Version History<br/>• Kill Switches, Quotas &amp; Rate Limits<br/>• Transactional Outbox &amp; Alerts"]:::infra
        end
    end

    subgraph DownstreamTargets ["🔌 Downstream Target Servers"]
        TargetA["MCP Server A (Refund)<br/>(Streamable HTTP)"]:::downstream
        TargetB["MCP Server B (Order)<br/>(Order Fulfillment)"]:::downstream
        TargetC["REST API (Customer)<br/>(OpenAPI Imported)"]:::downstream
    end

    %% External to Ingress
    ExternalAgents -->|Bearer Token / OAuth2| OAuth2
    OAuth2 --> PrincipalInjector
    PrincipalInjector --> MCPEndpoint
    PrincipalInjector --> AdminAPI

    %% Frontend to Admin
    AdminUI -->|REST / Thymeleaf| AdminAPI

    %% Invocation Pipeline
    MCPEndpoint -->|Tool Call Request| InvocationService
    InvocationService --> G1
    G1 --> G2
    G2 --> G3
    G3 --> G4
    G4 --> G5
    G5 --> G6
    G6 -->|Approved / Low Risk| HealthManager

    %% Downstream Dispatch
    HealthManager -->|Authorized Execution| TargetA
    HealthManager -->|Authorized Execution| TargetB
    HealthManager -->|Authorized Execution| TargetC

    %% Audit & DB Logging
    InvocationService -->|Advisory Lock Append| AuditService
    AuditService -->|Chained Append| PostgreSQL

    %% Operational Services
    KillSwitchService -.->|Mirrored State| G2
    PolicyService -.->|Pinned Version| G3
    ToolRegistry -.->|Health Status| HealthManager
    AlertService -->|Record Alert| PostgreSQL

    %% Redis Pub/Sub & Counters
    G4 <-.->|Atomic Lua / TIME| Redis
    KillSwitchService <-.->|Pub/Sub: killswitch| Redis
    PolicyService <-.->|Pub/Sub: policy| Redis
    HealthManager <-.->|Pub/Sub: circuit| Redis
    AlertService <-.->|Pub/Sub: alert| Redis

    %% Outbox & Persistence
    PostgreSQL <-->|Poll & Dispatch| Outbox
    Outbox -.->|Replay / Events| Redis
```

*Vector diagram available at:* `docs/diagrams/architecture_clean.svg`

---

## 🔒 Request Governance Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor Agent as 🤖 AI Agent
    participant Sec as 🛡️ Security / Ingress
    participant Inv as ⚙️ InvocationService
    participant Pipe as 🔒 Governance Pipeline
    participant Redis as ⚡ Redis Tier
    participant DB as 💾 PostgreSQL
    participant Admin as 👤 Human Approver
    participant Target as 🔌 Downstream Target

    Agent->>Sec: POST /mcp (Tool Call + Bearer Token)
    Sec->>Sec: Authenticate OAuth2 & Build ContextSnapshot
    Sec->>Inv: Handle tool call (agent_id, user_id, tool, args)
    Inv->>DB: Begin Invocation (Status: RECEIVED)
    
    rect rgb(30, 41, 59)
        note over Inv,Pipe: 🔒 Sequential Governance Verification
        Inv->>Pipe: Execute Guards(Context, Tool, Args)
        Pipe->>Pipe: 1. SchemaValidationGuard (Strict JSON Schema)
        Pipe->>Redis: 2. KillSwitchGuard (Authoritative Mirror Check)
        Pipe->>DB: 3. PolicyEvaluator (Resolve Binding & Pin Immutable Version)
        Pipe->>Redis: 4. RateLimit + QuotaGuard (Atomic Lua Script)
        
        opt Human Approval Required (High-Risk Action)
            Pipe->>DB: 5. ApprovalGuard (Persist APPROVAL_REQUEST, Status: PENDING)
            Pipe-->>Agent: Return Pending Handle (Invocation Paused)
            Admin->>DB: Review, Edit Args & Approve via Admin UI
            DB-->>Pipe: Approval Event Triggered
            Pipe->>Pipe: Revalidate Schema & Policy for Edited Arguments
        end
        
        Pipe->>Pipe: 6. RedactionGuard (Apply Irreversible Masking)
    end

    Pipe-->>Inv: Governance PASS (Status: APPROVED)
    Inv->>Redis: Check Downstream Circuit Breaker State
    Inv->>Target: Execute Downstream Call (MCP/REST with Timeout)
    Target-->>Inv: Raw Response Payload
    
    Inv->>Pipe: Post-Execution Redaction & Truncation Check
    Pipe-->>Inv: Sanitized Payload
    
    rect rgb(15, 23, 42)
        note over Inv,DB: 💾 Atomic Audit & Outbox Commit
        Inv->>DB: Acquire PostgreSQL Advisory Lock
        Inv->>DB: Append AUDIT_EVENT (curr_hash = SHA256(prev_hash + seq + payload))
        Inv->>DB: Insert EVENT_PUBLICATION (Transactional Outbox)
        Inv->>DB: Update TOOL_INVOCATION (Status: COMPLETED)
    end
    
    Inv-->>Sec: Safe Execution Result
    Sec-->>Agent: 200 OK (Clean Tool Result)
```

---

## 🧩 Architectural Components

### 1. Security & Ingress Boundary
- **OAuth2 Resource Server**: Validates bearer tokens (JWT or opaque via introspection), verifying issuer, audience, and scope constraints.
- **Principal Injector**: Maps authenticated agent identity and trusted on-behalf-of end-user claims (`_meta`) into an immutable `ContextSnapshot`. Forged caller headers are rejected.
- **MCP Ingress Endpoint (`/mcp`)**: Implements the Model Context Protocol (Streamable HTTP / JSON-RPC). Exposes tool discovery and tool execution interfaces.
- **Admin REST API (`/api/v1/*`)**: Admin endpoints for policies, kill switches, approvals, and audits utilizing `ApiResponse<T>` and `ApiError` conventions.

### 2. Domain Services
- **`InvocationService`**: Owns the invocation lifecycle state machine and orchestrates pipeline guards.
- **`AuditService`**: Appends audit events into a tamper-evident cryptographic hash chain serialized via PostgreSQL advisory locking.
- **`PolicyService`**: Manages policy lifecycle, version immutability, binding resolution, and dry-run execution.
- **`KillSwitchService`**: Controls global-write, agent, and tool kill switches with Redis mirror sync.
- **`ToolRegistryService`**: Manages tool registration, downstream server discovery, and OpenAPI import with SSRF defenses.
- **`HealthManager`**: Integrates Resilience4j circuit breakers and Redis shared state. Hides degraded tools from agent discovery.
- **`AlertService`**: Handles security alerts, rate limit violations, and system trips.

### 3. Governance Guards (Composite Pipeline)
1. `SchemaValidationGuard`: JSON Schema validation and argument normalization.
2. `KillSwitchGuard`: Low-latency emergency write cessation.
3. `PolicyEvaluator`: Pinned immutable policy version evaluation with default-deny semantics.
4. `RateLimit + Quota Guard`: Atomic token-bucket / sliding window rate limits and UTC daily spend caps in Redis.
5. `ApprovalGuard`: Persisted JPA state machine for human-in-the-loop review.
6. `RedactionGuard`: Irreversible masking of PII (emails, BD phone numbers, NID) and output truncation notices.

### 4. Infrastructure & Data Tier
- **PostgreSQL 16+**: Authoritative persistent store managed by Flyway migrations.
- **Redis 7+**: In-memory tier for atomic Lua rate limit counters, policy cache, kill switch mirrors, and circuit breaker states.
- **OutboxDispatcher**: Polling dispatcher executing transactional outbox delivery with exponential backoff and dead-letter replay.

---

## 🛡️ Critical Invariants
- **Approval Argument Digest**: Approvals are cryptographically bound to `invocation_id` and `arguments_hash`. Edited arguments must undergo complete revalidation.
- **Serialized Audit Chain**: Concurrency on the audit hash chain is explicitly serialized via PostgreSQL advisory locks.
- **Transactional Outbox**: Outbox records are inserted in the exact same transaction as the domain state change.
- **Fail-Closed Policy**: Medium and high-risk tool calls fail closed if the policy store is unreachable. Low-risk tool calls may use cached policy for at most 5 minutes.
- **Unknown Outcome on Write Timeout**: Downstream timeouts on non-idempotent write operations receive `UNKNOWN_OUTCOME` and are never blindly retried.
