# 📊 Log Service - 审计与文档更新 (2025-09-26)

## 概述
- 操作/业务事件日志收集，基于RocketMQ异步解耦，Elasticsearch 存储与查询。

## 基本信息
- 服务名: log-service
- 端口: 8088
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（log-collection-topic等）
- Nacos: localhost:8848
- 存储: Elasticsearch
- 文档: /swagger-ui.html, /doc.html

## 依赖与约束（项目规则对齐）
- 不需要MySQL及其相关ORM依赖（MyBatis-Plus/JPA 等）
- 不需要MapStruct（建议移除如无必要）
- 持久化以ES为主

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8088/doc.html

## 待改进项
- 如存在Redis依赖，建议剥离（遵循“日志服务不需要数据库相关依赖”的规则）
- 事件幂等与重放保护方案文档化

## 下一步
- ES生命周期策略与冷热分层
- 检索字段脱敏与访问审计

