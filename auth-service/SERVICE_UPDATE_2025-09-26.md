# 🔐 Auth Service - 审计与文档更新 (2025-09-26)

## 概述
- 认证授权中心，OAuth2.1 授权服务器 + 资源服务器（部分资源）。
- JWT签发与公钥发布，客户端（web、client-service）配置齐全。

## 基本信息
- 服务名: auth-service
- 端口: 8081
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 统一端口 127.0.0.1:39876（日志topic: LOG_AUTH_TOPIC）
- Nacos: localhost:8848，命名空间 public
- 日志: ./auth-service/logs
- SpringDoc/Knife4j: /swagger-ui.html, /doc.html

## 依赖与约束（符合项目规则）
- 不需要与MySQL相关的依赖与配置（无MyBatis-Plus、无JPA）
- MapStruct 非必须，仅DTO映射可选

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8081/doc.html
- Postman/Knife4j 进行回归

## 待改进项
- GitHub OAuth client配置通过环境变量管理，避免明文
- 核查是否存在多余的静态permitAll路径（仅保留OAuth2与发现端点）

## 下一步
- 引入密钥轮换策略（JWK定期更新）
- 细化客户端权限与Scope分级

---

## 新增与优化（2025-09-26 补充）

### Redis 连接验证改为可配置
- 新增开关 `auth.redis.validation.enabled`（默认不启用），在开发/无Redis环境避免启动失败
- 开启时将对 Redis 进行 ping 验证，失败会抛出异常快速失败

### 启动建议（本地开发）
- 如仅验证应用能启动：
  - 通过命令行禁用 Nacos 注册与配置：
    - `--spring.cloud.nacos.discovery.enabled=false`
    - `--spring.cloud.nacos.config.enabled=false`
  - 如本地无 Redis，则保持 `auth.redis.validation.enabled=false`（默认）
  - 如需实际发放 Token，请配置 Redis：`spring.data.redis.host`、`spring.data.redis.port`

