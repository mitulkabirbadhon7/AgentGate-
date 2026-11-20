# Phase 4 — Human Approval and Kill Switches

Implement:
- Approval state machine with PENDING/APPROVED/REJECTED/EXPIRED/CANCELLED.
- Approver roles and authorization.
- Web admin approval workflow and webhook notification.
- Configurable expiry and safe caller result contract.
- Argument editing bound to invocation and digest.
- Revalidate schema, policy, kill switch and limits after edits.
- KILL_SWITCH scopes: AGENT, TOOL, GLOBAL_WRITES.
- Cancel affected pending approvals.
- Redis mirror plus reliable freshness/authoritative check strategy.

Tests:
- every legal and illegal state transition.
- approval replay against different invocation rejected.
- edited arguments require fresh validation/authorization.
- timeout vs approve race.
- kill switch blocks direct invocation and cancels pending approvals.
- cache invalidation missed by an instance does not bypass emergency stop.

Stop after Phase 4.
