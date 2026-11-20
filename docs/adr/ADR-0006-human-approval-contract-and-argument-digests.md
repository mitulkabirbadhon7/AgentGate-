# ADR-0006: Human Approval Contract & Cryptographic Argument Digests

## Status
Accepted

## Context
High-risk tool calls (e.g. fund transfers, order cancellations) require human sign-off. If an operator modifies tool parameters (e.g., lowering a transaction amount from 50,000 BDT to 10,000 BDT) before approving, the edited arguments must not bypass schema validation, policy limits, or emergency kill switches.

## Decision
1. Invocations requiring approval generate an `APPROVAL_REQUEST` linked 1:1 with `TOOL_INVOCATION` with status `PENDING`.
2. The request computes and stores `arguments_hash = SHA256(canonicalJson(request_arguments))`. Original arguments and edited arguments are stored encrypted (`AES-256-GCM`).
3. An approver can `APPROVE`, `REJECT`, or `EDIT_AND_APPROVE`.
4. If arguments are edited, the gateway re-executes the Governance Pipeline (Schema Validation, Policy Evaluator, Kill Switch, and Quota Guards) against the edited arguments before downstream dispatch.
5. Approvals enforce an expiration deadline; expired requests fail closed.

## Consequences
- Prevents approval replay attacks and argument smuggling.
- Protects downstream systems from malformed edited arguments.
- Full cryptographic traceability of operator decisions.
