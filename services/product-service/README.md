# Product Service
Version: 1.1.0

Product domain service for products, categories, SPUs, SKUs, and catalog management.

- Service name: `product-service`
- Port: `8084`
- Database bootstrap: `db/init/product-service/init.sql`
- Test data: `db/test/product-service/test.sql`

## Responsibilities

- Owns product, SKU, SPU, and category data.
- Serves product browse and catalog-management APIs.
- Provides upstream product data for search indexing and stock display.
- Emits product and category changes that feed downstream search synchronization.

## HTTP Surface

- Products: `/api/products`
- Categories: `/api/categories/**`
- Catalog management: `/api/spus/**`, `/api/skus`, `/api/categories/{categoryId}/spus`

## Runtime Notes

- Hot product detail reads keep the dedicated multi-level cache path in `ProductDetailCacheService`.
- Category and shop-oriented caches use explicit Redis services.
- Product and category writes follow post-commit eviction plus delayed double delete.
- Search-side document rebuilding depends on this service staying the source of truth for product and category shape.

## Local Run

```bash
mvn -pl product-service spring-boot:run
```
