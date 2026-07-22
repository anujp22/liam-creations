---
name: testing
description: Testing strategy for the catalog app — backend JUnit 5 with MockMvc slices, full-context integration tests on H2, Spring Security test support, and rate-limit/error-path coverage; plus frontend testing approach for React + TanStack Query. Use when adding or reviewing tests, or deciding what to test for a change.
---

# Testing

Backend test deps present: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ,
MockMvc), `h2` (in-memory DB), `spring-security-test`. Run with `mvn clean test`.
Frontend has no test runner installed yet — see the frontend section before adding one.

Existing tests to copy as templates (they mirror the feature-package layout):
`ProductServiceTest` / `ReviewServiceTest` (service units), `ProductControllerTest`,
`AdminProductControllerTest`, `AdminAuthControllerTest`, `ReviewControllerTest`
(controller slices incl. auth), `ProductRepositoryTest` / `ReviewRepositoryTest`
(`@DataJpaTest` on H2), `ApiRateLimitFilterTest` (429 path), and
`ValidationExceptionHandlerTest` (advice → 400). Put a new feature's tests under
`src/test/java/com/codewithanuj/catalog/<feature>/…` matching the main-source package.

## What to test (the pyramid)

- **Many** fast unit tests on services / pure logic (no Spring context).
- **Some** slice/integration tests on controllers and repositories.
- **Few** end-to-end / full-context tests for critical flows.
  Don't invert this — a suite of only slow full-context tests is a smell.

## Backend

### Service / unit tests
- Plain JUnit 5 + Mockito. Mock the repository, assert business logic and edge cases.
- Test the unhappy paths: not-found, validation failure, forbidden, boundary values.
- AssertJ (`assertThat(...)`) for readable assertions.

### Controller slice tests (`@WebMvcTest`)
- Load just the web layer; mock the service (`@MockBean`).
- Assert status codes, response JSON shape, and validation → 400 with field errors.
- Cover the error-advice mapping (e.g. domain exception → 404/409).

### Repository / persistence tests (`@DataJpaTest`)
- Runs against H2. Verify custom `@Query` methods, constraints, and that Flyway
  migrations apply cleanly. Watch for Postgres/H2 dialect gaps.

### Security tests
- Use `spring-security-test`: `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`.
- Assert public routes are reachable and protected routes reject unauthenticated /
  unauthorized callers (401/403).

### Cross-cutting behavior
- **Rate limiting:** assert the 429 response after exceeding a Bucket4j bucket — this
  is a real feature and regressions are silent otherwise.
- **Caching:** verify cached reads don't re-hit the source when they shouldn't.

### Conventions
- Arrange–Act–Assert; one behavior per test; descriptive method names
  (`returns404WhenProductMissing`).
- Deterministic: no reliance on wall-clock, ordering, or shared mutable state.
- Keep test data setup close to the test; prefer builders/factories over huge fixtures.

## Frontend

No runner is installed yet. If tests are wanted, propose **Vitest + React Testing
Library + MSW** (Vite-native, minimal config) and confirm before adding deps.

When set up, focus on:
- **Components:** render, assert user-visible output, interact via role/label queries
  (Testing Library philosophy — test behavior, not implementation).
- **Query hooks:** wrap in a `QueryClientProvider`; mock the network with MSW rather
  than mocking `fetch` ad hoc. Assert loading → success and loading → error transitions.
- **The three states:** every data view's loading, error, and empty rendering.

Until a runner exists, `npm run lint` + `npm run build` (which type-checks via `tsc -b`)
are the frontend safety net — keep them green.

## Checklist for any change

1. New logic has unit tests including at least one failure/edge case.
2. New/changed endpoint has a slice test covering status + validation + error path.
3. Security-relevant change asserts the auth outcome (allowed and denied).
4. Schema change is exercised through H2/Flyway.
5. `mvn clean test` is green (and frontend `lint`/`build` if touched).
