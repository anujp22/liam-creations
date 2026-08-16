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
        /api/*      ────────┤
        /uploads/*  ────────┼──────────▶ ALB ──▶ backend container ──▶ RDS Postgres
        /sitemap.xml ───────┘
```

Because everything shares one origin, the browser never makes a cross-origin request
and CORS is not load-bearing in production. The CORS config stays in place for local
development and as a safety net.

### CloudFront behaviours

| Path pattern   | Origin | Cache | Notes |
|----------------|--------|-------|-------|
| `/api/*`       | ALB    | **Disabled** | Must forward `Authorization`, and all methods (GET/POST/PUT/PATCH/DELETE). Caching these will serve one customer's data to another. |
| `/uploads/*`   | ALB    | Enabled, long TTL | Filenames are UUIDs and never rewritten, so they are safe to cache aggressively. |
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
| `ADMIN_USERNAME` | — | |
| `ADMIN_PASSWORD` | — | From Secrets Manager. Must not be `admin123`; A13 adds a startup guard for this. |
| `CORS_ALLOWED_ORIGIN_PROD` | `https://shop.example.com` | The CloudFront domain. Scheme included, no trailing slash. |
| `PUBLIC_BASE_URL` | `https://shop.example.com` | Used to build absolute sitemap URLs. **Defaults to `http://localhost:5173`** — if unset, the live sitemap advertises localhost URLs to Google. |

### Optional

| Variable | Default | Notes |
|----------|---------|-------|
| `TRUSTED_PROXY_COUNT` | `2` | Proxies in front of the app: CloudFront + ALB. Set to `1` if CloudFront is removed. Wrong value either collapses all visitors into one rate-limit bucket or lets clients forge the address we key on. |
| `APP_UPLOADS_DIR` | `uploads` | Container-local path. **Ephemeral** — wiped on every deploy until S3 storage (A2) lands. |

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

- **Uploads are container-local** and wiped on every deploy (A2, S3 storage).
- **No HTTPS/HSTS enforcement** in the app itself (A15).
- **No automated database backups** configured beyond the manual snapshot above (A15).
- **No log aggregation or uptime monitoring** wired up (A15). `RequestLoggingFilter`
  already emits a per-request correlation id, so the groundwork exists.
- **Admin auth sends a reusable password** on every request (A12, pending a decision).
- **No privacy policy** despite the cart collecting name, phone, email and address (A15).
