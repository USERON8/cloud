# Cloud Shop Microservices

简化版电商微服务项目，后端基于 Spring Boot + Spring Cloud Alibaba，前端为 Vue 3 + TypeScript。

## 模块与端口

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `gateway` | `80` | 统一网关，转发 `/api/**`、`/auth/**` |
| `auth-service` | `8081` | OAuth2/JWT 认证与 GitHub 登录 |
| `user-service` | `8082` | 用户、商家、管理员、资料与地址 |
| `order-service` | `8083` | 订单与退款 |
| `product-service` | `8084` | 商品与分类 |
| `stock-service` | `8085` | 库存与库存变更 |
| `payment-service` | `8086` | 支付与支付宝接口 |
| `search-service` | `8087` | Elasticsearch 搜索 |
| `my-shop-web` | `5173`(dev) | Web/Android/iOS 前端 |

## 快速启动

1. 启动基础依赖：

```bash
docker compose -f docker/docker-compose.yml up -d mysql redis nacos namesrv broker nginx minio elasticsearch
```

2. 初始化数据库（先 `init` 再 `test`，可选）：见 `db/README.md`。

3. 构建后端：

```bash
mvn -T 1C clean package -DskipTests
```

4. 启动后端服务（任选其一）：

```bash
# 方式1：快速拉起主链路
powershell -File .tmp/acceptance/start-services.ps1

# 方式2：按模块单独启动
mvn -pl gateway spring-boot:run
```

5. 构建前端并部署到 Nginx 静态目录：

```bash
pnpm --dir my-shop-web install
pnpm --dir my-shop-web build
```

将 `my-shop-web/dist` 内容拷贝到 `docker/docker-compose/nginx/html/` 后重启 `nginx` 容器。

## 常用入口

- 前端首页：`http://127.0.0.1:18080`
- 网关 API 文档：`http://127.0.0.1:18080/doc.html`
- Nacos：`http://127.0.0.1:18080/nacos`
- RocketMQ Dashboard：`http://127.0.0.1:38082`
- Kibana：`http://127.0.0.1:5601`

## 目录说明

- `db/`：初始化、测试数据与归档 SQL
- `docker/`：容器与基础设施配置
- `tests/perf/k6/`：主链路压测脚本
- `docs/`：运维与排障文档
