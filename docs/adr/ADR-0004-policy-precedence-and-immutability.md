# ADR-0004: Declarative Policy Precedence & Immutability

## Status
Accepted

## Context
Multiple policies may apply to a single tool invocation (e.g., a global corporate compliance policy vs. an agent-specific allowance vs. a tool-specific rate limit). In addition, if policies can be mutated in place, historic audits lose reproducibility.

## Decision
1. **Default Deny**: Any invocation that does not match an explicit `ALLOW` or `REQUIRE_APPROVAL` policy rule is denied by default.
2. **Deterministic Precedence**: Scopes are evaluated with strict hierarchy:
   $$\text{GLOBAL} < \text{AGENT} < \text{TOOL} < \text{AGENT\_TOOL}$$
   Within the same scope, rules are sorted by `priority ASC`.
3. **Immutable Policy Versions**: Once published, a `POLICY_VERSION` record is write-once and permanently immutable. Updates create a new version with a monotonic version number and SHA-256 fingerprint.
4. **Version Pinning**: Every `TOOL_INVOCATION` records the exact `policy_version_id` evaluated.

## Consequences
- Guaranteed reproducible audit verification.
- Eliminates ambiguity when multiple conflicting policies exist.
- Hot policy updates propagate via Redis Pub/Sub within 10 seconds.
