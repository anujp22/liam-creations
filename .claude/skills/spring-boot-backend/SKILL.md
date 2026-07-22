---
name: spring-boot-backend
description: Conventions and guardrails for the Spring Boot 3.3 / Java 17 catalog backend — controller/service/repository layering, DTOs and Bean Validation, Spring Security, Bucket4j rate limiting, Caffeine caching, and consistent error responses. Use when adding endpoints, services, entities, or wiring backend features.
---

# Spring Boot Backend (catalog-backend)

Stack: Spring Boot 3.3.12, Java 17, spring-web, spring-data-jpa, spring-validation,
spring-security, actuator, Bucket4j 8.10, Caffeine, PostgreSQL (H2 for tests), Flyway.
Module lives in `backend/catalog-backend`. Build/test with `mvn clean test`.

## Package layout (feature-first — follow it)

Root package `com.codewithanuj.catalog`. Code is grouped **by feature**, not by layer:

- `catalog.<feature>.{controller,dto,model,service,repository}` — e.g. `product`, `review`.
- `catalog.shared.{config,exception,storage}` — cross-cutting: `SecurityConfig`,
  `ApiRateLimitFilter`, `RequestLoggingFilter`, `WebConfig`, `GlobalExceptionHandler`,
  `ApiErrorResponse`, storage services.
- `catalog.seo` — sitemap etc.

A new feature (e.g. `order`) gets its own `catalog.order.*` package with the same
subpackages. Don't create a top-level `controllers`/`services` layer split.

## Layering (keep these boundaries)

- **Controller** (`@RestController`): HTTP only. Accept/return DTOs, validate input,
  map to service calls. No business logic, no repository access, no entity leakage.
- **Service** (`@Service`): business logic and transactions. Owns `@Transactional`
  boundaries. Takes/returns DTOs or domain types, never `HttpServletRequest`.
- **Repository** (`extends JpaRepository`): persistence only. Prefer derived query
  methods; use `@Query` for anything non-trivial and document why.
- **Entity** (`@Entity`): persistence model. Never serialize entities directly to the
  API — map to a response DTO (record) to avoid lazy-loading and over-exposure.

## DTOs and validation

- Use Java `record` types for request/response DTOs.
- Validate at the controller boundary: `@Valid @RequestBody CreateFooRequest req`.
- Use `jakarta.validation` constraints (`@NotBlank`, `@Size`, `@Positive`, `@Email`).
- Never accept the JPA entity as a request body.

## Error handling (this repo's actual convention)

- One `@RestControllerAdvice`: `shared.exception.GlobalExceptionHandler`. It returns
  the `ApiErrorResponse` record: `(timestamp, status, error, message, path)`. Reuse
  this shape — don't invent a second error format.
- **Services signal failures by throwing `ResponseStatusException`** (e.g.
  `new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")`). The advice
  maps it to the right status. This is the established pattern here — follow it rather
  than introducing custom exception classes unless a case truly needs one.
- `MethodArgumentNotValidException` → 400 with a joined `field: message, …` string.
- `MethodArgumentTypeMismatchException` → 400. Never leak stack traces.

## Security (this repo's actual setup)

- `shared.config.SecurityConfig` defines a `SecurityFilterChain` bean (component-based).
  Auth is **HTTP Basic, `SessionCreationPolicy.STATELESS`, CSRF disabled** (SPA + Basic).
- Admin is a single in-memory user (`InMemoryUserDetailsManager`, role `ADMIN`) with
  credentials from `${admin.username}`/`${admin.password}` env config; password via
  `BCryptPasswordEncoder`.
- Route rules: `GET /api/products/**`, `GET /api/reviews/**`, `POST /api/products/*/reviews`,
  `GET /uploads/**`, `GET /sitemap.xml`, `/actuator/health` are public; `/api/admin/**`
  requires `ROLE_ADMIN`; everything else `denyAll()`. Add new public routes in **both**
  `authorizeHttpRequests` and the custom `BasicAuthenticationFilter.shouldNotFilter`
  override (public routes skip Basic processing so stale cached creds can't block them).
- CORS origins come from `${cors.allowed-origin.dev|prod}`; `ApiRateLimitFilter` is
  registered before `BasicAuthenticationFilter`.
- Never log credentials, tokens, or full request bodies containing secrets.

## Rate limiting (Bucket4j) & caching (Caffeine)

- Rate limiting lives in `shared.config.ApiRateLimitFilter` (Bucket4j), registered in
  `SecurityConfig` before `BasicAuthenticationFilter`. When adding a new abuse-prone
  endpoint (esp. public writes like review submission), decide its bucket policy
  explicitly and test the 429 path (`ApiRateLimitFilterTest` is the model).
- Use Caffeine (`@Cacheable`) for read-heavy, slow-changing data. Always define an
  eviction/TTL policy; never cache user-specific data in a shared key.

## Config

- No secrets in `application.properties`/`application.yml` committed to git — use env
  vars / Spring config import. DB URL, credentials, and any API keys come from the
  environment.
- Keep profile-specific config (`application-test.properties` uses H2) separate.

## Checklist before finishing a backend change

1. New/changed endpoint has a DTO, validation, and an error path.
2. `@Transactional` is on the service method that needs it (write operations).
3. A test exists (MockMvc slice or full context) — see the `testing` skill.
4. `mvn clean test` passes.
5. If it touches the schema, there is a matching Flyway migration — see `postgres-data`.
