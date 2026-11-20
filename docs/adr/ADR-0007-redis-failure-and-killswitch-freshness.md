# ADR-0007: Redis Outage Handling & Authoritative Kill-Switch Freshness

## Status
Accepted

## Context
Redis is used as a fast tier for rate-limit counters, policy cache, and kill-switch mirrors. If Redis becomes temporarily unreachable or suffers a network partition, the gateway must not compromise system safety or fail open.

## Decision
1. **Kill Switch Freshness**: Sensitive write operations must check authoritative kill switch state. If Redis is unreachable, the gateway falls back immediately to querying PostgreSQL for active kill switches.
2. **Fail-Closed Policy**: During a complete cache and database outage, medium and high-risk operations fail closed immediately. Low-risk operations may rely on an in-memory last-known-good snapshot for at most 5 minutes.
3. **Dual Quota Protection**: While Redis Lua scripts handle primary atomic increments, daily quota spend totals are asynchronously reconciled against PostgreSQL records via the outbox publisher.

## Consequences
- Guaranteed zero emergency stop bypasses even during total cache downtime.
- Defense-in-depth against denial-of-service and network partitions.
- Clear, predictable degradation semantics.
