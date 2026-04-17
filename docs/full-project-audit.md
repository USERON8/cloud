# Full Project Audit

Audit date: `2026-04-17`

Scope:
- Gateway route ownership and security boundary
- Governance aggregation and operations surface split
- Core business services under `services/*-service`
- Shared security and messaging modules under `common-parent/*`
- Current reference docs under `docs/*`

## Executive Summary

- The public authentication boundary is now effectively centralized at `gateway`, with trusted internal identity propagation and downstream HMAC verification in place.
- A dedicated `governance-service` now owns the main non-merchant operational surfaces: statistics, thread-pool inspection, user/admin governance, token governance, MQ governance, outbox governance, notification operations, stock ledger reads, and Grafana governed entrypoints.
- Merchant-domain admin paths remain intentionally hosted in `user-service` for this phase, including merchant certification and shop certification flows.
- Order and payment have completed the key RPC-driven refactor path and no longer rely on ad-hoc internal HTTP for their core business interaction.
- The repository now compiles successfully with `mvn -DskipTests compile`.

## Service Audit

### gateway

Status:
- Completed for the current phase

Findings:
- Public JWT validation remains at gateway and is now paired with internal identity header propagation.
- Governance-owned admin paths are routed to `governance-service` with higher priority than the old business-service routes.
- Merchant-domain admin/auth paths are intentionally preserved on `user-service`.

Current governance-owned routes:
- `/api/admin/statistics/**`
- `/api/admin/thread-pool/**`
- `/api/admin/manage/users/**`
- `/api/admin/query/users/**`
- `/api/admin/mq/**`
- `/api/admin/outbox/**`
- `/api/admin/observability/**`
- `/api/app/user/notification/**`
- `/auth/tokens/**`
- `/api/admin/stocks/ledger/**`

Intentional non-migration routes:
- `/api/admin/merchant/**`
- `/api/admin/merchant/auth/**`

### governance-service

Status:
- Completed as the governance aggregation module for this phase

Findings:
- Internal governance aggregation is stable under `/internal/governance/**`.
- Admin governance entrypoints are stable under the routed admin shapes and `/api/admin/governance/**`.
- MQ dead-letter handling, outbox backlog inspection, single-event requeue, bounded batch requeue, and Grafana governed entry are all centralized here.
- Grafana now supports governed redirect entrypoints with dashboard whitelist enforcement.

Current limitations:
- Grafana integration is still redirect-based, not reverse-proxy and not SSO.
- Governance is a dedicated module/service in the repository, but not a separately isolated deployment program or operations platform.

### user-service

Status:
- Partially migrated

Findings:
- Merchant ownership has been normalized onto `merchant.owner_user_id`.
- User statistics, thread-pool inspection, admin management, user query/manage, and notification operations are now exported to governance through Dubbo or internal governance paths.
- Merchant management and merchant certification remain intentionally hosted here.

Intentional retained scope:
- Merchant profile management
- Merchant certification
- Shop certification review chain
- Merchant-facing REST surfaces

Audit concern still open:
- Merchant-facing controllers and services should continue to be reviewed for any remaining compatibility assumptions that blur merchant id and owner user id.

### auth-service

Status:
- Completed for the current governance scope

Findings:
- Token governance capabilities are exported to `governance-service`.
- Admin token tooling now has both governance-owned admin paths and internal governance paths.
- No additional governance split is currently required here beyond integration verification.

### stock-service

Status:
- Partially migrated

Findings:
- Stock ledger admin entry has been moved to `governance-service`.
- `stock-service` now primarily acts as the RPC provider for ledger reads and the internal-scope provider for inventory mutation endpoints.

Intentional retained scope:
- Internal stock mutation endpoints
- Service-local stock governance support paths

### order-service

Status:
- Completed for the key RPC refactor scope

Findings:
- Core order/payment interaction is RPC-based.
- Compatibility endpoints still exist in some REST shapes, but direct pay mutation is intentionally blocked in business logic.
- The main remaining work here is compatibility cleanup, not architecture correction.

### payment-service

Status:
- Completed for the key payment-chain refactor scope

Findings:
- Payment orders, callback logs, outbox relay, and refund handling reflect the new payment domain shape.
- Redis usage remains constrained to idempotency, rate limiting, and short-lived helper state.
- Verified payment confirmation is the only path to close the order payment state machine.

### product-service

Status:
- Stable, not a governance migration target

Findings:
- Merchant ownership checks now depend on canonical merchant ownership from `user-service`.
- No governance migration is required beyond standard API and ownership consistency.

### search-service

Status:
- Stable, not a governance migration target

Findings:
- Search remains a public/business capability outside governance migration scope.
- Gateway fallback and public search paths are still correctly documented as non-governance surfaces.

### common-security

Status:
- Completed for the current security refactor scope

Findings:
- Shared internal request headers, signer, and downstream authentication filter are in place.
- Downstream bearer-token bypass for trusted public JWT traffic remains supported as designed.

### common-messaging

Status:
- Completed for the current operations refactor scope

Findings:
- Shared outbox governance endpoints now exist behind each service.
- Single-event and bounded batch requeue are both available for governance aggregation.
- MQ governance support remains layered on service-local infrastructure endpoints plus governance aggregation.

## Documentation Sync In This Round

- `docs/backend-api.md`
- `docs/backend-rpc-refactor-plan.md`
- `docs/frontend-api.md`
- `docs/observability-stack.md`
- `docs/project-closeout.md`

## Scope Decisions Frozen In This Phase

- Merchant admin paths remain in `user-service`.
- Merchant certification and shop certification remain in `user-service`.
- Grafana remains a governed redirect target, not reverse-proxy and not SSO.
- Governance migration in this phase targets operational/admin tooling, not all business REST surfaces.

## Remaining Work

### Real remaining items

- Run end-to-end verification for gateway -> governance-service -> business-service chains in an integration environment.
- Continue merchant-domain ownership audit for any leftover compatibility shortcuts.
- Revisit compatibility-era public REST shapes and remove only the ones no longer required by frontend or operations clients.

### Explicitly not in this phase

- Moving merchant certification or shop certification into `governance-service`
- Turning Grafana into an SSO or reverse-proxy surface
- Eliminating every business REST endpoint in favor of RPC-only internal topology

## Final Assessment

- If the acceptance bar is "has the governance and security refactor landed in code": yes.
- If the acceptance bar is "has every operational/admin capability outside the merchant domain been centralized": mostly yes.
- If the acceptance bar is "has the original plan been finished with no remaining cleanup or future-phase decisions": not fully; the remaining items are now mostly boundary confirmation, compatibility cleanup, and environment validation rather than missing core architecture work.
