# ADR-0004: Policy Precedence and Evaluation Model

## Status
Accepted

## Context
Multiple governance policies may target global scope, specific agents, specific tools, or combined agent-tool pairs. A deterministic precedence model is mandatory to avoid ambiguity and race conditions.

## Decision
1. Policies evaluate under default-deny: if no rule explicitly permits the invocation, the decision is `DENY`.
2. Policy bindings resolve with precedence:
   - Level 1 (Highest): `AGENT_TOOL` binding.
   - Level 2: `TOOL` binding.
   - Level 3: `AGENT` binding.
   - Level 4 (Lowest): `GLOBAL` binding.
3. Within the same scope level, lower `priority` numerical values evaluate first.
4. Any explicit `DENY` rule encountered within the matching policy version immediately terminates evaluation with `DENY`.
5. An invocation pins the specific `version_id` of the evaluated policy for immutable audit and replayability.

## Consequences
- Predictable, deterministic governance outcomes.
- Granular overrides allow fine-tuning security posture per agent and tool without changing global policies.
