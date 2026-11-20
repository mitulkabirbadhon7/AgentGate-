# AgentGate — Verification Strategy

## Unit tests
- Policy matching, binding precedence, default deny, dry-run.
- JSON Schema validation and normalization.
- Redaction and truncation.
- Approval transitions, expiry and edited-argument revalidation.
- Kill-switch scopes.
- Rate/quota windows and concurrent boundary behavior.
- Audit canonicalization/hash verification.
- Tool health/discovery filtering.

## Integration tests
Use Testcontainers for PostgreSQL and Redis.
- Flyway migrations from empty database.
- Constraints, repository behavior and transaction boundaries.
- Policy publication/cache invalidation.
- Invocation + audit + outbox transaction.
- Concurrent audit append.
- Outbox retry/deduplication.
- Redis atomic rate counters.
- OAuth2 authentication and authorization.
- MCP/REST adapter contract and timeout behavior.

## End-to-end tests
- Agent authentication and permitted tool discovery.
- Allowed and denied calls.
- High-risk approval; reject/expire; edit and revalidate.
- Kill switch blocks new calls and cancels pending approvals.
- Policy change propagation <=10 seconds.
- Downstream failure opens circuit and hides tool.
- Audit search/export and tamper verification.
- OpenAPI SSRF test cases.

## QA evidence
- QA1: 30 malicious scripted calls; 200 legitimate calls; report block/hold and false-block rates.
- QA2: 100 calls/sec load test; p50/p95/p99 overhead and reproducible method.
- QA3: timed audit investigation and tamper test.
- QA4: policy-store outage fault injection; prove risk-specific behavior and 5-minute low-risk maximum.
- QA5: classmate follows runbook; time REST tool import/configuration/policy setup without code changes.

Never claim an acceptance criterion is met without recorded test evidence.
