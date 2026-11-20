# Phase 0 — Architecture Decisions and Project Foundation

Read all mandatory project context and inspect the current workspace. Do not implement business features.

Tasks:
1. Verify installed Java, Maven, Docker and Git.
2. Recommend compatible Java/Spring Boot/Spring AI versions using official compatibility documentation; do not guess.
3. Create the Spring Boot Maven skeleton with only agreed dependencies.
4. Add configuration profiles and safe `.env.example`; do not create real secrets.
5. Add Docker Compose for local PostgreSQL and Redis with health checks and persistent volumes.
6. Add Flyway baseline and a minimal health/readiness configuration.
7. Add GitHub Actions CI for build, tests and formatting/static checks.
8. Create/complete ADRs for transport, OAuth2 issuer/on-behalf-of trust, policy precedence, audit-chain concurrency, approval contract, Redis failure and single-tenant scope.
9. Ensure package layout follows API conventions.
10. Do not create all domain entities yet; only foundation.

Tests/exit:
- Maven build succeeds.
- Application starts with local profile.
- PostgreSQL and Redis health checks pass.
- Flyway runs.
- CI workflow syntax and commands are verified.
- No secrets are committed.
Update tracker and session log. Stop after Phase 0.
