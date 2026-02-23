# 环境配置矩阵（dev/prod）

## 基础原则

- 所有服务默认 `spring.profiles.active=dev`。
- 生产环境使用启动参数或环境变量覆盖为 `prod`。
- 交易链路服务（`order-service`、`payment-service`、`stock-service`）必须激活 `rocketmq` 绑定配置。

## 服务级矩阵

| 服务 | 默认 active | include | 关键依赖 | 说明 |
|---|---|---|---|---|
| gateway | `dev` | `route` | Nacos, Redis(可选), Auth JWK | 网关路由文件在 `application-route.yml` |
| auth-service | `dev` | - | Nacos, RocketMQ, JWT Key | 提供 OAuth2/JWK |
| user-service | `dev` | - | Nacos, MySQL, Redis, RocketMQ | 用户查询与管理 |
| product-service | `dev` | - | Nacos, MySQL, Redis, RocketMQ | 发送商品搜索同步事件 |
| search-service | `dev` | - | Nacos, Elasticsearch, Redis, RocketMQ | 消费 `SEARCH_EVENTS_TOPIC` |
| order-service | `dev` | `rocketmq` | Nacos, MySQL, Redis, RocketMQ | 下单后发布 `order-created` |
| payment-service | `dev` | `rocketmq` | Nacos, MySQL, Redis, RocketMQ | 消费 `order-created`，支付成功后发布 `payment-success` |
| stock-service | `dev` | `rocketmq` | Nacos, MySQL, Redis, RocketMQ | 预占/确认扣减/回滚 |

## 核心环境变量

| 变量 | 默认值 | 用途 |
|---|---|---|
| `NACOS_SERVER_ADDR` | `localhost:8848` | 配置中心与服务发现 |
| `ROCKETMQ_NAME_SERVER` | `127.0.0.1:39876` | 消息中间件地址 |
| `AUTH_JWK_SET_URI` | `http://127.0.0.1:8081/.well-known/jwks.json` | 资源服务 JWT 验签 |
| `AUTH_ISSUER_URI` | `http://127.0.0.1:8081` | JWT issuer 校验 |
| `SERVER_PORT` | 各服务默认端口 | 服务端口覆盖 |

## 交易链路消息绑定（必须生效）

- `order-service`: `orderCreatedProducer-out-0`、`paymentSuccessConsumer-in-0`、`stockFreezeFailedConsumer-in-0`
- `payment-service`: `orderCreatedConsumer-in-0`、`paymentSuccessProducer-out-0`
- `stock-service`: `orderCreatedConsumer-in-0`、`paymentSuccessConsumer-in-0`、`stockFreezeFailedProducer-out-0`

## 商品搜索同步消息绑定（必须生效）

- `product-service`: `search-producer-out-0`（topic=`SEARCH_EVENTS_TOPIC`）
- `search-service`: `searchConsumer-in-0`（subscription=`PRODUCT_CREATED||PRODUCT_UPDATED||PRODUCT_DELETED`）
