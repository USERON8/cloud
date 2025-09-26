# 💳 Payment Service - 审计与文档更新 (2025-09-26)

## 概述
- 支付订单、支付流水，支付宝已接入，计划接入微信与退款流程。

## 基本信息
- 服务名: payment-service
- 端口: 8086
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（payment-events、PAYMENT_TO_ORDER/STOCK等）
- Nacos: localhost:8848
- DB: payment_db（MySQL），MyBatis-Plus
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8086/doc.html

## 待改进项
- 秘钥与网关配置全部改为环境变量注入
- 退款/关单流程与订单状态联动

## 下一步
- 微信支付与对账/清结算完善

