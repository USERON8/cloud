# Search Service
Version: 1.1.0

Search and discovery service for products, shops, suggestions, and recommendation endpoints.

- Service name: `search-service`
- Port: `8087`
- Primary dependencies: Elasticsearch, Redis, MySQL, Nacos

## Responsibilities

- Serves product search, shop search, suggestions, and recommendation APIs.
- Maintains Elasticsearch-backed product and shop documents.
- Tracks hot keywords and hot-selling products in Redis.
- Rebuilds or refreshes search documents from upstream product, category, and stock signals.

## HTTP Surface

- Product search: `/api/search/products/**`
- Shop search: `/api/search/shops/**`
- Shop detail: `/api/shops/{shopId}`

## Runtime Notes

- This service has no dedicated business bootstrap SQL under `db/init`; search data is built from Elasticsearch indexes and upstream sync inputs.
- Hot keywords and today-popular product ids use Redis single-level cache.
- Search result optimization inside the service also relies on Redis rather than local L1 cache.
- Index freshness depends on upstream sync signals plus scheduled rebuild paths.

## Local Run

```bash
mvn -pl search-service spring-boot:run
```
