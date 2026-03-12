# 2026-03-13 Code Review & Fix Log (Backend)

## Scope
- Services: `user-service`, `order-service`
- Category: business correctness, consistency, authorization, performance/stability
- Note: This document records review findings and the applied fix strategy. Tests were not run.

## Summary
- `user-service`: stats cache refresh, merchant auth cache eviction, default address transaction safety, username batch validation, stats query performance.
- `order-service`: role-based action control, idempotency key isolation, stock reservation compensation, timeout coverage, order number collision risk.

---

## user-service

### 1) Stats cache refresh ineffective (Medium)
- Risk: `/api/statistics/refresh-cache` hit `@Cacheable` and skipped recomputation.
- Fix strategy: explicitly recompute and repopulate `user:statistics` cache (overview, role/status distribution, active:7/30).
- Files:
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserStatisticsServiceImpl.java`
- Status: Fixed.

### 2) Merchant auth revoke left stale cache (Medium)
- Risk: cache entries remained after delete; stale data could be returned.
- Fix strategy: add `removeByMerchantId` with explicit eviction for `id:*` and `merchantId:*` cache keys; controller uses it.
- Files:
  - `services/user-service/src/main/java/com/cloud/user/service/MerchantAuthService.java`
  - `services/user-service/src/main/java/com/cloud/user/service/impl/MerchantAuthServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/controller/merchant/MerchantAuthController.java`
- Status: Fixed.

### 3) Default address reset across transactions (Medium)
- Risk: reset occurs before save/update; failure leaves no default address.
- Fix strategy: move reset into `save/updateById` so it is in the same transaction; remove controller-level reset.
- Files:
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserAddressServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/controller/user/UserAddressController.java`
- Status: Fixed.

### 4) Batch username existence check fails on blank values (Low)
- Risk: blank username triggers `BusinessException`, failing the batch.
- Fix strategy: skip blank values and add graceful fallback.
- Files:
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserAsyncServiceImpl.java`
- Status: Fixed.

### 5) Stats query performance (Low/Perf)
- Risk: Redis `SCAN` + N `GET`; registration trend computed with per-day DB queries.
- Fix strategy:
  - Use `multiGet` for Redis batch fetch.
  - Aggregate registration trend in a single query and fill missing dates.
- Files:
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserStatisticsServiceImpl.java`
  - `services/user-service/src/main/java/com/cloud/user/service/impl/UserAsyncServiceImpl.java`
- Status: Fixed.

---

## order-service

### 1) Role-based action control missing (High)
- Risk: user could trigger shipment/approval/refund actions without privilege.
- Fix strategy: enforce action allow-lists by role at controller level.
- Files:
  - `services/order-service/src/main/java/com/cloud/order/controller/OrderController.java`
- Status: Fixed.

### 2) Idempotency key not user-scoped (High)
- Risk: the same `Idempotency-Key` could return another user’s order.
- Fix strategy: prefix with `userId:`; require `userId` for admin-created orders.
- Files:
  - `services/order-service/src/main/java/com/cloud/order/controller/OrderController.java`
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderPlacementServiceImpl.java`
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`
- Status: Fixed.

### 3) Partial stock reservation without compensation (High)
- Risk: some SKU reservations succeed, others fail; reserved stock is not released.
- Fix strategy: record successful reservations and release them on failure.
- Files:
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderPlacementServiceImpl.java`
- Status: Fixed.

### 4) Timeout cancellation coverage (Medium)
- Risk: only `CREATED` orders are cancelled; `STOCK_RESERVED` could be left indefinitely.
- Fix strategy: include `STOCK_RESERVED` in timeout scanning and cancellation.
- Files:
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderTimeoutServiceImpl.java`
- Status: Fixed.

### 5) Order number collision risk (Medium)
- Risk: `System.currentTimeMillis` is collision-prone under high concurrency.
- Fix strategy: use UUID for main/sub order numbers.
- Files:
  - `services/order-service/src/main/java/com/cloud/order/service/impl/OrderServiceImpl.java`
- Status: Fixed.

---

## Tests
- Not executed in this round.
- Recommended regressions:
  - Order creation with stock reservation failure.
  - Timeout cancellation and stock release.
  - Role-based after-sale and sub-order actions.
  - Statistics endpoints and cache refresh.
