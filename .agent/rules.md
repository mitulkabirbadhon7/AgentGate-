# AgentGate — Antigravity Rules

These are mandatory project rules for every coding session.

## Mandatory context
Before every task, read this file, `docs/MEMORY.md`, the relevant architecture/data/security documents, the implementation tracker, and applicable ADRs. Inspect existing source and tests before editing.

## Workflow discipline
- Work only on the requested phase and its acceptance criteria.
- First provide a short plan: files to create/change, design decisions, risks and tests.
- Do not start implementation if a blocking decision is unresolved; ask one focused question or propose an ADR.
- Never silently change requirements, schema, public API, dependency versions, architecture or security behavior.
- Do not overwrite existing user work without inspection.
- Do not claim a test/build passed unless you ran it and observed the result.
- After implementation, summarize changed files, commands run, actual results, known limitations and next step.

## Architecture
- Modular monolith first; no microservices without an accepted ADR.
- Feature-oriented packages with clear boundaries.
- Controllers are thin. Business logic belongs in application/domain services.
- Repositories are accessed through application/infrastructure boundaries, not controllers.
- Use ports/adapters at genuine integration boundaries (MCP, REST, policy evaluator, redactor, publisher).
- Use constructor injection; avoid field injection.
- Prefer cohesive small classes over giant services/controllers.
- Apply design patterns only where they solve a real problem.

## Java quality
- Use meaningful names, explicit types, enums for closed sets, immutable DTOs/records where suitable.
- Use BigDecimal for money; never float/double for currency.
- Use Instant/UTC for persisted event timestamps.
- Validate boundary input with Bean Validation and JSON Schema validation.
- Avoid raw Map/Object for domain state unless dynamic JSON is genuinely required.
- Do not use Lombok to hide important behavior or generate excessive boilerplate without a reason.

## API/error conventions
- Use a consistent ApiResponse<T> and ApiError contract for REST endpoints.
- Use @RestControllerAdvice for centralized REST exception mapping.
- Use Spring Security authentication entry points/access-denied handlers for 401/403.
- Do not build an Express-style asyncHandler wrapper; use Spring's request lifecycle and exception propagation.
- Return stable machine-readable error codes; never leak stack traces, SQL, tokens, secrets or internal hostnames.
- Include correlation/request ID in logs and responses where applicable.
- Map MCP failures to safe protocol-appropriate errors separately from REST response DTOs.

## Security invariants
- Default deny.
- Derive agent identity from verified OAuth2 authentication, never caller-supplied identity fields.
- On-behalf-of user identity must come from a verified trusted claim or documented trusted exchange.
- Never store plaintext client secrets or downstream credentials.
- Validate arguments before downstream execution.
- Re-check kill switch immediately before a sensitive downstream side effect.
- Human approval is bound to invocation ID and exact argument digest.
- Edited arguments must be revalidated and re-authorized.
- Never blindly retry non-idempotent writes.
- Medium/high-risk calls fail closed when authoritative policy cannot be verified.
- Redis Pub/Sub is not durable; design cache invalidation with TTL/version/authoritative checks.
- Do not log raw sensitive arguments/results.

## Database
- Flyway migrations are authoritative.
- Do not use Hibernate schema auto-update in production.
- Add database constraints, FKs, indexes and check constraints deliberately.
- Published policy versions are immutable.
- Audit events are append-only; restrict UPDATE/DELETE privileges.
- Use optimistic locking for concurrent approval/config changes.
- Keep invocation state, audit events and outbox events conceptually separate.
- Never store Redis counters as the only durable quota state.

## Testing
- Unit test business rules and state transitions.
- Integration test PostgreSQL/Redis transactions and repositories.
- Security-test spoofing, cross-customer access, argument smuggling, approval replay and kill-switch bypass.
- Fault-test policy-store, Redis and downstream failures.
- Run formatter/static analysis/tests/build for the affected scope.
- Update docs, ADRs and tracker when decisions or progress change.

## Git and secrets
- Never commit `.env`, credentials, private keys, tokens or production data.
- Keep `.env.example` with variable names and safe placeholders only.
- Review git diff and git status before suggesting a commit.
