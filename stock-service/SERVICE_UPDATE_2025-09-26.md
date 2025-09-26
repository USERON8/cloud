# 📋 Stock Service - 审计与文档更新 (2025-09-26)

## 概述
- 库存管理、出入库记录、并发控制（Redisson），与订单/支付事件联动。

## 基本信息
- 服务名: stock-service
- 端口: 8085
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（stock-events、ORDER_TO_STOCK_TOPIC等）
- Nacos: localhost:8848
- DB: stock_db（MySQL），MyBatis-Plus
- 锁: Redisson
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8085/doc.html

## 待改进项
- 锁粒度与过期策略再优化，避免热点竞争
- 与订单的幂等防重策略合并统一

## 下一步
- 库存预警与异步补库存策略

