# 风险总表（按等级）

## P0
1. 全服务 `testenv` 绕过鉴权能力可导致全放行  
证据：`gateway/src/main/resources/application-testenv.yml:3`、`auth-service/src/main/resources/application-testenv.yml:3`、`user-service/src/main/resources/application-testenv.yml:3`、`product-service/src/main/resources/application-testenv.yml:3`、`order-service/src/main/resources/application-testenv.yml:3`、`payment-service/src/main/resources/application-testenv.yml:3`、`stock-service/src/main/resources/application-testenv.yml:3`、`search-service/src/main/resources/application-testenv.yml:3`，以及 `common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:72`

## P1
1. gateway 黑名单校验 fail-open  
证据：`gateway/src/main/java/com/cloud/gateway/config/ResourceServerConfig.java:191`、`:193`
2. auth-service 默认允许动态生成 JWT 密钥  
证据：`auth-service/src/main/resources/application.yml:114`
3. auth-service 默认 client secret 回退硬编码  
证据：`auth-service/src/main/resources/application.yml:67`、`:80`
4. payment-service 当前无法通过全量编译（Alipay SDK 构造签名不匹配）  
证据：`payment-service/src/main/java/com/cloud/payment/config/AlipayConfig.java:49`
5. 交易域消息消费者在异常分支释放幂等锁，存在重复副作用窗口  
证据：`order-service/src/main/java/com/cloud/order/messaging/OrderMessageConsumer.java:76`、`payment-service/src/main/java/com/cloud/payment/messaging/PaymentMessageConsumer.java:80`、`stock-service/src/main/java/com/cloud/stock/messaging/StockMessageConsumer.java:129`
6. 基础设施默认口令/弱口令  
证据：`docker/docker-compose.yml:22`、`:107`、`:236`，`docker/monitoring-compose.yml:48`
7. Elasticsearch 安全关闭  
证据：`docker/docker-compose.yml:254`

## P2
1. 公共资源服务 CORS 允许任意来源且允许凭据  
证据：`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:58`、`:59`
2. 多服务 `/actuator/**` 对匿名开放  
证据：`common-module/src/main/java/com/cloud/common/config/BaseResourceServerConfig.java:115`，`gateway/src/main/java/com/cloud/gateway/config/ResourceServerConfig.java:78`
3. 前端将 access/refresh token 存于 localStorage  
证据：`my-shop-web/src/auth/session.ts:30`、`:31`、`:42`、`:43`
4. 搜索服务文档与当前同步策略描述不一致  
证据：`search-service/README.md:7`、`:17`
5. 测试覆盖对关键交易一致性场景不足  
证据：当前仅 11 个 Java 测试文件，缺少交易链路集成回归

## P3
1. gateway CSP 含 `unsafe-inline` 与 `unsafe-eval`  
证据：`gateway/src/main/resources/application.yml:73`
2. 前端路由角色控制仅为 UX 层，需文档明确非安全边界  
证据：`my-shop-web/src/router/index.ts:97`

