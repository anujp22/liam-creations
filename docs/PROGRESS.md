# Launch-readiness progress

Checkpoint: **2026-08-21**. Branch `feature/launch-blockers`.
Backend **186 tests green**; frontend **28 tests green**, lints and builds clean.

Working through an external audit (items A0–A15) to take the app from local-only to a
real public deployment where the shop owner uploads products and customers browse,
mostly on mobile.

**Where this now stands: every remaining item depends on a hosting decision, not on
code.** The app is feature-complete for launch. See "What only you can do" below.

---

## Decisions already made — do not reopen without reason

| Decision | Choice |
|---|---|
| **Hosting topology** | **Single origin.** One host serves the SPA, with `/api/*`, `/uploads/*`, `/sitemap.xml` routed to the backend. |
| **Where to host** | **Undecided on purpose.** AWS (console, cheaper services) or GCP, chosen after development finishes. Keep everything portable — the CloudFront + ALB layout in `infra/DEPLOYMENT.md` is one option, not a commitment. |
| **Image storage** | **S3, wherever the app ends up running.** Keeps photos decoupled from the compute platform. |
| **Image handling** | **Resize on upload** — a web-sized version (~1600px, ~200KB) plus a thumbnail. |
| **Customer accounts** | **None.** No signup. The WhatsApp number is the identity. |
| **Order tracking** | **Lightweight orders screen.** Done — see the Orders section below. |
| **Privacy policy** | **Required, not optional.** Page written and routed; the copy still needs your review. |
| **SEO** | **Do not submit to Google until the real domain is live.** Indexing a temporary host URL creates duplicate-content problems. |
| **Demo data** | Solved. Verified on real Postgres that a fresh production database starts with **zero** products. |

---

## What only you can do — these are the slow ones

None of them block further development; all of them block launch.

1. **Pick a host (AWS or GCP).** A6, the `/uploads` edge routing, HTTPS/HSTS and
   `TRUSTED_PROXY_COUNT` are all downstream of this.
2. **Buy the domain.** SEO submission is deliberately held until it exists.
3. **Create the S3 bucket and IAM credentials** — `infra/DEPLOYMENT.md`, "Photo bucket setup".
4. **Set a real `ADMIN_PASSWORD`.** The app now refuses to boot without one (A13).
5. **Review the privacy policy draft** at `frontend/src/pages/PrivacyPolicyPage.tsx` and
   give me a contact address for data requests — it is a `REPLACE-ME@example.com`
   placeholder today.
6. **Confirm `VITE_OWNER_WHATSAPP`** is the real business number.
7. ~~Decide A12~~ — **decided 2026-08-21: Option 3, HttpOnly session cookie.** Build it
   at handover, not before. See below.

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
| **A2** | Photos were stored byte-for-byte in the container: a product page came to 20-40MB, and every deploy wiped the files. Uploads now go through `ImageProcessor` (~1600px web image + 400px thumbnail), and `app.storage=s3` swaps in `S3StorageService`. **Verified end to end** against MinIO and a real 6.1MB photo: 92KB web + 9.8KB thumb. |
| **A10** | **Investigated — audit's claim was wrong.** `findFiltered` works fine on real Postgres; Hibernate 6 binds enum parameters with explicit types. No fix needed. |
| **A13** | Production would inherit the shipped `admin/admin123` if `ADMIN_PASSWORD` was forgotten, and boot looking healthy. `AdminCredentialsValidator` now refuses to start on a blank, well-known, too-short, or username-matching password. Written as an `EnvironmentPostProcessor`, not a bean — see below. |
| **Orders** | New feature, agreed in the decision table. Entity + `V14`, order codes from a sequence, capture on the WhatsApp handoff, admin Orders screen. See its own section below. |
| **A14** | No index on `products` beyond the primary key. Five partial indexes added in a Postgres-only migration, each chosen by measurement. See below. |
| **A12** | **Decided, deliberately not built.** Option 3 (HttpOnly server-side session cookie) chosen 2026-08-21. Memo: `docs/ADMIN_AUTH_OPTIONS.md`. |
| **Privacy policy** | Written and routed at `/privacy`, linked from the footer and the checkout form. Copy is a draft pending your review. |
| **Frontend tests** | There were none; the only gate was `tsc && vite build`. Vitest + Testing Library added, wired into CI. 28 tests. |
| — | `PUBLIC_BASE_URL` had no prod value, so the live sitemap would have advertised `localhost` URLs. Now required. |
| — | `eclipse-temurin:17-jre-alpine` has no arm64 build; the image would not build on Apple Silicon. Switched to `17-jre-jammy`. |
| — | **The security chain was overwriting real error statuses.** Fixed — see below. |

