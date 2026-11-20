# Phase 2 — Schema Validation and Policy Engine

Implement:
- Tool JSON Schema validation and safe normalization.
- POLICY, immutable POLICY_VERSION and POLICY_BINDING persistence.
- Binding scopes GLOBAL/AGENT/TOOL/AGENT_TOOL with DB/service consistency checks.
- Deterministic policy precedence and conflict behavior from accepted ADR.
- Default deny and explainable decision result.
- Dry-run that records would-be decision without applying the block.
- Policy playground API/UI.
- Cache versioning, TTL and invalidation; propagation <=10 seconds.
- Low-risk last-known snapshot bounded to five minutes; medium/high fail closed.

Tests:
- all binding scopes and precedence ties.
- default deny, allow, deny, argument conditions and dry-run.
- immutable published version.
- invalid cross-policy active version.
- cache invalidation and outage behavior.
- policy update propagation measurement.

Stop after Phase 2.
