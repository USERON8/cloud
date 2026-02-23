# MySQL -> Search ES 全量同步说明

## 目标

将 `product-service` 的 MySQL 商品数据全量同步到 `search-service` 的 Elasticsearch 索引。

## 同步机制

1. 在 `product-service` 分页读取 MySQL `products`。
2. 每条商品发布一条 `PRODUCT_UPDATED` 搜索同步事件到 `SEARCH_EVENTS_TOPIC`。
3. `search-service` 消费事件并执行 ES `upsert`。

## 新增接口

- `POST /api/product/search-sync/full`
- 权限：`ROLE_ADMIN`
- 参数：
  - `pageSize`（可选，默认 `200`，最大 `1000`）
  - `status`（可选，按商品状态过滤）

## 调用示例

```bash
curl -X POST "http://localhost:80/api/product/search-sync/full?pageSize=200&status=1" \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```

返回 `data` 为本次发送到搜索同步通道的事件总数。

## 建议执行顺序

1. 先执行搜索索引重建：`POST /api/search/rebuild-index`
2. 再执行本接口做 MySQL 全量回灌。
3. 在 Grafana / Prometheus 观察 `trade_message_consume_total{service="search-service"...}` 以及搜索结果抽样校验。
