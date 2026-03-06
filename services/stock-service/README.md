# Stock Service

库存服务，支持库存增减、预留/释放、盘点与变更日志查询。

- 服务名：`stock-service`
- 端口：`8085`
- 数据库脚本：`db/init/stock-service/init.sql`
- 测试数据：`db/test/stock-service/test.sql`

## 核心接口

- 统一入口：`/api/stocks/**`
- 内部调用：`/internal/stock/**`

## 本地启动

```bash
mvn -pl stock-service spring-boot:run
```
