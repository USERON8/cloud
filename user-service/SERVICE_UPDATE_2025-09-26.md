# 👥 User Service - 审计与文档更新 (2025-09-26)

## 概述
- 用户注册/登录、资料、地址管理，多级用户体系（用户/商家/管理员）。

## 基本信息
- 服务名: user-service
- 端口: 8082
- Profile: dev/prod

## 统一配置要点
- RocketMQ: 127.0.0.1:39876（user-events、LOG_USER_TOPIC）
- Nacos: localhost:8848，public
- DB: user_db（MySQL 8.0+），MyBatis-Plus（3.5.13）
- 缓存: Redis 7.0+
- 文档: /swagger-ui.html, /doc.html

## 健康检查
- /actuator/health

## 文档/测试
- Knife4j: http://localhost:8082/doc.html
- Postman/Knife4j

## 待改进项
- 统一MyBatis日志实现为Slf4jImpl（避免StdOutImpl）
- 强化异常统一处理与错误码枚举

## 下一步
- 地址与订单联动校验（减少脏写）
- 用户行为事件标准化（埋点）

---

## 新增与优化（2025-09-26 补充）

### Redis 缓存：String + Hash 混合策略
- 新增混合缓存组件：HybridCacheManager（common-module），在 user-service 中通过 AOP 切面自动调用
- 新增分析器与性能度量：CacheDataAnalyzer、CachePerformanceMetrics
- 在 user-service 的 MultiLevelCacheAspect 中集成混合缓存：
  - 读取：根据方法返回类型与Redis实际数据结构，自动 smartGet（Hash优先）
  - 写入：smartSet 智能选择 String/Hash 存储
- 适用策略：
  - 用户对象（userCache 下）优先采用 Hash，便于字段级更新与读取
  - 简单值/集合使用 String
- 兼容回退：当混合缓存出错时自动回退到 opsForValue（String）

### 运维与监控
- 暴露缓存性能报告生成接口（内部使用）：通过 CachePerformanceMetrics 生成统计
- 建议在后续集成 Prometheus 采集缓存命中率、耗时分布

### 使用说明
- 现有 @MultiLevelCacheable/@MultiLevelCachePut/@MultiLevelCacheEvict 注解无需改动
- Redis Key 仍使用原有规范：cache:{cacheName}:{key}
- 若需进行字段级更新（如仅更新nickname/phone），可通过 HybridCacheManager.updateHashFields(key, fields, ttl)

