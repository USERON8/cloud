# 整改实施单（可直接执行）

## 批次 A（先做，阻断发布风险）
1. 任务：封禁生产环境 `testenv-bypass-enabled`  
范围：`gateway` + 7 个业务服务 + `auth-service`  
动作：
- 启动时检测 `profile in (prod,staging)` 且 `testenv-bypass-enabled=true` 则 `fail fast`
- CI/CD 增加配置扫描，禁止该键在非 testenv 配置出现  
验收：
- 非 testenv 启动时此开关无法生效
- 匿名访问受保护接口返回 401/403

2. 任务：修复 `payment-service` 编译阻断  
范围：`payment-service/src/main/java/com/cloud/payment/config/AlipayConfig.java`  
动作：
- 按当前 `alipay-sdk 4.40.452.ALL` 可用构造器重写客户端初始化  
验收：
- `mvn -T 1C -DskipITs test` 全仓通过

3. 任务：清理默认密钥与弱口令  
范围：`auth-service`、`docker/*`、`monitoring-compose`  
动作：
- 移除硬编码默认 secret/password
- 通过环境变量注入，未注入即启动失败  
验收：
- 无默认明文凭据可直接登录

## 批次 B（交易一致性）
1. 任务：升级消息幂等实现  
范围：`order-service` `payment-service` `stock-service`  
动作：
- 引入“消息处理记录表/缓存”持久化终态
- 业务关键写操作增加唯一键防重
- 避免在“部分成功”后直接释放幂等锁  
验收：
- 重复消息、异常重试、回放消息均不产生重复扣减/重复状态迁移

2. 任务：补充交易链路集成测试  
范围：`tests` + 各交易服务测试模块  
动作：
- 新增场景：重复 `order-created`、重复 `payment-success`、库存不足、退款回滚  
验收：
- CI 中新增用例稳定通过

## 批次 C（安全面收敛与可观测）
1. 任务：收敛 actuator/docs 暴露面  
范围：`gateway` `common-module`  
动作：
- `/actuator/**` 仅内网或鉴权访问
- 文档路由仅开发环境开放或加鉴权  
验收：
- 外部匿名请求不可访问敏感运维端点

2. 任务：CORS 与前端 token 安全加固  
范围：`common-module` `my-shop-web`  
动作：
- CORS 改为白名单域名
- 中期将 token 从 localStorage 迁移至 HttpOnly Cookie  
验收：
- 非白名单跨域被拒绝
- 前端脚本无法读取会话凭据（目标态）

3. 任务：网关黑名单校验 fail-open 改造  
范围：`gateway`  
动作：
- Redis 异常时关键接口 fail-closed
- 增加降级日志与指标  
验收：
- 黑名单 token 在依赖异常时仍被拒绝

