# ADR-0006: Approval Contract and State Machine

## Status
Accepted

## Context
High-risk tool invocations (e.g. database deletions, wire transfers, permission changes) require human approval. The lifecycle involves asynchronous wait states, potential argument editing, timeouts, and cancellations.

## Decision
1. When policy flags an invocation as requiring approval, `TOOL_INVOCATION` transitions to `RECEIVED` / pending approval, and an `APPROVAL_REQUEST` is created with status `PENDING`.
2. The approval request stores the SHA-256 digest of original arguments (`arguments_hash`).
3. An approver can:
   - `APPROVE` as-is.
   - `EDIT_AND_APPROVE` modified arguments.
   - `REJECT` with reason.
4. If arguments are edited by human approvers, the invocation arguments are updated, and the pipeline MUST re-run JSON Schema validation, kill-switch checks, rate limits, and policy evaluation on the edited arguments.
5. Pending approvals maintain optimistic locking via a `version` column to prevent concurrent approval/expiry race conditions.
6. A kill-switch event affecting an agent or tool automatically transitions all associated pending approvals to `CANCELLED`.

## Consequences
- Prevents argument tampering or smuggling during human approval.
- Clean handling of race conditions between approver actions and timeout expirations.
