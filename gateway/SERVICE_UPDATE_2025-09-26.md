# 🚪 Gateway Service - 审计与文档更新 (2025-09-26)

## 概述
- 网关作为统一入口，承担认证、路由、限流、监控。
- 已严格遵循OAuth2.1资源服务器配置，Knife4j文档聚合。

## 基本信息
- 服务名: gateway
- 端口: 80
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 统一端口 127.0.0.1:39876（如以网关透传为主，此项仅文档聚合不直接消费MQ）
- Nacos: localhost:8848，命名空间 public
- Redis: 建议使用独立DB（database: 6）
- Swagger/Knife4j: /swagger-ui.html 与 /doc.html（聚合）

## 安全与路由
- OAuth2.1 标准端点开放：/oauth2/**、/.well-known/**、/userinfo
- 资源API需JWT认证
- 路由优先级：具体 > API > 直接 > 通用

## 健康检查
- Actuator: /actuator/health

## 文档/测试
- Knife4j: http://localhost/doc.html（聚合）
- 推荐通过 Postman 与 Knife4j 测试API

## 待改进项
- 检查是否存在多余的 permitAll 路径，确保生产环境收紧API暴露
- CORS 统一策略在 prod 环境落地

## 下一步
- 引入统一网关日志追踪ID透传
- 完成限流规则配置的配置中心化（Nacos）

