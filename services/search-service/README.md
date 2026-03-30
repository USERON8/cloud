# Search Service
Version: 1.1.0

Search service responsible for product and shop discovery, recommendations, and suggestion features.

- Service name: `search-service`
- Port: `8087`
- Primary dependencies: Elasticsearch, Redis, and Nacos

## Responsibilities

- Provides product search, shop search, suggestions, hot keywords, keyword recommendations, and filtered search views.
- Maintains Elasticsearch-backed query endpoints for product and shop discovery.
- Consumes upstream product synchronization messages and XXL full-index rebuild jobs.
- Maintains Redis-based hot keyword ranking and today hot-selling product ranking.

## Core Endpoints

- Product search: `/api/search/**`
- Shop search: `/api/search/shops/**`

## Current Confirmed Cache Model

- Redis single-level hot data cache:
  - hot keyword list
  - today hot-selling product id list
- Redis single-level cache inside `ElasticsearchOptimizedService`:
  - smart search results
  - search suggestions
  - hot keywords
  - keyword recommendations

## Data Sources And Background Jobs

- This service does not maintain an independent MySQL bootstrap script
- Product data is synchronized from upstream services into Elasticsearch, with MQ-based incremental sync and XXL full rebuild already connected
- Redis-backed hot data caches and search cache policies are configured in `application.yml`
- Hot keyword DB synchronization uses `scheduled` mode by default; switch to XXL with `SEARCH_HOT_DB_SYNC_TRIGGER_MODE=xxl`

## What Was Changed In The Current Sync Round

- Added explicit Redis hot-data cache service for:
  - hot keyword list
  - today hot-selling product id list
- Removed stale dev-only `cache.multi-level` config from `application-dev.yml`
- Removed stale smart-search, suggestion, hot-keyword, and recommendation L1 config from `application.yml`
- Removed local L1 caches for hot keywords and keyword recommendations in `ElasticsearchOptimizedService`
- Removed remaining local L1 caches for smart search results and search suggestions in `ElasticsearchOptimizedService`
- Removed the now-unused Caffeine dependency and refresh-executor config
- Updated this README to match the actual implementation

## Known Findings In This Sync

- `search-service` hot-data and keyword/search cache paths are now aligned to explicit Redis single-level cache.
- Cache invalidation for hot keywords is still interval-driven, so correctness still depends on TTL plus invalidation timing rather than strong consistency.
- If the team later wants stronger freshness for search-result cache, the next step should be explicit invalidation events tied to product index updates.

## Local Run

```bash
mvn -pl search-service spring-boot:run
```
