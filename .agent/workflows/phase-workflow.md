# Antigravity Phase Workflow

For each phase:
1. Read `.agent/rules.md`, `docs/MEMORY.md`, architecture/data/security docs, tracker and applicable ADRs.
2. Inspect the repository, current branch, git status and existing tests.
3. Restate phase scope and acceptance criteria.
4. Identify unresolved decisions. If blocking, stop and ask.
5. Provide a plan with files/modules, migrations, risks and tests.
6. Implement only the requested phase in small increments.
7. Add tests alongside implementation.
8. Run formatter/static analysis, unit tests, integration tests relevant to the phase, and production build.
9. Inspect git diff for secrets, unrelated changes and architecture drift.
10. Update tracker, MEMORY and ADR/docs.
11. Report exact commands and actual results; list unrun tests honestly.
12. Suggest a commit message; do not commit/push unless the user explicitly asks.

Never jump to the next phase automatically.
