# User Service

User domain service for consumer, merchant, and admin capabilities.

- Service name: `user-service`
- Default port: `8082`
- Main dependencies: MySQL, Redis, Nacos, OAuth2 Resource Server
- Optional dependency: MinIO (avatar upload)

## Scope

This service provides:

- User profile and address APIs
- Merchant and merchant-auth APIs
- Admin and internal Feign APIs
- User statistics APIs
- Thread pool monitoring APIs
- User notification APIs

## Security Model

OAuth2 JWT is required for protected APIs.

- User-facing APIs:
  - `isAuthenticated()` for profile and address operations
- Admin-facing APIs:
  - `ROLE_ADMIN` + admin scopes
  - `SCOPE_admin:read` for read endpoints
  - `SCOPE_admin:write` for write endpoints
- Internal APIs:
  - `SCOPE_internal_api`

Scope format is unified to `resource:action` (colon).

## API Groups

### 1) Admin APIs
Base path: `/api/admin`

- `GET /api/admin`
- `GET /api/admin/{id}`
- `POST /api/admin`
- `PUT /api/admin/{id}`
- `DELETE /api/admin/{id}`
- `PATCH /api/admin/{id}/status`
- `POST /api/admin/{id}/reset-password`

### 2) User Management APIs
Base path: `/api/manage/users`

- `PUT /api/manage/users/{id}`
- `POST /api/manage/users/delete`
- `POST /api/manage/users/deleteBatch`
- `POST /api/manage/users/updateBatch`
- `POST /api/manage/users/updateStatusBatch`

### 3) User Query APIs
Base path: `/api/query/users`

- `GET /api/query/users`
- `GET /api/query/users/search`
- `GET /api/query/users/findByGitHubId`
- `GET /api/query/users/findByGitHubUsername`
- `GET /api/query/users/findByOAuthProvider`

### 4) User Profile APIs
Base path: `/api/user/profile`

- `GET /api/user/profile/current`
- `PUT /api/user/profile/current`
- `PUT /api/user/profile/current/password`
- `POST /api/user/profile/current/avatar` (multipart form-data, field: `file`)

### 5) User Address APIs
Base path: `/api/user/address`

- `POST /api/user/address/add/{userId}`
- `PUT /api/user/address/update/{addressId}`
- `DELETE /api/user/address/delete/{addressId}`
- `GET /api/user/address/list/{userId}`
- `GET /api/user/address/default/{userId}`
- `POST /api/user/address/page`
- `DELETE /api/user/address/deleteBatch`
- `PUT /api/user/address/updateBatch`

### 6) User Notification APIs
Base path: `/api/user/notification`

All endpoints require: `ROLE_ADMIN` + `SCOPE_admin:write`.

- `POST /api/user/notification/welcome/{userId}`
- `POST /api/user/notification/status-change/{userId}`
- `POST /api/user/notification/batch`
- `POST /api/user/notification/system`

### 7) Merchant APIs
Base path: `/api/merchant`

- `GET /api/merchant`
- `GET /api/merchant/{id}`
- `POST /api/merchant`
- `PUT /api/merchant/{id}`
- `DELETE /api/merchant/{id}`
- `POST /api/merchant/{id}/approve`
- `POST /api/merchant/{id}/reject`
- `PATCH /api/merchant/{id}/status`
- `GET /api/merchant/{id}/statistics`
- `DELETE /api/merchant/batch`
- `PATCH /api/merchant/batch/status`
- `POST /api/merchant/batch/approve`

### 8) Merchant Auth APIs
Base path: `/api/merchant/auth`

- `POST /api/merchant/auth/apply/{merchantId}`
- `GET /api/merchant/auth/get/{merchantId}`
- `DELETE /api/merchant/auth/revoke/{merchantId}`
- `POST /api/merchant/auth/review/{merchantId}`
- `GET /api/merchant/auth/list`
- `POST /api/merchant/auth/review/batch`

### 9) Statistics APIs
Base path: `/api/statistics`

- `GET /api/statistics/overview`
- `GET /api/statistics/overview/async`
- `GET /api/statistics/registration-trend`
- `GET /api/statistics/registration-trend/async`
- `GET /api/statistics/type-distribution`
- `GET /api/statistics/status-distribution`
- `GET /api/statistics/active-users`
- `GET /api/statistics/growth-rate`
- `GET /api/statistics/activity-ranking`
- `POST /api/statistics/refresh-cache`

### 10) Thread Pool APIs
Base path: `/api/thread-pool`

- `GET /api/thread-pool/info`
- `GET /api/thread-pool/info/detail?name={beanName}`

### 11) Internal APIs

#### Internal user APIs
Base path: `/internal/user`

- `GET /internal/user/username/{username}`
- `GET /internal/user/id/{id}`
- `POST /internal/user/register`
- `PUT /internal/user/update`
- `GET /internal/user/password/{username}`
- `GET /internal/user/github-id/{githubId}`
- `GET /internal/user/oauth`
- `POST /internal/user/github/create`
- `PUT /internal/user/github/update/{userId}`

#### Internal admin APIs
Base path: `/admin`

- `GET /admin/query/getById/{id}`
- `GET /admin/query/getAll`
- `POST /admin/manage/create`
- `PUT /admin/manage/update/{id}`
- `DELETE /admin/manage/delete/{id}`

## Notification Delivery Provider

Notification service now uses a pluggable delivery provider.

- Default provider: Redis
- Config key: `user.notification.delivery-provider`
- Default value: `redis`

Current Redis provider stores notification payloads in Redis keys.
This keeps API behavior stable and allows future extension to email/SMS providers.

## Local Run

### 1) Start dependencies

```bash
cd docker
docker compose up -d mysql redis nacos
```

Optional for avatar:

```bash
docker compose up -d minio
```

### 2) Start service

```bash
cd user-service
mvn spring-boot:run
```

### 3) Verify health

```bash
curl http://localhost:8082/actuator/health
```

## Testing

Run service tests:

```bash
mvn -pl user-service -am test -DskipITs
```

## Notes

- `docker/` SQL scripts are not modified by this service documentation.
- Keep role/scope assignments in auth-service aligned with these endpoint requirements.
