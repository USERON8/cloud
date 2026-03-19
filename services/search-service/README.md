# Search Service
Version: 1.1.0

搜索服务，负责商品与店铺检索、推荐与建议词能力。

- 服务名：`search-service`
- 端口：`8087`
- 主要依赖：Elasticsearch、Redis、Nacos

## 核心接口

- 商品搜索：`/api/search/**`
- 店铺搜索：`/api/search/shops/**`

## 说明

- 当前服务不维护独立 MySQL 初始化脚本
- 商品数据由上游同步到 Elasticsearch，当前已接入 MQ 增量同步和 XXL 全量重建
- 内置 L1/L2 缓存与热词刷新策略（配置见 `application.yml`）
- 热词 DB 同步默认使用 `scheduled` 模式；切换到 XXL 需要设置 `SEARCH_HOT_DB_SYNC_TRIGGER_MODE=xxl`

## 本地启动

```bash
mvn -pl search-service spring-boot:run
```
