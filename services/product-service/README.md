# Product Service
Version: 1.1.0

Product domain service providing product, category, and batch operation endpoints.

- Service name: `product-service`
- Port: `8084`
- Database bootstrap: `db/init/product-service/init.sql`
- Test data: `db/test/product-service/test.sql`

## Responsibilities

- Owns product, SKU, SPU, and category domain data.
- Provides product read/write interfaces to frontend and other services.
- Supplies product data to `search-service` document building and synchronization flows.
- Acts as a major upstream for stock, search, and order display information.

## Core Endpoints

- Products: `/api/product/**`
- Categories: `/api/category/**`
- Internal access: `/internal/product/**`

## Current Design Notes

- `product-service` is a core read-heavy domain service and a natural candidate for multi-level caching on hot product detail paths.
- Product detail now keeps its dedicated explicit multi-level cache implementation in `ProductDetailCacheService`.
- Category tree and shop query caches now use explicit Redis cache services instead of `Spring Cache` annotations.
- Search document synchronization depends on product data shape staying stable.

## Known Findings In This Sync

- Product detail still uses local Caffeine + Redis hash storage, which matches the service's current hot-detail multi-level cache goal.
- Category tree and shop read/query/statistics cache paths have been moved to explicit Redis cache services, and `ProductApplication` no longer depends on `@EnableCaching`.
- If cache cleanup continues later, the next product-side review target should be whether category and shop caches also need a local L1 layer for very high read traffic.

## Local Run

```bash
mvn -pl product-service spring-boot:run
```
