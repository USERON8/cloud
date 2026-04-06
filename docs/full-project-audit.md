# Full Project Audit

Audit date: `2026-04-06`

Scope:
- UniApp pages under `my-shop-uniapp/src/pages/app`
- Frontend API modules under `my-shop-uniapp/src/api`
- Service controllers under `services/*-service/src/main/java`
- Current docs and Postman assets under `docs/*`

## Closed Front-to-Back Chains

- Catalog browse and keyword search pages use the documented category, product, and search endpoints.
- Address book page uses the backend address CRUD endpoints and owner-scoped address access rules.
- Merchant center uses merchant profile, merchant auth, merchant statistics, and license upload endpoints.
- Admin center uses merchant review, merchant listing, user query, admin listing, and statistics overview endpoints.
- Operations center now compiles against the current admin toolchain APIs for token management, category maintenance, catalog maintenance, search utilities, shop search, payment helpers, statistics, and thread-pool inspection.
- Cart checkout, order creation, order query, payment lookup, and refund lookup remain aligned with the previously closed transaction chain audit.
- Profile page now calls `/api/user/profile/current`, `/api/user/profile/current/password`, and `/api/user/profile/current/avatar` directly instead of rendering JWT claim snapshots only.

## Documentation Sync In This Round

- `docs/backend-api.md`
- `docs/frontend-api.md`

## High-Signal Findings Resolved In This Round

### 1. Profile page was session-only

- Previous behavior: `my-shop-uniapp/src/pages/app/profile/index.vue` rendered session claims only and did not exercise the backend profile APIs already documented.
- Current behavior: the page refreshes current profile data from the backend, supports profile updates, password changes, and avatar upload.

### 2. Broader operational API coverage was under-documented

- Authentication docs now call out admin token-management endpoints under `/auth/tokens/*`.
- Frontend docs now state that the admin workspace consumes statistics and thread-pool monitor endpoints, and that auth-token operations are admin-only tools.

### 3. Operations workspace had broken API imports

- Previous behavior: `my-shop-uniapp/src/pages/app/ops/index.vue` referenced token, category, product-catalog, and thread-pool helpers that were not imported from the frontend API modules, leaving the page in a compile-time broken state.
- Current behavior: the page imports the full current helper set from `src/api/auth-tokens.ts`, `src/api/category.ts`, `src/api/product-catalog.ts`, and `src/api/thread-pool.ts`, and the H5 frontend build succeeds again.

### 4. Search and shop docs drifted from the real frontend API surface

- Previous behavior: `src/api/shop-search.ts` exposed a `searchAfter` field that the backend shop-search request model does not accept, and the frontend API guide did not list `src/api/shop-search.ts`, `src/api/product-catalog.ts`, or the full set of public search and shop endpoints already wrapped by the frontend.
- Current behavior: the fake `searchAfter` field is removed, and the frontend API guide now points to the real module boundaries and public endpoint surface for search, filters, shop discovery, and product-catalog maintenance.

## Current Boundary

- The main user-facing and operational pages present in the UniApp app now have traced backend contracts and synced documentation.
- This audit does not claim exhaustive browser-level execution for every page against live services in one environment.
- Environment-bound integrations, including payment callback runtime wiring and external object storage reachability, remain deployment prerequisites rather than repository contract gaps.
