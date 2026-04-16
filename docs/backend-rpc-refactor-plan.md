# Backend RPC Refactor Plan

Generated on: 2026-04-15
Scope: backend refactor execution status for RPC, security boundary, merchant ownership, payment chain, and governance split.

## Current Status Summary

### Completed

- Internal service-to-service RPC contracts are in place under `common-parent/common-api`.
- Order and payment internal calls now use Dubbo RPC instead of ad-hoc HTTP.
- Payment chain has been rebuilt around dedicated payment orders, callback logs, outbox relay, compensation, and idempotent cache-only security helpers.
- Gateway now propagates trusted internal identity headers after successful public JWT authentication.
- Downstream servlet services now accept trusted internal identity headers with HMAC verification and can authenticate internal requests without reusing the public bearer token.
- Legacy merchant ownership shortcut `userId == merchantId` has been removed from the common permission utility path.
- Added `governance-service` as the first dedicated governance aggregation module, using Dubbo to collect user statistics, thread-pool metrics, and stock ledger reads.
- `governance-service` now also aggregates token governance reads and control operations from `auth-service` through Dubbo RPC.
- `governance-service` now also aggregates admin management operations from `user-service` through Dubbo RPC.
- `governance-service` now also aggregates user query and batch user-management operations from `user-service` through Dubbo RPC.
- Gateway now exposes `/api/admin/governance/**` as the admin-compatible proxy to the governance aggregation surface.
- Gateway now also routes the governance-owned admin paths (`/api/admin/statistics/**`, `/api/admin/thread-pool/**`, `/api/admin/manage/users/**`, `/api/admin/query/users/**`, and `/auth/tokens/**`) to `governance-service` before business-service route matches.
- `GET /api/admin/stocks/ledger/{skuId}` is now routed to `governance-service` as the formal admin governance entrypoint instead of reaching `stock-service` directly.

### Partially Completed

- Merchant ownership model has moved to `merchant.owner_user_id`, and the main write paths already validate ownership through `user-service`, but some business APIs still expose merchant-facing REST surfaces directly.
- Gateway and service responsibilities are cleaner on the security side, but some operational and admin APIs are still hosted inside business services.
- Pure internal RPC is established on key chains, but the project still keeps many REST controllers that should eventually be reduced to public-edge responsibilities only.
- Internal governance aliases now exist for thread-pool monitor, user statistics, and stock ledger so trusted callers no longer need to depend on the public admin API shapes for those operations.

### Not Completed

- Governance and operations surfaces have not been split into an isolated service/module.
- `/api/admin/**` and several merchant/admin management APIs are still routed directly to business services through gateway.
- The repository still contains compatibility-era public REST shapes that should be revisited in a later cleanup phase.

## Architecture Target

### Public Edge

- Gateway validates public JWT.
- Gateway converts the authenticated principal into trusted internal headers.
- Gateway signs internal identity headers with shared HMAC.
- Downstream services restore identity from internal headers and no longer depend on forwarded bearer tokens for normal routed traffic.

### Internal Calls

- Internal business-to-business calls use Dubbo RPC.
- Internal HTTP should be reserved for infrastructure and controlled compatibility cases only.

## Security Model Refactor

### Implemented

- Added shared internal request header constants in `common-security`.
- Added shared HMAC signer in `common-security`.
- Added downstream internal request authentication filter in `common-security`.
- Registered the internal filter before bearer token authentication in the servlet resource server chain.
- Added gateway internal identity propagation filter that:
  - reads the authenticated JWT principal,
  - strips the external `Authorization` header before forwarding,
  - injects trusted internal identity headers,
  - signs them with HMAC.

### Remaining

- Introduce the same internal header trust model for any non-servlet internal services if new runtime types are added later.
- Review and reduce any public endpoints that were only kept for historical convenience.

## Gateway And Service Responsibility Split

### Implemented

- Gateway is now the effective public authentication boundary for normal routed authenticated traffic.
- Downstream services can authenticate trusted internal requests without re-consuming the public bearer token.

### Remaining

