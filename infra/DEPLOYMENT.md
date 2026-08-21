# Deployment

Reference for deploying the Liams Creations catalog. Nothing here runs itself — every
AWS resource is created and applied by a human.

## Topology: single origin

One CloudFront distribution is the only public hostname. The SPA is the default
origin; three path patterns are routed to the ALB in front of the backend.

```
                    ┌──────────────────────┐
   customer ───────▶│  CloudFront (HTTPS)  │
                    └───────┬──────────────┘
                            │
        default ────────────┼──────────▶ S3 bucket (SPA build output)
        /uploads/*  ────────┼──────────▶ S3 bucket (product photos)
        /api/*      ────────┤
        /sitemap.xml ───────┼──────────▶ ALB ──▶ backend container ──▶ RDS Postgres
```

Because everything shares one origin, the browser never makes a cross-origin request
and CORS is not load-bearing in production. The CORS config stays in place for local
development and as a safety net.

### CloudFront behaviours

| Path pattern   | Origin | Cache | Notes |
|----------------|--------|-------|-------|
| `/api/*`       | ALB    | **Disabled** | Must forward `Authorization`, and all methods (GET/POST/PUT/PATCH/DELETE). Caching these will serve one customer's data to another. |
| `/uploads/*`   | **S3 (photos)** | Enabled, long TTL | Product photos, written by the backend and read straight from the bucket — they do not pass through the ALB. Filenames are UUIDs and never rewritten, and the backend sets a one-year immutable `Cache-Control`. |
| `/sitemap.xml` | ALB    | Short TTL | Fixes the storefront 404 described in A6. |
| `default (*)`  | S3     | Enabled | SPA assets. |

**SPA routing:** add custom error responses mapping **403 → `/index.html` (200)** and
**404 → `/index.html` (200)** so client-side routes like `/product/PRD-001` resolve on
a hard refresh. Without this, deep links break.

> The `/api/*` caching rule is the single most dangerous setting in this document. A
> cached API response is a data leak between customers, not just a stale page.

## Environment variables

Cross-checked against `application-prod.properties` and `application.properties`.

### Required — the app will not start or will misbehave without these

| Variable | Example | Notes |
|----------|---------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` | **Activates everything below.** If unset, the app silently falls back to dev defaults, including admin/admin123. |
| `DB_URL` | `jdbc:postgresql://catalog.xxxx.rds.amazonaws.com:5432/instagram_catalog` | |
| `DB_USERNAME` | `catalog_user` | |
| `DB_PASSWORD` | — | From Secrets Manager. Never in a task definition literal. |
| `ADMIN_USERNAME` | — | Must be set. The app refuses to start without it. |
| `ADMIN_PASSWORD` | — | From Secrets Manager. **Enforced at startup** by `AdminCredentialsValidator`: at least 12 characters, not a well-known default such as `admin123`, and not equal to `ADMIN_USERNAME`. The app refuses to start otherwise — see below. |
| `CORS_ALLOWED_ORIGIN_PROD` | `https://shop.example.com` | The CloudFront domain. Scheme included, no trailing slash. |
| `PUBLIC_BASE_URL` | `https://shop.example.com` | Used to build absolute sitemap URLs. **Defaults to `http://localhost:5173`** — if unset, the live sitemap advertises localhost URLs to Google. |
| `S3_BUCKET` | `liams-catalog-photos` | Bucket holding product photos. No default — the app will not start without it, which is deliberate: uploads that silently vanish on the next deploy are worse than a failed boot. |
| `S3_REGION` | `ap-south-1` | Bucket region. |

### If the container will not start and the logs say "Refusing to start"

That is `AdminCredentialsValidator` (A13) doing its job, not a bug. The message names
the variable to fix. It runs before Flyway opens a database connection, so this failure
appears on its own without a database stack trace on top of it.

Note that the guard only applies when `SPRING_PROFILES_ACTIVE=prod`. A deployment that
forgets that variable gets dev defaults *including* `admin/admin123`, and nothing will
complain — which is why `SPRING_PROFILES_ACTIVE` is first in the table above.

### Photo bucket setup

The backend writes photos but never serves them, so the bucket has to be readable by
CloudFront. Two things to get right:

1. **Do not make the bucket public.** Use CloudFront Origin Access Control and a bucket
   policy that allows only that distribution to read. The backend needs
   `s3:PutObject` and `s3:DeleteObject` on `<bucket>/products/*` through its task role
   — not access keys in environment variables.
2. **Turn on versioning** if you want a deleted photo to be recoverable. Removing an
   image from a product deletes the object, and that is not undoable otherwise.

### Optional

| Variable | Default | Notes |
|----------|---------|-------|
| `TRUSTED_PROXY_COUNT` | `2` | Proxies in front of the app: CloudFront + ALB. Set to `1` if CloudFront is removed. Wrong value either collapses all visitors into one rate-limit bucket or lets clients forge the address we key on. |
| `APP_STORAGE` | `s3` in prod | `local` writes into the container and is **ephemeral** — dev only. |
| `APP_UPLOADS_DIR` | `uploads` | Container-local path. Only used when `APP_STORAGE=local`. |
| `S3_KEY_PREFIX` | `products/` | Key prefix inside the photo bucket. |
| `S3_PUBLIC_URL_PREFIX` | `/uploads/` | Relative on purpose, so no CDN domain is baked into the database. Only set this to an absolute CDN origin if you accept that changing that domain later invalidates every stored image URL. |

## Deploy sequence

Order matters: the database must accept the new schema before new code depends on it.

1. **CI green.** `mvn clean verify` and the frontend lint + build both pass on the commit.
2. **Back up the database.** Take an RDS snapshot and wait for it to complete. This is
   the rollback point for anything the migration does.
3. **Deploy the backend.** Flyway runs migrations automatically at startup, against
   `classpath:db/migration` only — demo seed data cannot reach production.
4. **Verify the backend** before touching the frontend:
   - `GET /actuator/health` returns `{"status":"UP"}`
   - `GET /api/products` returns a page (empty catalog is expected on first deploy)
   - Log in to admin and create one product end to end.
5. **Build and publish the SPA.** `npm ci && npm run build` in `frontend/`, sync `dist/`
   to the S3 bucket.
6. **Invalidate CloudFront** for `/*`. Skipping this serves the old JavaScript against
   the new API, which is the classic "works for me, broken for customers" deploy.
7. **Smoke test on the real domain:** load the storefront, open a product, confirm an
   uploaded image renders, submit a review, and fetch `/sitemap.xml`.

## Rollback

**Application code** — redeploy the previous container image and re-invalidate
CloudFront. Fast, and safe as long as the schema did not change.

**Database schema** — Flyway migrations here are forward-only; there are no `down`
scripts. If a migration is the problem, restore the snapshot from step 2. This is why
step 2 is not optional.

**The asymmetry to plan around:** rolling application code back is minutes, rolling the
schema back is a restore with data loss back to the snapshot. So prefer additive
migrations — add a column, backfill, switch reads, drop later in a separate release —
rather than anything destructive in the same deploy as the code that needs it.

## Known gaps at time of writing

These are tracked and not yet done:

- **No HTTPS/HSTS enforcement** in the app itself (A15).
- **No automated database backups** configured beyond the manual snapshot above (A15).
- **No log aggregation or uptime monitoring** wired up (A15). `RequestLoggingFilter`
  already emits a per-request correlation id, so the groundwork exists.
- **Admin auth sends a reusable password** on every request (A12, pending a decision).
- **No privacy policy** despite the cart collecting name, phone, email and address (A15).
