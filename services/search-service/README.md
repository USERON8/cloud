# Search Service
Version: 1.1.0

Search service responsible for product and shop discovery, recommendations, and suggestion features.

- Service name: `search-service`
- Port: `8087`
- Primary dependencies: Elasticsearch, Redis, and Nacos

## Core Endpoints

- Product search: `/api/search/**`
- Shop search: `/api/search/shops/**`

## Notes

- This service does not maintain an independent MySQL bootstrap script
- Product data is synchronized from upstream services into Elasticsearch, with MQ-based incremental sync and XXL full rebuild already connected
- Built-in L1/L2 caches and hot keyword refresh policies are configured in `application.yml`
- Hot keyword DB synchronization uses `scheduled` mode by default; switch to XXL with `SEARCH_HOT_DB_SYNC_TRIGGER_MODE=xxl`

## Local Run

```bash
mvn -pl search-service spring-boot:run
```
