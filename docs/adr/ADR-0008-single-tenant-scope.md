# ADR-0008: Single-Tenant Scope and Extension Model

## Status
Accepted

## Context
AgentGate is initially targeted as a dedicated single-tenant governance gateway deployed per enterprise or cloud enclave. We must ensure the design remains clean and simple without premature multi-tenancy complexity while allowing future tenant isolation if required.

## Decision
1. AgentGate operates as a single-tenant deployment for the current release. All registered agents, downstream servers, and policies share the boundary of the running gateway instance.
2. Cross-tenant separation at this stage is achieved by deploying independent gateway instances per tenant environment with isolated database and Redis schemas.
3. Logical grouping is managed via agent identifiers, scopes (`GLOBAL`, `AGENT`, `TOOL`, `AGENT_TOOL`), and role clearances (`STANDARD`, `ELEVATED`, `ADMIN`).

## Consequences
- Keeps code and data model straightforward, maintainable, and easy to explain.
- Eliminates risk of cross-tenant data leaks in early releases.
- Easy to containerize and deploy across multiple client enclaves.
