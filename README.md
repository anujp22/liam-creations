# Liams Creations — Instagram Catalog

> A full-stack e-commerce catalog for a real Instagram-based business selling bridal
> sarees and wedding décor. Customers browse, filter, and review products and reach out
> to buy over WhatsApp; the owner runs the shop through a secured admin console.

<p>
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot 3.3" src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white">
  <img alt="React 19" src="https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black">
  <img alt="TypeScript" src="https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-Flyway-4169E1?logo=postgresql&logoColor=white">
  <img alt="Tests" src="https://img.shields.io/badge/tests-87%20passing-4c1">
</p>

I designed and built this project end to end — data model, REST API, security, the React
storefront, and the admin console — to demonstrate that I can ship a complete, production-
minded application rather than isolated features. The sections below walk through the
architecture and the engineering decisions behind it.

---

## Table of Contents
- [What This Project Demonstrates](#what-this-project-demonstrates)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [API Reference](#api-reference)
- [Engineering Decisions](#engineering-decisions)
- [Testing](#testing)
- [Running It Locally](#running-it-locally)
- [Configuration](#configuration)
- [Project Layout](#project-layout)

---

## What This Project Demonstrates

- **Full-stack ownership** — a typed React 19 SPA talking to a layered Spring Boot REST
  API, with a shared contract enforced through DTOs on both ends.
- **Secure-by-default API** — stateless HTTP Basic auth, role-based route authorization,
  BCrypt hashing, CORS locked to known origins, and per-client rate limiting.
- **Real data engineering** — a normalized PostgreSQL schema evolved through **12 Flyway
  migrations**, versioned in source control, with lazy/batched image loading to avoid N+1
  queries.
- **Production concerns** — externalized configuration via a `prod` Spring profile,
  container health checks, request correlation IDs in logs, and a swappable storage
  abstraction (local disk today, S3-ready).
- **Quality discipline** — **87 tests** across controllers, services, and repositories,
  covering the happy path plus auth, validation, and rate-limit edge cases.
- **Product thinking** — SEO (dynamic sitemap + JSON-LD structured data), privacy-friendly
  analytics, and a friction-free WhatsApp checkout that fits how the business actually sells.

---

## Tech Stack

| Layer | Technologies |
|-------|--------------|
| **Backend** | Java 17, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security, Bean Validation, Actuator |
| **Data** | PostgreSQL, Flyway migrations, Hibernate/JPA |
| **API hardening** | Bucket4j + Caffeine (rate limiting), BCrypt, CORS, request-logging filter |
| **Frontend** | React 19, TypeScript, Vite, React Router 7, TanStack React Query |
| **Testing** | JUnit 5, Spring Boot Test, Spring Security Test, H2 |
| **Ops / SEO** | Docker-ready health checks, dynamic `sitemap.xml`, JSON-LD, GoatCounter analytics |

---

## Architecture

```
                        ┌──────────────────────────────┐
   Browser  ───────────▶│  React 19 + Vite SPA          │
                        │  React Query · React Router    │
                        └───────────────┬───────────────┘
                                        │  JSON over HTTPS (CORS-scoped)
                                        ▼
        ┌───────────────────────────────────────────────────────────┐
        │  Spring Boot REST API                                      │
        │                                                            │
        │  Filters:  RateLimit (Bucket4j) → BasicAuth → RequestLog   │
        │  Web:      ProductController · ReviewController · Admin*    │
        │  Service:  ProductService · ReviewService  (business rules)│
        │  Data:     Spring Data JPA repositories                    │
        │  Cross-cutting: SecurityConfig · GlobalExceptionHandler    │
        │                 StorageService (local disk ▸ S3-ready)     │
        └───────────────────────────────┬───────────────────────────┘
                                        │  JDBC
                                        ▼
                          ┌──────────────────────────┐
                          │  PostgreSQL (Flyway)      │
                          └──────────────────────────┘
```

The backend follows a conventional **controller → service → repository** layering. Web
controllers stay thin and speak only in DTOs; services own the business rules (soft
delete, product-number generation, review moderation); repositories handle persistence.
Cross-cutting concerns — security, rate limiting, error mapping, storage — live in a
`shared` package so feature code stays focused.

---

## Features

### Storefront (public)
- Product grid with **category filter, keyword search, sort, and on-sale** views
- Product detail pages with an image gallery, sale pricing, and stock status
- Dedicated **Sale** and **Built on Request** pages
- Client-side **cart** and a **WhatsApp CTA** so buyers can message the owner in one tap
- **Customer reviews** with star ratings and an aggregate rating badge per product
- **SEO**: dynamic `sitemap.xml`, canonical tags, and `Product` JSON-LD structured data
- Optional **GoatCounter** analytics with pageview + conversion events

### Admin console (authenticated)
- HTTP Basic login behind guarded React routes
- Full **product CRUD** with soft-delete, restore, and permanent delete
- **Inventory & status** management (in stock / out of stock / built on request)
- **Featured** toggling and **on-sale** management
- **Review moderation** — approve / reject / delete with a live pending count
- **Metrics dashboard** summarizing the catalog

---

## API Reference

**Public** (no auth):

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/api/products` | Paginated list; filters: `status`, `category`, `search`, `onSale` |
| `GET`  | `/api/products/{productNumber}` | Single product |
| `GET`  | `/api/products/{productNumber}/reviews` | Product reviews |
| `POST` | `/api/products/{productNumber}/reviews` | Submit a review |
| `GET`  | `/api/reviews/summary` | Aggregate rating summary |
| `GET`  | `/sitemap.xml` | Dynamically generated sitemap |
| `GET`  | `/actuator/health` | Health probe |

**Admin** (HTTP Basic, `ROLE_ADMIN`):

| Method | Path | Description |
|--------|------|-------------|
| `GET`    | `/api/admin/me` · `/api/admin/metrics` | Identity & dashboard metrics |
| `POST` / `PUT` | `/api/admin/products` · `/{productNumber}` | Create / update product |
| `PATCH`  | `/api/admin/products/{productNumber}/status` · `/featured` | Update status / featured |
| `DELETE` | `/api/admin/products/{productNumber}` · `/permanent` | Soft / hard delete |
| `POST`   | `/api/admin/products/{productNumber}/restore` | Restore a soft-deleted product |
| `POST`   | `/api/admin/uploads` | Upload product image (≤ 8 MB) |
| `GET` / `PATCH` / `DELETE` | `/api/admin/reviews` · `/{id}` | Moderate reviews |

---

## Engineering Decisions

A few choices I want to call out, with the reasoning:

- **Flyway over `ddl-auto`.** Schema is code. Twelve ordered migrations mean the database
  is reproducible on any machine and every change is reviewable in a diff — `ddl-auto` is
  set to `none` deliberately.
- **Lazy + batched image loading.** Products have many images. An eager collection forces
  Hibernate into in-memory pagination (HHH000104); I made the collection lazy with
  `@BatchSize(30)`, so a full page of products loads its images in one extra query instead
  of N.
- **Public routes bypass the auth filter.** A custom `shouldNotFilter` on the Basic auth
  filter lets storefront GETs and review submissions through untouched, so a stale
  browser-cached credential can never break the public site — while admin routes still go
  through auth normally.
- **Rate limiting at the edge.** A Bucket4j filter runs before authentication, throttling
  abusive clients (and brute-force login attempts) before they reach business logic.
- **DTO-first boundaries.** Entities never leak to the wire; request/response DTOs plus
  Bean Validation keep the API contract explicit and the domain model free to change.
- **Storage behind an interface.** `StorageService` abstracts image persistence — local
  disk in dev, ready to swap for S3 in prod without touching controllers.
- **Configuration is externalized.** A `prod` profile pulls every secret (DB creds, admin
  password, CORS origin) from environment variables; nothing sensitive is committed.

---

## Testing

```bash
cd backend/catalog-backend
mvn clean test
```

**87 tests** spanning the stack:

- **Controller** tests with `MockMvc` and Spring Security Test — verifying status codes,
  validation errors, and that admin routes reject unauthenticated callers.
- **Service** tests covering business rules (soft delete, product-number generation,
  review moderation).
- **Repository** tests against H2 for query behavior.
- **Filter** tests asserting the rate limiter returns `429` once a client exceeds its
  bucket.

---

## Running It Locally

### Prerequisites
- JDK 17 · Node.js 18+ · PostgreSQL

### 1. Database
```sql
CREATE DATABASE instagram_catalog;
CREATE USER catalog_user WITH PASSWORD 'catalog_password';
GRANT ALL PRIVILEGES ON DATABASE instagram_catalog TO catalog_user;
```
Flyway applies all migrations and seeds sample products on first startup.

### 2. Backend
```bash
cd backend/catalog-backend
mvn spring-boot:run          # API on http://localhost:8080
```
Dev admin login (override in prod): `admin` / `admin123`.

### 3. Frontend
```bash
cd frontend
cp .env.example .env         # set VITE_OWNER_WHATSAPP, optional analytics URL
npm install
npm run dev                  # storefront on http://localhost:5173
```

---

## Configuration

Development defaults live in `application.properties`. For production, activate the `prod`
profile (`SPRING_PROFILES_ACTIVE=prod`) and provide secrets via environment variables:

| Variable | Purpose |
|----------|---------|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL connection |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Admin credentials |
| `CORS_ALLOWED_ORIGIN_PROD` | Allowed frontend origin (e.g. CloudFront domain) |
| `PUBLIC_BASE_URL` | Public URL used to build absolute sitemap links |
| `APP_UPLOADS_DIR` | Upload directory (or swap `StorageService` for S3) |

Frontend env vars (`frontend/.env`): `VITE_OWNER_WHATSAPP` (international format, no `+`)
and optional `VITE_GOATCOUNTER_URL`.

---

## Project Layout

```
instgram-catalog/
├── backend/catalog-backend/        # Spring Boot API (Maven)
│   └── src/main/java/.../catalog/
│       ├── product/                # catalog: model · repository · service · controllers · DTOs
│       ├── review/                 # customer reviews + moderation
│       ├── seo/                    # dynamic sitemap
│       └── shared/                 # security · rate limiting · storage · error handling
├── frontend/                       # React + Vite storefront and admin UI
│   └── src/                        # api · components · context · hooks · pages · utils
├── docs/                           # notes and architecture
└── infra/                          # deployment notes and configs
```

---

<sub>Built as an in-depth learning project to work through real production concerns — data
modeling, API security, testing, and SEO — on a full-stack Java + React codebase.</sub>
