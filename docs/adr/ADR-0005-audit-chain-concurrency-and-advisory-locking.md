# ADR-0005: Audit Chain Concurrency & PostgreSQL Advisory Locking

## Status
Accepted

## Context
A key requirement (FR7 / QA3) is a tamper-evident audit log where each event contains a SHA-256 hash of its predecessor (`prev_hash`). In a high-throughput environment ($\ge 100\text{ calls/sec}$), concurrent threads attempting to read the latest hash and write the next hash create race conditions and split chains unless properly synchronized.

## Decision
1. Concurrency is serialized at the database level using PostgreSQL transaction-level advisory locks:
   `SELECT pg_advisory_xact_lock(hashtext('audit_chain_partition_global'));`
2. Inside the protected transaction, the latest `curr_hash` and `sequence_number` are read (`FOR UPDATE`), the new SHA-256 hash is computed over `(prev_hash + sequence_number + event_time + canonical_payload)`, and the new row is appended.
3. The advisory lock releases automatically upon transaction commit or rollback.
4. Database privileges on the `audit_event` table grant `INSERT` and `SELECT` only; `UPDATE` and `DELETE` are denied to all runtime application roles.

## Consequences
- Guaranteed strictly linear, non-branching hash chain.
- Eliminates application-level distributed lock failures.
- Zero risk of audit split-brain under concurrent load.
