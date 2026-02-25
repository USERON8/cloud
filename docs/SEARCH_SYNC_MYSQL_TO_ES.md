# MySQL -> Elasticsearch Sync (Logstash)

## Goal

Synchronize product data from MySQL `product_db` into Elasticsearch `product_index` through Logstash.

## Current Flow

1. `product-service` writes product data to MySQL only.
2. `logstash` reads incremental rows from MySQL with JDBC.
3. `logstash` upserts documents into Elasticsearch `product_index`.

## Removed Legacy Flow

The previous MQ-based search sync producer/consumer flow has been removed:

- Removed producer endpoint: `POST /api/product/search-sync/full`
- Removed `SEARCH_EVENTS_TOPIC` producer/consumer chain
- Removed search-service event consumer for product sync

## Operation Notes

- Ensure `docker/monitor/logstash/drivers/mysql-connector-j-9.3.0.jar` exists.
- Start sync container:

```bash
docker compose -f docker/docker-compose.yml up -d --build logstash
```

- Verify Elasticsearch documents in `product_index`.
