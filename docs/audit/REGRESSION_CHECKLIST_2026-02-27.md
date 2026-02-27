# 回归验证清单

## A. 路由与鉴权
- [ ] 匿名访问 `/api/orders/**` `/api/payments/**` `/api/stocks/**` 返回 401/403
- [ ] 匿名访问 `/api/search/**`（公开接口）符合预期
- [ ] 非 testenv profile 下，`testenv-bypass-enabled` 不生效
- [ ] 黑名单 token 在 Redis 异常时仍被拦截

## B. 交易主链路
- [ ] 下单后产生 `order-created`
- [ ] 支付成功后产生 `payment-success`
- [ ] 库存确认扣减且订单状态流转到终态

## C. 交易异常链路
- [ ] 库存不足触发 `stock-freeze-failed` 并取消订单
- [ ] 支付回调重放不造成重复扣减/重复记账
- [ ] 重复消费同一 `eventId` 不造成二次副作用

## D. 搜索链路
- [ ] `/api/search/search` 正常返回
- [ ] search-service 超时时网关 fallback 生效
- [ ] `/api/search/suggestions` 与 `/api/search/hot-keywords` 可用

## E. 前端
- [ ] 登录失效后能触发 refresh 或重新登录
- [ ] 越权页面跳转 `/forbidden` 且后端 API 同步拒绝
- [ ] 未泄露敏感调试信息

## F. 基础设施
- [ ] 默认口令已替换/移除
- [ ] ES 安全策略符合环境要求
- [ ] Prometheus/Grafana 可采集并展示关键指标

## G. 自动化
- [ ] `mvn -T 1C -DskipITs test` 通过
- [ ] `tests/perf/k6/run-all-services-smoke.ps1` 可执行
- [ ] `tests/perf/k6/run-acceptance.ps1` 可执行

