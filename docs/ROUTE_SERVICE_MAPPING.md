# 路由与服务映射表

> 版本：v1（双前缀兼容）
> 兼容策略：保留旧前缀 1 个版本窗口，默认以 `/api/**` 为标准入口。

| 服务 | 网关 Route ID | 推荐入口（v2） | 兼容入口（legacy） | 服务端 Controller 前缀 |
|---|---|---|---|---|
| auth-service | `auth-service-api` | `/auth/**` | - | `/auth/**` |
| user-service | `user-service-api-v2` | `/api/manage/users/**` `/api/query/users/**` `/api/user/address/**` `/api/merchant/**` `/api/admin/**` `/api/statistics/**` `/api/thread-pool/**` | `/users/**` `/merchant/**` `/admin/**` | 与 v2 一致 |
| product-service | `product-service-api-v2` | `/api/product/**` `/api/category/**` | `/product/**` `/category/**` `/brand/**` | `/api/product/**` `/api/category/**` |
| order-service | `order-service-api-v2` | `/api/orders/**` `/api/v1/refund/**` | `/order/**` `/cart/**` | `/api/orders/**` `/api/v1/refund/**` |
| payment-service | `payment-service-api-v2` | `/api/payments/**` `/api/v1/payment/alipay/**` | `/payment/**` | `/api/payments/**` `/api/v1/payment/alipay/**` |
| stock-service | `stock-service-api-v2` | `/api/stocks/**` | `/stock/**` | `/api/stocks/**` |
| search-service | `search-service-api-v2` | `/api/search/**` | `/search/**` | `/api/search/**` |

## 文档路由

以下文档路由统一保留：

- `/auth-service/**`
- `/user-service/**`
- `/product-service/**`
- `/order-service/**`
- `/payment-service/**`
- `/stock-service/**`
- `/search-service/**`

包含 `doc.html`、`swagger-ui`、`v3/api-docs`、`swagger-resources`、`webjars`。

## 下线项

- 本轮已移除 `log-service` 在网关中的业务路由与文档路由。
