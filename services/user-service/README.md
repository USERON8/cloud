# User Service
Version: 1.1.0

User domain service covering consumers, merchants, administrators, and merchant verification workflows.

- Service name: `user-service`
- Port: `8082`
- Database bootstrap: `db/init/user-service/init.sql`
- Test data: `db/test/user-service/test.sql`

## Responsibilities

- Owns user profile, address, merchant, merchant-auth, and admin domain data.
- Exposes query, management, profile, statistics, notification, and internal Dubbo interfaces.
- Coordinates with `auth-service` for principal and authority synchronization.
- Publishes user-notification related messages through RocketMQ.

## Core Endpoint Groups

- User profile: `/api/user/profile/**`
- User address: `/api/user/address/**`
- User query: `/api/query/users/**`
- User management: `/api/manage/users/**`
- Merchant management: `/api/merchant/**`
- Merchant verification: `/api/merchant/auth/**`
- Administration: `/api/admin/**`
- Statistics and thread pools: `/api/statistics/**`, `/api/thread-pool/**`

## Current Confirmed Cache Model

- Redis single-level cache already implemented explicitly for:
  - user basic info
  - user address data
  - admin service
  - merchant service
  - merchant-auth service
  - statistics service
  - async user refresh and file upload invalidation paths

## Authorization Rules

- Uses OAuth2 JWT resource server mode
- Scopes follow the unified `resource:action` format
- Administrative endpoints require `ROLE_ADMIN` plus the matching scope
- Internal Dubbo endpoints use `/internal/user/**`

## What Was Changed In The Current Sync Round

- Replaced previous partial user-info cache implementation with explicit Redis single-level cache service.
- Replaced previous partial user-address cache implementation with explicit Redis single-level cache service.
- Replaced legacy cache invalidation in async refresh and avatar upload flows with explicit Redis cache updates.
- Replaced admin, merchant, merchant-auth, and statistics service cache usage with explicit Redis single-level cache services.
- Removed remaining `Spring Cache` dependency from `UserServiceImpl` and aligned user write paths with explicit Redis cache refresh and eviction.
- Added and updated targeted tests for the user, address, admin, merchant, merchant-auth, and statistics cache paths.

## Known Findings In This Sync

- Business cache paths in `user-service` are now unified to explicit Redis single-level cache services.
- `UserApplication` still enables Spring caching at framework level, but current user-domain business flows no longer depend on annotation-driven cache behavior.
- Future cache work in this service should prefer extending the existing explicit cache services instead of reintroducing `CacheManager` or `@Cacheable` style annotations.

## Local Run

```bash
mvn -pl user-service spring-boot:run
```
