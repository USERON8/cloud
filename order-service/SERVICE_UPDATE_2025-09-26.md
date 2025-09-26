# 📝 Order Service - 审计与文档更新 (2025-09-26)

## 概述
- 订单生命周期管理（创建/支付/发货/完成/取消），消息驱动。

## 基本信息
- 服务名: order-service
- 端口: 8084
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（order-events、与payment/stock互通topic）
- Nacos: localhost:8848
- DB: order_db（MySQL），MyBatis-Plus
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8084/doc.html

## 待改进项
- 分布式事务（Seata/Saga）落地
- Controller层权限检查补齐（与网关策略一致）

## 下一步
- 订单状态机与补偿机制完善

