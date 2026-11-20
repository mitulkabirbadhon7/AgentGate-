# ADR-0005: Audit Hash Chain and Concurrency Serialization

## Status
Accepted

## Context
Quality Attribute QA3 requires tamper-evident audit logging with verifiable integrity. Concurrent invocations appending to an audit chain risk hash race conditions and branching chains if uncoordinated.

## Decision
1. Each audit event links cryptographically to its predecessor:
   `curr_hash = SHA256(prev_hash + sequence_number + canonical_payload_json)`
2. To support high concurrent throughput without unbounded lock contention, serialization of hash calculation and insertion is scoped to a database transaction using a dedicated PostgreSQL advisory transaction lock: `SELECT pg_advisory_xact_lock(hashtext('agentgate_audit_chain'))`.
3. Audit events are strictly append-only; database permissions and application code prohibit UPDATE and DELETE operations.
4. Tamper verification reads the sequential chain and recomputes hashes, detecting any modified payload, altered hash, or missing sequence number.

## Consequences
- Guaranteed non-repudiation and cryptographic integrity.
- Safe concurrent execution without chain collisions.
