# 故障分诊清单（交易链路）

## 1. 网关入口

- [ ] `GET /actuator/health`（gateway）是否正常。
- [ ] `/api/orders`、`/api/payments`、`/api/stocks` 是否出现 404/401/5xx 激增。
- [ ] 路由是否命中错误服务前缀（检查最新 `application-route.yml`）。

## 2. 订单链路

- [ ] 下单后是否发布 `order-created`。
- [ ] 订单状态是否卡在待支付且长时间不变。
- [ ] 幂等键 `eventId` 是否重复且被正确去重。

## 3. 支付链路

- [ ] `order-created` 消费后是否仅创建待支付记录。
- [ ] 支付回调后是否发布 `payment-success`。
- [ ] 支付成功事件是否存在发送失败日志。

## 4. 库存链路

- [ ] `order-created` 是否触发预占。
- [ ] `payment-success` 是否触发确认扣减。
- [ ] 库存不足时是否触发 `stock-freeze-failed` 并取消订单。
- [ ] 退款后是否走“预占释放/已扣减回补”正确分支。

## 5. 搜索链路

- [ ] 商品新增/更新/删除是否发布搜索同步事件。
- [ ] `search-service` 是否消费 `PRODUCT_CREATED|PRODUCT_UPDATED|PRODUCT_DELETED`。
- [ ] 手动重建索引接口是否可用。

## 6. 基础设施

- [ ] Nacos 配置是否与激活 profile 一致。
- [ ] RocketMQ topic、tag、group 是否匹配。
- [ ] Redis 是否可用（幂等键、上下文缓存）。
- [ ] Elasticsearch 是否可写、索引是否存在。
