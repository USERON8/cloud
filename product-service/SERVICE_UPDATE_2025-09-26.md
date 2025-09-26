# 📦 Product Service - 审计与文档更新 (2025-09-26)

## 概述
- 商品/分类管理，多级缓存（Caffeine+Redis），支持搜索事件发布。

## 基本信息
- 服务名: product-service
- 端口: 8083
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（SEARCH_EVENTS_TOPIC、LOG_PRODUCT_TOPIC）
- Nacos: localhost:8848
- DB: product_db（MySQL），MyBatis-Plus
- 缓存: Caffeine + Redis
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8083/doc.html

## 待改进项
- 与库存服务的缓存失效联动策略完善
- 数据变更事件防重与重放保护

## 下一步
- 商品索引构建与实时同步优化

