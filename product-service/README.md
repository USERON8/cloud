# Product Service

商品域服务，提供商品、分类与批量操作接口，并支持搜索同步触发。

- 服务名：`product-service`
- 端口：`8084`
- 数据库脚本：`db/init/product-service/init.sql`
- 测试数据：`db/test/product-service/test.sql`

## 核心接口

- 商品：`/api/product/**`
- 分类：`/api/category/**`
- 内部调用：`/internal/product/**`
- 全量搜索同步：`POST /api/product/search-sync/full`

## 本地启动

```bash
mvn -pl product-service spring-boot:run
```
