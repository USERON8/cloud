# User Service
Version: 1.1.0

User domain service covering consumers, merchants, administrators, and merchant verification workflows.

- Service name: `user-service`
- Port: `8082`
- Database bootstrap: `db/init/user-service/init.sql`
- Test data: `db/test/user-service/test.sql`

## Core Endpoint Groups

- User profile: `/api/user/profile/**`
- User address: `/api/user/address/**`
- User query: `/api/query/users/**`
- User management: `/api/manage/users/**`
- Merchant management: `/api/merchant/**`
- Merchant verification: `/api/merchant/auth/**`
- Administration: `/api/admin/**`
- Statistics and thread pools: `/api/statistics/**`, `/api/thread-pool/**`

## Authorization Rules

- Uses OAuth2 JWT resource server mode
- Scopes follow the unified `resource:action` format
- Administrative endpoints require `ROLE_ADMIN` plus the matching scope
- Internal Dubbo endpoints use `/internal/user/**`

## Local Run

```bash
mvn -pl user-service spring-boot:run
```
