# UniApp API Guide

Updated: 2026-04-22

Source of truth:

- API callers under `my-shop-uniapp/src/api`
- session handling in `my-shop-uniapp/src/auth/session.ts`
- role handling in `my-shop-uniapp/src/auth/permission.ts`

## Runtime Rules

- Default public entry: `http://127.0.0.1:18080`
- `src/api/http.ts` attaches the current bearer token automatically when a session exists.
- Frontend identity comes from JWT claims normalized in `src/auth/session.ts`; backend authorization still remains authoritative.
- Frontend must never send `X-Internal-*`, `X-Signature`, `X-Timestamp`, or `X-Nonce`.
- Anonymous browse is centered on `/api/search/**` and `/api/shops/**`. Most other modules require an authenticated session.

## API Module Map

| Module | Route groups | Current role |
| --- | --- | --- |
| `src/api/auth.ts` | `/oauth2/**`, `/auth/users/register`, `/auth/sessions`, `/auth/tokens/validate`, `/auth/oauth2/github/**` | Login, registration, logout, token exchange, GitHub OAuth |
| `src/api/auth-tokens.ts` | `/auth/authorizations/**`, `/auth/blacklist-entries/**`, `/auth/cleanups/**` | Admin token governance |
| `src/api/user.ts` | `/api/users/me/**` | Profile, password, avatar |
| `src/api/address.ts` | `/api/users/{userId}/addresses/**`, `/api/addresses/**` | Address management |
| `src/api/merchant.ts` | `/api/merchants/**` | Merchant CRUD, review, status, statistics |
| `src/api/merchant-auth.ts` | `/api/merchants/{merchantId}/authentication/**`, `/api/merchant-authentications/**` | Merchant verification and file upload entrypoints |
| `src/api/admin.ts` | `/api/admins/**` | Admin account management |
| `src/api/user-management.ts` | `/api/admin/users/**` | Admin user search and batch operations |
| `src/api/statistics.ts` | `/api/admin/statistics/**` | Dashboard statistics |
| `src/api/thread-pool.ts` | `/api/admin/thread-pools/**` | Thread-pool monitoring |
| `src/api/stock.ts` | `/api/admin/stocks/ledger/{skuId}` | Stock ledger read |
| `src/api/category.ts` | `/api/categories/**` | Category tree and category management |
| `src/api/product.ts` | `/api/products`, `/api/spus`, `/api/search/products/**` | Product browse and search-facing product reads |
| `src/api/product-catalog.ts` | `/api/spus/**`, `/api/skus`, `/api/categories/{categoryId}/spus` | Catalog management |
| `src/api/search-ops.ts` | `/api/search/products/**` | Product search, filters, suggestions, hot keywords |
| `src/api/shop-search.ts` | `/api/search/shops/**`, `/api/shops/{shopId}` | Shop search and discovery |
| `src/api/cart.ts` | `/api/users/me/cart/**` | Remote cart |
| `src/api/order.ts` | `/api/orders/**`, `/api/after-sales/**` | Order lifecycle and after-sale |
| `src/api/payment.ts` | `/api/payment-orders/**`, `/api/payment-refunds/**` | Payment orders, checkout sessions, status, refunds |

## Current Frontend Chains

### Authentication and session

1. Frontend redirects the browser to `GET /oauth2/authorize`.
2. Token exchange happens through `POST /oauth2/token`.
3. `src/auth/session.ts` decodes JWT claims and stores:
   - access token
   - token type
   - expiry time
   - normalized user roles
4. `src/auth/permission.ts` reduces role handling to `USER`, `MERCHANT`, or `ADMIN`.

### Cart checkout

1. `src/api/cart.ts` reads or updates `/api/users/me/cart`.
2. The server returns the remote cart `id`.
3. `src/api/order.ts` sends `POST /api/orders` with:
   - `cartId`
   - `clientOrderId`
   - header `Idempotency-Key`

The frontend should always use the server-issued cart identifier instead of rebuilding checkout data from local cart state alone.

### Payment flow

1. `src/api/payment.ts` creates or reads a payment order.
2. The frontend calls `POST /api/payment-orders/{paymentNo}/checkout-sessions`.
3. The UI opens `session.checkoutPath`, which resolves to `GET /api/payment-checkouts/{ticket}`.
4. The app polls `GET /api/payment-orders/{paymentNo}/status` until the payment reaches a terminal state.

Frontend code should not construct checkout URLs manually. The checkout ticket endpoint returns raw HTML and is intentionally not wrapped in `Result<T>`.

### Admin and governance flow

- Admin pages use gateway-facing routes such as `/api/admin/users/**`, `/api/admin/statistics/**`, and `/api/admin/thread-pools/**`.
- The frontend does not call `/internal/governance/**` directly.
- Governance and auth-token admin functions still stay on public admin routes exposed by `gateway`.

## Upload and Security Notes

- Avatar upload uses `resolveApiUrl('/api/users/me/avatar')` together with the current bearer token.
- Merchant-auth file uploads use the route templates declared in `src/api/merchant-auth.ts`.
- Frontend code must not attempt to emulate internal service headers or gateway HMAC signatures.

## References

- Backend route ownership: `docs/backend-api.md`
- Backend runtime rules: `docs/backend-runtime.md`
- Postman collection: `docs/postman/cloud-shop.postman_collection.json`
