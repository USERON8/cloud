# Search Service

搜索服务，负责商品与店铺检索、推荐与建议词能力。

- 服务名：`search-service`
- 端口：`8087`
- 主要依赖：Elasticsearch、Redis、Nacos、RocketMQ（搜索事件消费）

## 核心接口

- 商品搜索：`/api/search/**`
- 店铺搜索：`/api/search/shops/**`

## 说明

- 当前服务不维护独立 MySQL 初始化脚本
- 商品数据由业务事件同步到 Elasticsearch

## 本地启动

```bash
mvn -pl search-service spring-boot:run
```
