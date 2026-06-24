# AGENTS.md

This repository uses a CI-based development harness for the AMHS/OHT transfer control project.

## First Rule

Do not read every harness document for every task.

Classify the task first, then read only the relevant documents under `.md/` when they are available in the workspace.

## Document Routing

Backend domain or business logic work:

- `.md/01_DOMAIN_RULES.md`
- `.md/02_API_CONTRACT.md` when API or SSE behavior is affected
- `.md/03_BACKEND_CODING_RULES.md`
- `.md/07_DONE_DEFINITION.md`

Backend API contract work:

- `.md/01_DOMAIN_RULES.md`
- `.md/02_API_CONTRACT.md`
- `.md/03_BACKEND_CODING_RULES.md`
- `.md/07_DONE_DEFINITION.md`

Performance or load-test work:

- `.md/08_PERFORMANCE_VALIDATION.md`
- `.md/03_BACKEND_CODING_RULES.md`
- `.md/07_DONE_DEFINITION.md`

Documentation or portfolio work:

- actual code being described
- measurement results being claimed
- `.md/01_DOMAIN_RULES.md` for domain behavior
- `.md/08_PERFORMANCE_VALIDATION.md` for performance numbers

## Hard Constraints

- Do not invent status values, API paths, SSE event names, or error codes.
- Do not introduce Kafka, RabbitMQ, WebFlux, WebSocket, QueryDSL, jOOQ, or MSA unless explicitly requested.
- Keep backend enum, API endpoint, SSE event, and frontend type contracts synchronized.
- Do not commit `.md/` harness documents unless explicitly requested.
- Keep controller, application, domain, repository, and monitoring responsibilities separated.

## Verification

Run these before claiming backend work is complete:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/harness-check.ps1
.\gradlew.bat test
```

## Feedback Loop

If verification fails, do not only fix the immediate code.

Check whether the failure should become one of these:

- document rule
- test case
- harness-check rule
- API/SSE contract update
- backend/frontend type synchronization rule

Promote a failure into the harness only when it can recur or affects domain correctness, API contracts, architecture boundaries, concurrency safety, transactional consistency, or performance claims.
