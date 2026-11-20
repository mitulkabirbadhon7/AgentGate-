# ADR-0001: Modular Monolith Architecture

## Status
Accepted

## Context
AgentGate is designed as a zero-trust governance gateway for AI agent tool calls. Distributed microservices introduce significant network latency, distributed transaction complexity, and network partition failure modes that violate our p95 overhead requirements (<30ms).

## Decision
We adopt a Spring Boot Modular Monolith architecture organized into feature-oriented modules (`shared`, `security`, `agent`, `tool`, `policy`, `invocation`, `approval`, `audit`, `quota`, `killswitch`, `health`, `alert`, `outbox`). All inter-module communication within the process uses strongly typed Java interfaces and domain events without network hops.

## Consequences
- High performance with minimal serialization overhead.
- Direct transactional consistency across invocation, audit, and outbox tables.
- Clear module boundaries prevent spaghetti code.
- Microservice decomposition is prohibited without a new accepted ADR.