---

## Still to do

**Blocked on the hosting decision, not on code**

- **A6** — `robots.txt` needs an absolute sitemap URL; the routing half depends on the
  host. Hold search-engine submission until the domain exists.
- **A15** — HTTPS/HSTS, database backups, log aggregation, uptime monitoring. The
  privacy-policy half of A15 is done.

**Blocked on you**

- **A12 — implement Option 3 at handover.** The decision is made; the work is deferred
  on purpose, because dev deliberately keeps `admin`/`admin123` and A13 already blocks
  those in prod only. When it is built: **CSRF must be re-enabled for admin routes** —
  currently disabled, which is fine for header auth and unsafe for cookies. That is the
  part to test hardest. Note it is buildable locally whenever you want: `Secure` cookies
  work on `localhost`, and single-origin routing is already settled.
- The privacy policy contact address.

---

## Orders — decisions worth knowing before changing that code

**Verified in a real browser (2026-08-21), not just by tests.** Driven with Playwright at
420px width against the throwaway stack, because every path below is one a customer on a
phone can actually reach:

| Path | Result |
|---|---|
| Happy path | Order saved, code shown on screen, WhatsApp message built from the server's total. No console errors. |
| Empty cart | "Your cart is empty." — no form, nothing to submit. |
| Required fields blank | Three field errors; WhatsApp **not** opened. |
| Malformed email | Field error; WhatsApp **not** opened. |
| Save fails (backend hiccup) | WhatsApp **still opens**, message carries no order code, customer told the order was not recorded. The deliberate trade-off. |
| Item gone (409) | Handoff **blocked** with "No longer available… Please remove it and try again." |
| Product deleted while in the cart | Dead item dropped with a "Remove PN-xxx" button, surviving item and total intact, order still placeable. |
| Admin Orders screen | Lists newest first, expands to contact details and the price snapshot, "Move to" omits the current status, status change persists. |


- **The client sends product numbers and quantities. Nothing else.** Prices and the
  total are looked up and summed server-side, so a hand-built request cannot name its
  own total. The response carries those prices back, and the storefront builds the
  WhatsApp message *from the response* — one calculation rather than two that agree most
  of the time. The test for this uses a server total that deliberately disagrees with
  the cart; with matching numbers it passed even when the message was built from the
  cart, which is exactly the bug it exists to catch.
- **`order_items` snapshots title and unit price, and has no foreign key to `products`.**
  An order records what was agreed; deleting or repricing a product must not rewrite it.
  This is A11's reasoning applied to the permanent record rather than the temporary one.
  Verified: an order placed at ₹5,000 still reads ₹5,000 after the product moved to ₹9,999.
- **If saving the order fails, WhatsApp still opens** — with the cart's own prices and no
  order code, and the customer is told it was not recorded. Losing the record is bad;
  losing the sale to a backend hiccup is worse. The single exception is a 409 (an item
  has genuinely gone), which blocks the handoff.
- **Status moves freely in any direction, on purpose.** A forward-only rule would make an
  accidental "Delivered" permanent, and the realistic mistake on a phone is a mis-tap,
  not an invalid transition. Only Cancel asks for confirmation.
- **Order codes come from a sequence (`LC-1000`, …)** and are never reused, exactly like
  product numbers. A customer quoting "LC-1042" weeks later must still mean one order.
- `POST /api/orders` is public and unauthenticated (there are no customer accounts) and
  it writes personal data, so it is rate limited at **5/min per client IP** in its own
  bucket, alongside review submission.

## A13 — why an `EnvironmentPostProcessor` and not a bean

A `@PostConstruct` bean was written first and worked, but it runs mid-refresh with no
ordering guarantee against Flyway. Verified against a real boot: with the database
unreachable, the operator got a Flyway connection stack trace and no mention of the
password at all. The `EnvironmentPostProcessor` runs before any bean exists, so the
message about the real problem is the only one printed. It is registered in
`META-INF/spring.factories` — a test asserts that registration, because losing it would
leave every rule test passing while production stopped being checked.

## A14 — what the measurements actually said

Measured against a real Postgres with 50,000 products, 10% soft-deleted, **before**
writing the migration. Two findings the audit's framing did not anticipate:

- **Paginated listings were already fast without any index** — Postgres walks the primary
  key and filters as it goes. What is slow is the same query *with a sort*, and the
  storefront always applies one. Sorting forces the whole filtered set to be materialised.
- **"Indexes on deleted, status, category" is not quite the fix.** Every query also
  filters on `deleted`, so partial indexes that fold that predicate in beat plain ones on
  both size and speed. A plain index on `deleted` was tried; the planner ignored it for
  the Deleted tab while costing 360 KB.

