---
name: react-frontend
description: Conventions for the React 19 + TypeScript + Vite frontend — TanStack Query for server state, React Router 7, component/hook structure, and typed API access. Use when adding pages, components, hooks, data fetching, or routing in frontend/src.
---

# React Frontend (frontend/)

Stack: React 19, TypeScript (strict), Vite 8, React Router 7, TanStack Query 5,
ESLint flat config. Source under `frontend/src` with folders: `api/`, `components/`,
`context/`, `hooks/`, `pages/` (+ `pages/admin/`), `utils/`, `assets/`.
Scripts: `npm run dev`, `npm run build` (runs `tsc -b` then `vite build`), `npm run lint`.

## Server state = TanStack Query, not useEffect

- Fetch/cache all server data with `useQuery`/`useMutation`. Do **not** roll your own
  `useEffect` + `useState` fetching.
- **API functions live in `src/api/`** (`products.ts`, `admin.ts`, `reviews.ts`) and are
  thin + typed. Public reads use plain `fetch`; admin/authenticated calls go through the
  `adminRequest<T>()` helper in `api/admin.ts` (attaches the Basic token, centralizes
  401 → logout and error-message extraction from `body.message`). Reuse these — don't
  hand-roll `fetch` with auth headers in components.
- **Query hooks live in `src/hooks/`** (`useProducts`, `useReviews`, `useTitle`), not
  inline in components. Follow the existing key shapes:
  - list: `['products', { status, category, page, search, sort, onSale }]`
  - single: `['product', productNumber]`
  - reviews: `['reviews', productNumber, page]`; summaries: `['rating-summaries', key]`
- Paginated lists use `placeholderData: keepPreviousData` so the current page stays
  visible while the next loads. Guard dependent queries with `enabled: Boolean(id)`.
- Set `staleTime` for slow-changing data (rating summaries use `60_000`).
- Invalidate precisely in mutation `onSuccess` (`queryClient.invalidateQueries`).

## Components

- Function components + hooks only. One component per file; PascalCase filename
  matching the export.
- Type props with an explicit `type`/`interface`; no implicit `any`. Prefer discriminated
  unions over boolean-flag soup.
- Keep components presentational where possible; push data/logic into hooks (`src/hooks/`)
  or query hooks.
- Co-locate a component's styles with it; keep shared primitives in `components/`.

## Routing (React Router 7)

- Routes are defined in `App.tsx`. Lazy-load heavy/admin routes with `React.lazy` +
  `Suspense` to keep the initial bundle small.
- Admin routes (`pages/admin/`) are guarded by the `RequireAdmin` component backed by
  `AdminAuthContext`; redirect unauthenticated users to the login page rather than
  rendering a blank screen. Reuse `RequireAdmin` for any new admin route.

## State

- Server state → TanStack Query. App/UI state → React Context in `src/context/`:
  `AdminAuthContext` (admin session; Basic token in **sessionStorage** key `lc-admin-auth`,
  cleared on tab close and on any 401) and `CartContext`. Don't put server data in Context.
- Keep local component state local. Reach for Context only when prop-drilling is real.

## TypeScript & quality

- Strict mode is on — fix type errors, don't `// @ts-ignore` them.
- Mirror backend DTO shapes as TS types in `src/api/` so the contract is explicit.
- `npm run lint` and `npm run build` must both pass before finishing.

## Loading, error, empty states

- Every data view handles all three: loading (skeleton/spinner), error (message +
  retry), and empty (friendly "nothing here yet"). See the `ui-ux` skill.

## Checklist

1. Data fetched via TanStack Query with typed API function and stable keys.
2. Mutations invalidate the right query keys.
3. Loading / error / empty states handled.
4. Props typed, no `any`, no ignored TS errors.
5. `npm run lint` and `npm run build` pass.
