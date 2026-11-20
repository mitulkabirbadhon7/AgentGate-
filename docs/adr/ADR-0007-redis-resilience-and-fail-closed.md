# ADR-0007: Redis Resilience, Mirror Freshness, and Fail-Closed Behavior

## Status
Accepted

## Context
Redis provides sub-millisecond rate-limiting counters, kill-switch mirrors, and cache invalidation via Pub/Sub. However, Redis Pub/Sub is inherently at-most-once and not durable. In the event of a Redis outage, security invariants must be preserved.

## Decision
1. Kill switches maintain authoritative state in PostgreSQL and are mirrored to Redis for sub-millisecond checks. A background poller refreshes local cache periodically, and every check verifies local mirror timestamp against maximum acceptable staleness (<5 seconds).
2. During a Redis outage:
   - Medium and High risk tool invocations FAIL CLOSED (rejected with `SERVICE_UNAVAILABLE` or `POLICY_STORE_UNREACHABLE`).
   - Low risk tool invocations are permitted to use local in-memory snapshot for up to 5 minutes, after which they also fail closed.
3. Redis rate limits utilize atomic Lua scripts; if Redis is unreachable, fail-closed behavior applies to medium/high-risk tools to protect downstream capacity.

## Consequences
- Zero-trust invariants preserved during partial infrastructure outages.
- Low-risk read operations remain resilient for brief network blips.
