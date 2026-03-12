# Gateway
Version: 1.1.0

统一入口网关，负责路由、JWT 资源服务校验和统一跨域处理。

- 服务名：`gateway`
- 端口：`8080`
- 依赖：Nacos、Redis、auth-service

## 当前路由前缀

- `/auth/**`、`/oauth2/**`、`/.well-known/**` -> `auth-service`
- `/api/manage/users/**`、`/api/query/users/**`、`/api/user/**`、`/api/merchant/**`、`/api/admin/**`、`/api/statistics/**` -> `user-service`
- `/api/product/**`、`/api/category/**` -> `product-service`
- `/api/v2/**` -> `order-service`
- `/api/payments/**`、`/api/v1/payment/alipay/**` -> `payment-service`
- `/api/stocks/**` -> `stock-service`
- `/api/search/**` -> `search-service`

## CORS 说明

网关已开启 `DedupeResponseHeader`，避免重复 `Access-Control-Allow-Origin` 导致浏览器拦截。

## 限流与缓存

- Sentinel 已启用，默认保护 v2 路由（默认阈值 `80 QPS / 1s`，可通过环境变量覆盖）
- 搜索回退缓存：`SearchFallbackCache` 按路由/参数分级 TTL 缓存搜索回退响应
  - 配置前缀：`app.search.fallback.cache`

## 本地启动

```bash
mvn -pl gateway spring-boot:run
```
