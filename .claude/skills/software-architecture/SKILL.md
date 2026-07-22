---
name: software-architecture
description: Architecture and design-decision guidance for this full-stack catalog app (Spring Boot backend + React SPA + Postgres) — layering, module boundaries, API contract design, configuration/secrets, and how to weigh trade-offs before adding complexity. Use when planning a feature, introducing a new dependency, or restructuring code.
---

# Software Architecture (catalog app)

Shape of the system: React SPA (frontend/) ⇄ REST API (Spring Boot, backend/) ⇄
PostgreSQL, with Flyway-managed schema. Keep this simple; add structure only when a
concrete need justifies it.

How it's actually built (respect these established choices):
- Backend is **feature-packaged** (`catalog.product`, `catalog.review`, `catalog.shared`,
  `catalog.seo`) — a new domain gets its own package, not a shared layer folder.
- Auth is **HTTP Basic + stateless**, single in-memory admin; storefront reads and review
  submission are public, `/api/admin/**` is admin-only.
- Data is **soft-deleted** (`deleted` flag), never hard-deleted from the storefront.
- Errors use one shape (`ApiErrorResponse`), signaled via `ResponseStatusException`.
- Frontend server state is TanStack Query; admin session token in sessionStorage.
- Brand/name in UI copy: "Liams Creations".
See the `spring-boot-backend`, `postgres-data`, and `react-frontend` skills for specifics.

## Guiding principles

- **YAGNI + boring by default.** Prefer the simplest thing that satisfies the current
  requirement. No frameworks, layers, or abstractions for hypothetical futures.
- **Clear boundaries over clever coupling.** Frontend talks to backend only through the
  documented REST contract. Backend layers (controller → service → repository) don't
  skip levels.
- **Single source of truth.** Schema: Flyway. Server state: the database (surfaced via
  the API), cached client-side by TanStack Query. Don't duplicate authority.

## API contract

- REST, resource-oriented URLs, correct status codes. Requests/responses are DTOs, not
  entities.
- The contract is the boundary: version or evolve it additively. Breaking changes need
  a deliberate migration plan for the SPA.
- Errors have one consistent shape across the whole API (see `spring-boot-backend`).

## Where things live

- Cross-cutting backend concerns (security, rate limiting, error mapping, CORS) belong
  in filters/advice/config, not sprinkled in controllers.
- Business rules live in services, not controllers or the React app. The frontend may
  mirror validation for UX, but the backend is authoritative.
- Shared frontend logic → hooks; shared UI → components; API access → `src/api/`.

## Configuration & secrets

- All environment-specific values (DB URL, credentials, keys, external URLs) come from
  the environment, never hardcoded or committed.
- Separate profiles for test (H2) vs. runtime (Postgres).

## Introducing a new dependency or pattern — ask first

1. What concrete problem does it solve that the current stack can't?
2. What's the maintenance/security cost (transitive deps, updates)?
3. Is there a lighter option already in the stack?
   Prefer extending existing tools (Bucket4j, Caffeine, TanStack Query, React Router)
   over adding new ones.

## Non-functional concerns to keep in view

- **Security:** authn/authz on protected routes, input validation, no secret leakage,
  rate limiting on abuse-prone endpoints.
- **Performance:** pagination, indexing, caching read-heavy data, small initial JS bundle.
- **Observability:** Actuator health/metrics; meaningful logs without sensitive data.
- **Testability:** favor pure services and thin controllers so logic is unit-testable.

## When planning a feature

Produce: the API endpoints (method, path, request/response DTO), the schema change (+
Flyway migration), the service logic, the frontend data flow (query/mutation + keys),
and the tests — before writing code. See `feature-architect-planner` agent for larger
efforts.
