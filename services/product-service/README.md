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
- The repository currently contains a dedicated `ProductDetailCacheService`, which was not refactored in the current round.
- Search document synchronization depends on product data shape staying stable.

## Known Findings In This Sync

- No code changes were made here in the current round.
- Product detail caching still follows its own existing implementation and was not aligned to the newer single-level Redis rollout used for user/search hot data.
- If cache cleanup continues later, `product-service` is one of the next major modules that should get an explicit review.

## Local Run

```bash
mvn -pl product-service spring-boot:run
```
