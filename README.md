# Cloud Shop Microservices

简化版电商微服务项目，后端基于 Spring Boot + Spring Cloud Alibaba，前端为 Vue 3 + TypeScript。

## 模块与端口

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `gateway` | `8080` | 统一网关，转发 `/api/**`、`/auth/**` |
| `auth-service` | `8081` | OAuth2/JWT 认证与 GitHub 登录 |
| `user-service` | `8082` | 用户、商家、管理员、资料与地址 |
| `order-service` | `8083` | 订单与退款 |
| `product-service` | `8084` | 商品与分类 |
| `stock-service` | `8085` | 库存与库存变更 |
| `payment-service` | `8086` | 支付与支付宝接口 |
| `search-service` | `8087` | Elasticsearch 搜索 |
| `my-shop-web` | `5173`(dev) | Web/Android/iOS 前端 |

## 快速启动

1. 启动基础依赖（含端口占用清理）：

```bash
powershell -File scripts/dev/start-containers.ps1
# Linux/macOS:
# bash scripts/dev/start-containers.sh
```

2. 初始化数据库（先 `init` 再 `test`，可选）：见 `db/README.md`。

3. 构建后端：

```bash
mvn -T 1C clean package -DskipTests
```

4. 启动后端服务（含端口占用清理）：

```bash
powershell -File scripts/dev/start-services.ps1
# Linux/macOS:
# bash scripts/dev/start-services.sh
```

说明：服务交互参数统一由 Nacos `common.yaml` 配置中心下发，不依赖启动脚本外部注入。

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
- MinIO Console：`http://127.0.0.1:19001`
- Elasticsearch：`http://127.0.0.1:19200`
- Kibana：`http://127.0.0.1:15601`
- Prometheus：`http://127.0.0.1:19099`
- Grafana：`http://127.0.0.1:13000`

## 搜索接入策略

- 前端统一调用网关搜索入口：`/api/search/**`。
- 网关对以下接口启用 700ms 超时降级：`/api/search/smart-search`、`/api/search/search`、`/api/search/suggestions`。
- 当 `search-service` 超时或异常时，网关自动回退到 `product-service` 只读接口，不需要前端做多服务兜底。
- 关键词推荐建议优先调用：
  - `/api/search/suggestions`
  - `/api/search/hot-keywords`
  - `/api/search/keyword-recommendations`

## 目录说明

- `db/`：初始化、测试数据与归档 SQL
- `docker/`：容器与基础设施配置
- `tests/perf/k6/`：主链路压测脚本
- `docs/`：运维与排障文档
