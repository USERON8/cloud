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
- Mixed legacy cache usage still exists for:
  - admin service
  - merchant service
  - merchant-auth service
  - statistics service
  - some async and file upload related invalidation paths

## Authorization Rules

- Uses OAuth2 JWT resource server mode
- Scopes follow the unified `resource:action` format
- Administrative endpoints require `ROLE_ADMIN` plus the matching scope
- Internal Dubbo endpoints use `/internal/user/**`

## What Was Changed In The Current Sync Round

- Replaced previous partial user-info cache implementation with explicit Redis single-level cache service.
- Replaced previous partial user-address cache implementation with explicit Redis single-level cache service.
- Updated `UserServiceImpl` and `UserAddressServiceImpl` to use those cache services directly.
- Added and updated targeted tests for user and address cache services.

## Known Findings In This Sync

- `user-service` still has mixed cache styles and is not yet fully unified.
- A large amount of Spring Cache annotation-based behavior still remains outside the user-info and user-address paths.
- `CacheManager` is still directly referenced by several user-related implementations, so the service currently has both explicit Redis cache services and annotation-driven cache behavior.
- Further cleanup should continue in admin, merchant, merchant-auth, statistics, async warmup, and file-upload related paths.

## Local Run

```bash
mvn -pl user-service spring-boot:run
```
