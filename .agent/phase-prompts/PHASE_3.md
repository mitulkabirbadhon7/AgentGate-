# Phase 3 — Invocation Lifecycle and Tamper-Evident Audit

Implement:
- TOOL_INVOCATION lifecycle with distinct invocation_id, correlation_id and idempotency_key.
- AUDIT_EVENT append-only model and append service.
- Defined chain scope, canonical serialization, hash inputs and concurrency strategy from ADR.
- Database permissions/migration strategy to restrict update/delete.
- Audit search, filtering, export and verify command/API.
- Transactional outbox row insertion with relevant state changes.
- Redacted audit payloads; never persist secrets.

Tests:
- allowed, denied, failed and unknown-outcome invocations.
- concurrent append produces valid ordered chain.
- tamper detection and missing-event detection.
- idempotency duplicate behavior.
- audit/outbox transaction rollback behavior.
- search/export authorization and redaction.

Stop after Phase 3.
