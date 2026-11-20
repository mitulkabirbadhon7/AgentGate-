# Phase 5 — Limits, Redaction and Resilience

Implement:
- RATE_LIMIT_POLICY and QUOTA_POLICY semantics.
- Atomic Redis enforcement and explicit Redis outage behavior.
- Per-agent/per-tool rate limits, daily quotas and write blast-radius caps.
- Security alerts on breaches.
- Redaction for email, Bangladesh phone and NID patterns.
- Result-size limit and truncation notice.
- Per-tool/server timeouts and Resilience4j circuit breakers.
- Hide unhealthy tools from discovery and reject direct calls safely.
- Safe downstream write timeout/UNKNOWN_OUTCOME behavior; no blind retry.

Tests:
- concurrent quota/rate-limit boundary tests.
- daily UTC reset semantics.
- Redis outage fail-safe tests.
- redaction positive/negative cases.
- truncation notice.
- circuit opens/closes and discovery changes.
- downstream timeout after possible write commit does not duplicate operation.

Stop after Phase 5.
