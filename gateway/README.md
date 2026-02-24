# Gateway

统一入口网关，负责路由、JWT 资源服务校验和统一跨域处理。

- 服务名：`gateway`
- 端口：`8080`
- 依赖：Nacos、Redis、auth-service

## 当前路由前缀

- `/auth/**`、`/oauth2/**`、`/.well-known/**` -> `auth-service`
- `/api/manage/users/**`、`/api/query/users/**`、`/api/user/**`、`/api/merchant/**`、`/api/admin/**`、`/api/statistics/**` -> `user-service`
- `/api/product/**`、`/api/category/**` -> `product-service`
- `/api/orders/**`、`/api/v1/refund/**` -> `order-service`
- `/api/payments/**`、`/api/v1/payment/alipay/**` -> `payment-service`
- `/api/stocks/**` -> `stock-service`
- `/api/search/**` -> `search-service`

## CORS 说明

网关已开启 `DedupeResponseHeader`，避免重复 `Access-Control-Allow-Origin` 导致浏览器拦截。

## 本地启动

```bash
mvn -pl gateway spring-boot:run
```
