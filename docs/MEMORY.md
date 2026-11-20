# AgentGate — Durable Project Memory

## Product and course
- Product: AgentGate, a governance gateway for AI-agent tool calls.
- Course: CSE 444 — Software Architecture and Design Patterns.
- Build individually as a Spring Boot product.
- External scripted MCP clients represent agents; building the agents is out of scope.
- Downstream targets: MCP servers and plain REST endpoints; REST endpoints can be imported from OpenAPI.
- Initial deployment target: single tenant unless the instructor changes this.

## Functional requirements
FR1 Tool registry and unified permitted MCP endpoint.
FR2 OAuth2 client credentials for agents; trusted on-behalf-of end-user context.
FR3 Versioned policies, argument conditions, default deny, dry-run, changes effective within 10 seconds.
FR4 Human approval, edit/approve/reject, web page/webhook notification, configurable timeout.
FR5 JSON Schema validation, result redaction (email, Bangladesh phone, NID), result truncation notice.
FR6 Rate limits, daily quotas, write blast-radius cap and alerts.
FR7 Searchable/exportable tamper-evident audit.
FR8 Agent/tool/all-write kill switches and cancellation of pending approvals.
FR9 Policy playground.
FR10 Per-tool timeouts/circuit breakers and hiding unhealthy tools from discovery.

## Quality attributes
- QA1: 30 malicious calls blocked/held; <=5% false blocks among 200 legitimate calls.
- QA2: <=30 ms p95 gateway overhead at 100 calls/sec, excluding approval waits.
- QA3: investigation under one minute; audit tampering detected.
- QA4: medium/high fail closed on policy-store outage; low-risk may use last-known policy for <=5 minutes.
- QA5: classmate adds REST tool in <10 minutes without code changes.

## Design direction
- Modular monolith, feature-oriented packages.
- Spring MVC + virtual threads is the initial recommendation, subject to version/dependency compatibility and ADR.
- Spring Security OAuth2 Resource Server.
- Spring AI MCP server/client integration.
- PostgreSQL + Flyway for durable state; Redis for counters/cache/Pub/Sub/short-lived coordination.
- Resilience4j, Actuator, Micrometer, JUnit 5, Mockito, Testcontainers.
- Thymeleaf admin UI is the initial recommendation.
- Invocation lifecycle, audit events and outbox events are separate concepts.
- Default deny; policy versions immutable after publication.
- No untrusted end-user identity header.
- Do not assume Redis Pub/Sub delivers every invalidation.

## Current diagrams
- `docs/diagrams/architecture.png`: latest user-provided system architecture.
- `docs/diagrams/erd.png`: latest user-provided ER diagram.
- Both are design references; see written docs and ADRs for invariants and unresolved constraints.

## Decisions still to ratify
1. Exact Java, Spring Boot and Spring AI compatible versions.
2. MCP transport/protocol and whether to use WebMVC Streamable HTTP.
3. OAuth2 issuer, audience and trusted on-behalf-of claim mechanism.
4. Policy precedence/conflict resolution.
5. Audit chain scope, canonical serialization and concurrent append strategy.
6. Approval wait vs pending-handle contract.
7. Redis outage and kill-switch freshness guarantees.
