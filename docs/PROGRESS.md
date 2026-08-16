# Launch-readiness progress

Checkpoint: **2026-08-16**. Branch `feature/launch-blockers` (10 commits, not yet pushed).
Backend **100 tests green**; frontend builds and lints clean (still no frontend tests).

Working through an external audit (items A0–A15) to take the app from local-only to a
real public deployment where the shop owner uploads products and customers browse,
mostly on mobile.

---

## Decisions already made — do not reopen without reason

| Decision | Choice |
|---|---|
| **Hosting topology** | **Single origin.** One host serves the SPA, with `/api/*`, `/uploads/*`, `/sitemap.xml` routed to the backend. |
| **Where to host** | **Undecided on purpose.** AWS (console, cheaper services) or GCP, chosen after development finishes. Keep everything portable — the CloudFront + ALB layout in `infra/DEPLOYMENT.md` is one option, not a commitment. |
| **Image storage** | **S3, wherever the app ends up running.** Keeps photos decoupled from the compute platform. |
| **Image handling** | **Resize on upload** — a web-sized version (~1600px, ~200KB) plus a thumbnail. Owner uploads 3–8MB phone photos; without this a product page is 20–40MB, fatal on mobile data. |
| **Customer accounts** | **None.** No signup. The WhatsApp number is the identity. |
| **Order tracking** | **Lightweight orders screen.** Save the order on "Order on WhatsApp" (items, quantities, prices at that moment, customer details, order code like `LC-4821`). Admin moves it New → Confirmed → Paid → Shipped → Delivered / Cancelled by hand. No online payments. |
| **Privacy policy** | **Required, not optional**, once orders persist name/phone/email/address. |
| **SEO** | **Do not submit to Google until the real domain is live.** Indexing a temporary host URL creates duplicate-content problems. |
| **Demo data** | Solved. Verified on real Postgres that a fresh production database starts with **zero** products. |

---

## Done

| Item | What it was |
|---|---|
| **A1** | Uploaded images 404'd in dev — `/uploads` was not proxied by Vite. Worse than reported: it returned HTTP 200 with SPA HTML, so nothing looked like an error. |
| **A3** | Demo seed migrations would have populated a real shop with 100 fake sarees. Moved to `classpath:db/seed`, dev-only. **`V13` is load-bearing**: `V1` created `instagram_post_url NOT NULL` and `V3` (a seed file) dropped it, so skipping seeds in prod would have made every product insert fail. |
| **A4** | No deployment artifacts existed. Added multi-stage `Dockerfile` (non-root, healthcheck), `docker-compose.yml`, `.dockerignore`, GitHub Actions CI, `infra/DEPLOYMENT.md`. |
| **A5** | Rate limiter keyed on the proxy's address, so all visitors shared one bucket — one review submitter could lock out everyone. `ClientIpResolver` counts from the right of `X-Forwarded-For`. |
| **A7** | No 404 route (blank page) and no error boundary (white screen on any render throw). Both added, inside `ShopLayout`. |
| **A8** | `onSale=true` discarded status, category and search. Folded into one `findFiltered` query. |
| **A9** | Photos removed from a product leaked their files forever. Now diffed and deleted, best-effort. |
| **A11** | Cart persisted a full Product snapshot including price, so returning customers saw and WhatsApp-quoted stale prices. Now stores only product numbers + quantities; prices re-fetched every load. Legacy carts migrated. |
| **A10** | **Investigated — audit's claim was wrong.** `findFiltered` works fine on real Postgres; Hibernate 6 binds enum parameters with explicit types. No fix needed. |
| — | `PUBLIC_BASE_URL` had no prod value, so the live sitemap would have advertised `localhost` URLs. Now required. |
| — | `eclipse-temurin:17-jre-alpine` has no arm64 build; the image would not build on Apple Silicon. Switched to `17-jre-jammy`. |

---

## Still to do

**Blocks the owner from using it**
- **A2 — S3 storage + resize on upload.** Photos currently live inside the container and are wiped on every deploy. **This is the next task.** AWS SDK v2 (S3) and Thumbnailator are approved.
- **A13 — fail fast in prod** if the admin password is unset, blank, or still `admin123`.

**Then**
- **Orders feature** (new, agreed above): entity + migration, order codes, capture on WhatsApp handoff, admin Orders screen.
- **A14** — database indexes on `deleted`, `status`, `category`; advise on `pg_trgm` for search.
- **A6** — `robots.txt` needs an absolute sitemap URL; routing half depends on hosting. Hold submission until the domain exists.
- **A12** — admin auth options memo (Basic mitigations / short-lived token / session cookies). **Memo only — user decides before any implementation.**
- **A15** — HTTPS/HSTS, DB backups, log aggregation and uptime monitoring, privacy policy page + route.

---

## Known issues found but deliberately not fixed

- **Create responses return `null` timestamps.** `POST /api/admin/products` returns `createdAt`/`updatedAt` as null although the database has them set — the DTO is built before the flush.
- **Missing uploads return 401, not 404.** A 404 re-dispatches to `/error`, which `anyRequest().denyAll()` rejects. Likely affects every 404 on public routes and makes real debugging confusing.

---

## Testing notes that matter

- **H2 hides Postgres bugs.** During A8 a null `search` parameter produced `function lower(bytea) does not exist` on Postgres while **all tests stayed green on H2**. Postgres cannot infer a type for an untyped null. `findFiltered` now takes `""` instead of null — see the note on the method.
- **Testcontainers does not currently work on this machine.** Docker Desktop 29.7.2 enforces a minimum API version of 1.40; docker-java requests 1.32 and gets HTTP 400, which Testcontainers misreports as "Could not find a valid Docker environment". Bumping Testcontainers (1.20.6, 1.21.3), `DOCKER_API_VERSION`, and `testcontainers.properties` all failed. **Reverted — not in the build.** Options: downgrade Docker Desktop, use Colima/Rancher, run Testcontainers in CI only (recommended), or drop it.
- **The working alternative** is a throwaway Docker Compose stack on shifted ports, seeded through the admin API. It has caught two real bugs that the test suite missed. Use `-p <name>` and always tear down with `down -v`.
- **`timeout` does not exist on macOS.** Background the process and `kill` instead.
- **Always `mvn clean verify`** — stale `target/` output has already produced one false failure.
- **Never bind 5432 or 8080** in verification stacks; the user runs Postgres and the backend locally.
