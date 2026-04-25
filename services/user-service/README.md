# User Service
Version: 1.1.0

User domain service for consumers, merchants, merchant verification, administrators, and related profile data.

- Service name: `user-service`
- Port: `8082`
- Database bootstrap: `db/init/user-service/init.sql`
- Test data: `db/test/user-service/test.sql`

## Responsibilities

- Owns user profile, address, merchant, merchant-auth, and admin domain data.
- Provides the main CRUD and query APIs for those domains.
- Supplies user-domain data that is later aggregated by governance routes.
- Publishes notification-related messages and cooperates with `auth-service` on identity data.

## HTTP Surface

- User self-service: `/api/users/me/**`
- Address: `/api/users/{userId}/addresses/**`, `/api/addresses/**`
- Merchant: `/api/merchants/**`
- Merchant authentication: `/api/merchants/{merchantId}/authentication/**`, `/api/merchant-authentications/**`
- Admin accounts: `/api/admins/**`
- Admin support controllers also exist for `/api/admin/users/**`, `/api/admin/statistics/**`, `/api/admin/thread-pools/**`, and `/api/admin/notifications/**`

## Runtime Notes

- When traffic enters through `gateway`, the preferred public admin routes for statistics, thread pools, notifications, and admin-user governance are owned by `governance-service`.
- Business cache paths use explicit Redis single-level services for user, address, merchant, merchant-auth, and statistics reads.
- Avatar upload and related file handling depend on MinIO integration.
- This service remains the source of truth for user-domain data even when the public admin surface is aggregated elsewhere.

## Local Run

```bash
mvn -pl user-service spring-boot:run
```
