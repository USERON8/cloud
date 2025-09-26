# 🔍 Search Service - 审计与文档更新 (2025-09-26)

## 概述
- Elasticsearch 集成，商品/店铺搜索，搜索事件消费。

## 基本信息
- 服务名: search-service
- 端口: 8087
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（search-events/log等）
- Nacos: localhost:8848
- ES: 8.x
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8087/doc.html

## 待改进项
- 索引模板/别名策略优化，支持零停机重建
- 查询DSL与聚合性能调优

## 下一步
- 个性化搜索与推荐基线建设