- Move admin, thread-pool, MQ governance, token ops, and other operational surfaces out of business services.
- Reduce direct gateway exposure of business-internal management endpoints.

## Pure RPC Internal Call Rules

### Implemented

- `order-service -> payment-service` uses `PaymentDubboApi`.
- `payment-service -> order-service` uses `OrderDubboApi`.

### Remaining

- Continue replacing any future internal HTTP business calls with RPC contracts.
- Keep public REST only for true edge-facing workflows.

## Merchant Ownership Model

### Implemented

- Canonical ownership remains `merchant.owner_user_id`.
- `user-service` ownership checks are used by merchant authorization and product write guards.
- Common permission utility no longer treats `merchantId` as equivalent to current `userId`.

### Remaining

- Continue auditing merchant-facing controllers and services for any compatibility assumptions that still blur merchant id and owner user id.

## Payment Chain

### Implemented

- Payment order creation verifies order ownership and payable amount through RPC.
- Payment callback verification persists callback logs and drives state transition through payment service logic.
- Payment outbox relay is in place for payment success and refund completion events.
- Payment Redis usage remains limited to idempotency, short-lived result/status caching, rate limiting, and checkout tickets.

## Governance Surface Split

### Implemented

- Added internal governance aliases for:
  - thread-pool monitor,
  - user statistics,
  - stock ledger reads.
- These aliases require internal trusted identity with `SCOPE_internal`.
- Gateway now recognizes and authorizes these internal governance aliases explicitly instead of forcing them through the public admin role gate.
- Internal governance aliases are now hosted by dedicated internal controllers, separating them from the public admin controller surface inside the business services.
- Added `/internal/governance/**` as the dedicated governance aggregation entrypoint backed by `governance-service`.
- Added admin governance aggregation under `/internal/governance/admins/**`, backed by `AdminGovernanceDubboApi`.
- Added user query and user batch-management governance aggregation under `/internal/governance/users/**`, backed by `UserAdminGovernanceDubboApi`.
- Added auth token governance aggregation under `/internal/governance/auth/tokens/**`, backed by `AuthGovernanceDubboApi`.
- Added `/api/admin/governance/**` at gateway as the preferred admin-facing compatibility proxy for governance operations, reducing the need for operators to reach business-service admin paths directly.
- Added governance-owned admin controller handling for statistics, thread-pool, admin management, user query, user batch management, and token governance paths directly in `governance-service`.
- Added governance-owned admin stock ledger handling in `governance-service`, with `stock-service` retained only as the RPC provider and internal-scope surface.

### Remaining

- Extract operational endpoints from:
  - MQ governance utilities.

## Database Refactor Notes

### Implemented

- `outbox_event` exists across the major business services.
- Payment tables already reflect the rebuilt payment domain model.
- Merchant table includes `owner_user_id`.

## Code-Level Change Log

### Added

- `common-security/InternalRequestHeaders.java`
- `common-security/InternalAuthenticatedPrincipal.java`
- `common-security/InternalRequestSigner.java`
- `common-security/InternalRequestAuthenticationFilter.java`
- `gateway/InternalIdentityPropagationFilter.java`
- `common-api/AuthGovernanceDubboApi.java`
- `common-api/AdminGovernanceDubboApi.java`
- `common-domain/vo/auth/*`
- `auth-service/AuthGovernanceService.java`
- `auth-service/AuthGovernanceDubboService.java`
- `user-service/AdminGovernanceDubboService.java`
- `governance-service/GovernanceAdminController.java`
- `governance-service/GovernanceStockAdminController.java`

### Updated

- `common-security/BaseResourceServerConfig.java`
- `common-security/SecurityAutoConfiguration.java`
- `common-security/SecurityPermissionUtils.java`
- `governance-service/GovernanceController.java`

## Recommended Next Steps

1. Split operational/admin APIs into a dedicated governance surface.
2. Shrink gateway exposure for business-internal management endpoints.
3. Audit merchant/business controllers for any remaining ownership compatibility shortcuts.
4. Re-run end-to-end verification for gateway -> service authentication and order/payment flows in an integration environment.