Before → after (median `EXPLAIN ANALYZE`): Newest First 8.7 ms → 0.04 ms; sitemap 36.9 ms
→ 2.9 ms; Sale page 3.3 ms → 0.02 ms; listing by price 6.8 ms → 2.3 ms; admin metrics
count 4.7 ms → 1.0 ms.

**H2 rejects partial indexes**, so these live in `db/migration-postgresql` — a *sibling*
of `db/migration`, not a subdirectory, because Flyway scans locations recursively.
`PostgresOnlyMigrationsTest` guards that arrangement: dropping the location from the prod
config would silently return production to sequential scans.

Search (`LOWER(title) LIKE '%term%'`) is deliberately untouched — a leading wildcard
cannot use a B-tree. `pg_trgm` is the fix and it is a decision, not a migration. Written
up at the end of `docs/ADMIN_AUTH_OPTIONS.md`. Not urgent below a few thousand products.

## The error-dispatch fix — why every admin 400 looked like a 403

Spring registers the security filter chain for the ERROR dispatch as well as REQUEST, so
`anyRequest().denyAll()` also rejected the internal re-dispatch to `/error` and replaced
whatever status the application had decided. Reproduced against a running stack, and it
was worse than previously recorded: a bad enum value in an admin request was resolved as
**400 in the access log and arrived at the client as a 403 with an empty body**. A wrong
HTTP verb did the same; a missing upload came back 401.

Fixed in two halves: permit `DispatcherType.ERROR` (scoped to the dispatcher type, which
no client can request, rather than to the `/error` path), and handle the exceptions that
were reaching `/error` at all — `HttpMessageNotReadableException` now names the offending
field and lists the accepted enum values.

The pre-existing handler test runs with `addFilters = false`, which is precisely why none
of this was ever caught. `ErrorDispatchSecurityTest` keeps the filters on.

---

## Known issues found but deliberately not fixed

- **Create responses return `null` timestamps.** `POST /api/admin/products` returns
  `createdAt`/`updatedAt` as null although the database has them set — the DTO is built
  before the flush. Cosmetic. `OrderService` avoids it with `saveAndFlush`; `ProductService`
  still has it.
- **Long product titles truncate in the cart** ("Agarbatt…"). Pre-existing CSS, noticed
  during the browser run, out of scope for this branch.

---

## Testing notes that matter

- **H2 hides Postgres bugs.** During A8 a null `search` parameter produced
  `function lower(bytea) does not exist` on Postgres while **all tests stayed green on
  H2**. It also cannot express partial indexes (A14). Verify schema and query work
  against real Postgres.
- **Slice tests hide security bugs.** `@WebMvcTest` with `addFilters = false` — which is
  most of the suite — never builds the real filter chain. That is how the 403 above
  survived a green build for months.
- **Testcontainers does not currently work on this machine.** Docker Desktop enforces a
  minimum API version of 1.40; docker-java requests 1.32 and gets HTTP 400, which
  Testcontainers misreports as "Could not find a valid Docker environment". **Not in the
  build.** Options: downgrade Docker Desktop, use Colima/Rancher, run Testcontainers in
  CI only (recommended), or drop it.
- **The working alternative** is a throwaway Postgres container on a shifted port with the
  packaged jar pointed at it. Used for every verification on this branch and it caught
  three real problems the suite missed.
- **Never bind 5432 or 8080** in verification stacks; the user runs Postgres and the
  backend locally. 55432 and 18080 were used here.
- **CORS will bite a verification stack on an unusual port.** Running Vite on 15173 made
  every write return **403**, which looks exactly like a security bug and is not — the
  origin simply is not in `cors.allowed-origin.dev` (5173). Confirmed identical on the
  pre-existing reviews endpoint before changing anything. Pass
  `CORS_ALLOWED_ORIGIN_PROD=http://localhost:<port>` to the verify backend.
- **`timeout` does not exist on macOS.** Background the process and `kill` instead.
- **Always `mvn clean verify`** — stale `target/` output has already produced one false failure.
- **MinIO makes the S3 path locally verifiable.** `minio/mc` needs `--entrypoint /bin/sh`.
- **To drive the frontend against a throwaway backend**, the Vite proxy target is
  hardcoded to 8080 in `vite.config.ts`. A temporary config inside `frontend/` (it must
  live there to resolve `node_modules`) run with `npx vite --config` works — delete it
  afterwards. Playwright is not a project dependency; install it in a scratch directory
  rather than adding it to `package.json`.
- **Mutation-test the tests that matter.** Three deliberate breakages were introduced into
  the cart handoff to check the new tests caught them. One did not, and the test was
  strengthened. A test that passes against broken code is worse than no test.
