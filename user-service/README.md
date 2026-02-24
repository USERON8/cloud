# User Service

用户域服务，覆盖消费者、商家、管理员与商家认证流程。

- 服务名：`user-service`
- 端口：`8082`
- 数据库脚本：`db/init/user-service/init.sql`
- 测试数据：`db/test/user-service/test.sql`

## 核心接口分组

- 用户资料：`/api/user/profile/**`
- 用户地址：`/api/user/address/**`
- 用户查询：`/api/query/users/**`
- 用户管理：`/api/manage/users/**`
- 商家管理：`/api/merchant/**`
- 商家认证：`/api/merchant/auth/**`
- 管理员：`/api/admin/**`
- 统计与线程池：`/api/statistics/**`、`/api/thread-pool/**`

## 权限约定

- 使用 OAuth2 JWT 资源服务模式
- Scope 统一为 `resource:action`（仅保留冒号风格）
- 管理接口要求 `ROLE_ADMIN` + 对应 scope
- 内部 Feign 接口使用 `/internal/user/**`

## 本地启动

```bash
mvn -pl user-service spring-boot:run
```
