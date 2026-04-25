# Governance Service
Version: 1.1.0

Admin aggregation and governance service for operational routes that should not stay inside individual business services.

- Service name: `governance-service`
- Port: `8088`
- Primary dependencies: Nacos, Dubbo, `auth-service`, `user-service`, `stock-service`

## Responsibilities

- Exposes admin-facing governance routes through `gateway`.
- Aggregates token governance, MQ governance, Outbox governance, and observability redirects.
- Owns admin statistics, thread-pool reads, notification operations, and the public stock ledger entry.
- Provides the internal namespace `/internal/governance/**` for compatibility and operational routing.

## Public Surface Through Gateway

- Auth governance:
  - `/auth/authorizations/**`
  - `/auth/blacklist-entries/**`
  - `/auth/cleanups/**`
- Admin governance:
  - `/api/admin/statistics/**`
  - `/api/admin/thread-pools/**`
  - `/api/admin/users/**`
  - `/api/admin/mq/**`
  - `/api/admin/outbox/**`
  - `/api/admin/observability/**`
  - `/api/admin/notifications/**`
  - `/api/admin/stocks/ledger/**`
- Compatibility proxy:
  - `/api/admin/governance/**` -> `/internal/governance/**`

## Runtime Notes

- This service is the preferred public admin entry for governance-style operations.
- Some underlying data still originates from domain services, but the route boundary is centralized here.
- Direct access to port `8088` is mainly useful for isolated debugging; normal traffic should enter through `gateway`.

## Local Run

```bash
mvn -pl governance-service spring-boot:run
```
