# Phase 1 — Identity and Tool Registry

Read mandatory context and inspect Phase 0 implementation.

Implement:
- OAuth2 Resource Server with issuer/audience/scope validation.
- Agent client registry and secure credential hashing/rotation approach.
- Authenticated principal resolver; trusted on-behalf-of user claim mechanism from accepted ADR.
- Downstream server and tool CRUD.
- MCP and REST tool metadata/adapters.
- OpenAPI import with strict SSRF defenses and admin authorization.
- MCP endpoint and permitted/healthy tool discovery.
- Admin APIs with ApiResponse/ApiError conventions.

Do not implement full policy evaluation yet; discovery may use a clearly marked temporary authorization boundary only in local tests, never a production allow-all.

Tests:
- valid/invalid/expired/wrong-audience token.
- agent identity spoofing rejected.
- untrusted user header rejected.
- CRUD validation and uniqueness.
- OpenAPI malicious URL/private IP/redirect cases.
- MCP discovery hides disabled/unhealthy tools.
- adapter contract tests.

Stop after Phase 1 and update tracker/docs.
