# Data ownership and snapshot contract

This document defines which service owns each business fact and how denormalized or JSON data may be used. It is a migration contract for the current schema, not a license for services to query each other's databases.

## Ownership boundaries

| Fact | Authoritative service/table | Other representations | Write rule |
|---|---|---|---|
| Login principal, status and credential version | User / `users` | JWT claims, Redis `auth:version:*` | Only User changes the principal. Auth signs a snapshot; consumers validate its version. |
| Roles and permissions | User / `roles`, `permissions`, relation tables | JWT claims, Redis authority cache | Only User changes RBAC. Cached claims are derived and disposable. |
| Admin and merchant profile | User / `admin`, `merchant`, `merchant_auth` | Product merchant name snapshots | Profile writes go through User. A profile references, but is not, the login principal. |
| Product and SKU definition | Product / normalized SPU, SKU, category, attribute and specification tables | Search documents, order and review snapshots | Only Product changes catalog facts. Other services consume APIs or events. |
| Inventory balance | Stock / stock tables and ledger | Availability projections | Only Stock changes quantity. Product does not own saleable balance. |
| Order commercial record | Order / main order, sub-order and item tables | `sku_snapshot` JSON | Order snapshots facts at purchase time and never refreshes historical presentation from Product. |
| Payment state | Payment / payment order and refund tables | Order payment status projection | Only Payment performs money-state transitions; Order updates its projection from idempotent events. |
| Search document | Search / Elasticsearch indices | MySQL sync snapshot tables | Search data is a rebuildable projection, never the recovery source for Product. |

## JSON and snapshot rules

- Product `spec_json` is a read model assembled from normalized specification rows. New write APIs must update normalized rows; the JSON value is regenerated in the same use case and is not accepted as an independent second source of truth.
- Search `attributes` is a flattened query projection. It can be deleted and rebuilt from Product events or a full replay.
- Order `sku_snapshot` captures the customer-visible SKU name, selected specifications, unit price and image used when the order was created. It is immutable except for an explicit data-repair operation with an audit record.
- Review `product_snapshot` captures display context at review creation time. It must not drive catalog, price or inventory decisions.
- JSON fields require a schema version when their shape changes incompatibly. Readers must tolerate the current and immediately previous version during rolling deployment.

## Referential integrity rules

- Foreign keys are required for relationships whose parent and child are owned by the same service database, unless an append-only ingestion path has a documented reason not to use one.
- IDs that cross service boundaries are references, not database foreign keys. Their validity is checked by the owning service API during synchronous commands or by an idempotent event-fed local projection.
- Cached names, prices and statuses must be named or documented as snapshots/projections. They are never silently promoted to authoritative fields.

## API rules

- Public write endpoints accept command DTOs rather than persistence entities.
- A batch command is either atomic or returns a per-item result. It must not report overall success after silently swallowing item failures.
- State changes use domain actions (approve, disable, ship, refund) when transitions have business rules; generic PATCH remains limited to profile-like data without a lifecycle transition.
- Every cross-service command carries an idempotency key when retrying could create duplicate financial, order or message records.
