# ADR-0003: OAuth2 Authentication & Trusted On-Behalf-Of Identity

## Status
Accepted

## Context
External AI agents interact with AgentGate programmatically. Attackers could attempt identity spoofing by injecting arbitrary headers (e.g., `X-Agent-ID: admin`) or forging user context claims.

## Decision
1. We authenticate all inbound agent invocations via Spring Security OAuth2 Resource Server using verified JWT tokens or token introspection.
2. The agent identity is extracted solely from authenticated principal claims (`sub` or `client_id`). Caller-supplied HTTP headers or request body identity fields are strictly ignored and rejected.
3. On-behalf-of human end-user identity must originate from verified token claims (`_meta` or trusted token exchange).
4. An immutable `ContextSnapshot` is instantiated upon authentication and bound to the Java 21 Virtual Thread for the duration of the request lifecycle.

## Consequences
- Guaranteed authenticity of agent principal.
- Spoofing attacks via request body or headers are completely neutralized.
- Audit logs accurately attribute actors with zero repudiation.
