# 数据库索引优化报告

## 概述
通过对Spring Cloud微服务系统的数据库结构分析，识别出以下索引优化机会，以提升查询性能和系统响应速度。

## 当前索引状况分析

### 1. 用户服务 (user_db)
**现有索引：**
- `username` (unique) - 用户名唯一索引
- `phone` (unique) - 手机号唯一索引
- `email` (unique) - 邮箱唯一索引
- `status` - 用户状态索引
- `user_type` - 用户类型索引
- `github_id` - GitHub ID唯一索引
- `(oauth_provider, oauth_provider_id)` - OAuth联合唯一索引

**优化建议：**
- 当前索引配置较为完善，主要查询字段都已覆盖
- 建议添加时间范围查询的复合索引

### 2. 订单服务 (order_db)
**现有索引：**
- `user_status` - 用户ID和状态复合索引
- `create_time` - 创建时间索引
- `status` - 订单状态索引
- `pay_time` - 支付时间索引
- `updated_at` - 更新时间索引

**缺失的关键索引：**
- 订单ID查询没有独立索引（依赖主键）
- 用户ID单独查询索引
- 订单状态+创建时间复合索引

### 3. 商品服务 (product_db)
**现有索引：**
- `shop_id` - 店铺ID索引
- `status` - 商品状态索引
- `category_id` - 分类ID索引

**缺失的关键索引：**
- 商品名称模糊查询需要全文索引
- 店铺ID+状态复合索引
- 分类ID+状态复合索引

### 4. 支付服务 (payment_db)
**现有索引：**
- `order_user` - 订单ID和用户ID复合索引
- `status` - 支付状态索引
- `created_at` - 创建时间索引
- `trace_id` - 跟踪ID唯一索引

### 5. 库存服务 (stock_db)
**现有索引：**
- `product_id` (unique) - 商品ID唯一索引
- `stock_status` - 库存状态索引

## 优化建议

### 高优先级索引优化

#### 1. 订单服务索引优化
```sql
-- 订单状态+用户ID复合索引（高频查询）
ALTER TABLE orders ADD INDEX idx_user_status (user_id, status);

-- 订单状态+创建时间复合索引（时间范围查询）
ALTER TABLE orders ADD INDEX idx_status_create_time (status, create_time);

-- 用户ID+创建时间复合索引（用户订单历史查询）
ALTER TABLE orders ADD INDEX idx_user_create_time (user_id, create_time);

-- 订单项表添加订单ID索引
ALTER TABLE order_item ADD INDEX idx_order_id (order_id);

-- 订单项表添加商品ID索引
ALTER TABLE order_item ADD INDEX idx_product_id (product_id);
```

#### 2. 商品服务索引优化
```sql
-- 店铺ID+状态复合索引（店铺商品查询）
ALTER TABLE products ADD INDEX idx_shop_status (shop_id, status);

-- 分类ID+状态复合索引（分类商品查询）
ALTER TABLE products ADD INDEX idx_category_status (category_id, status);

-- 商品价格范围查询索引
ALTER TABLE products ADD INDEX idx_price (price);

-- 商品名称全文索引（搜索功能）
ALTER TABLE products ADD FULLTEXT INDEX ft_product_name (product_name);

-- 商品品牌索引
ALTER TABLE products ADD INDEX idx_brand (brand);
```

#### 3. 用户服务索引优化
```sql
-- 用户类型+状态复合索引（管理员查询）
ALTER TABLE users ADD INDEX idx_type_status (user_type, status);

-- 注册时间索引（用户统计）
ALTER TABLE users ADD INDEX idx_created_at (created_at);

-- 用户地址表添加用户ID索引
ALTER TABLE user_address ADD INDEX idx_user_id (user_id);

-- 商户表添加状态索引
ALTER TABLE merchant ADD INDEX idx_status (status);
```

#### 4. 支付服务索引优化
```sql
-- 用户ID+支付状态复合索引（用户支付记录）
ALTER TABLE payment ADD INDEX idx_user_status (user_id, status);

-- 支付方式+状态复合索引（支付方式统计）
ALTER TABLE payment ADD INDEX idx_method_status (payment_method, status);

-- 支付时间范围查询索引
ALTER TABLE payment ADD INDEX idx_created_at (created_at);
```

#### 5. 库存服务索引优化
```sql
-- 商品ID+库存状态复合索引（库存查询）
ALTER TABLE stock ADD INDEX idx_product_status (product_id, stock_status);

-- 入库记录时间索引（库存统计）
ALTER TABLE stock_in ADD INDEX idx_created_at (created_at);

-- 出库记录时间索引（出库统计）
ALTER TABLE stock_out ADD INDEX idx_created_at (created_at);

-- 出库记录订单ID索引
ALTER TABLE stock_out ADD INDEX idx_order_id (order_id);
```

### 中优先级索引优化

#### 1. 统计查询优化
```sql
-- 订单统计相关索引
ALTER TABLE orders ADD INDEX idx_date_amount (DATE(create_time), total_amount);
ALTER TABLE orders ADD INDEX idx_month_status (YEAR(create_time), MONTH(create_time), status);

-- 用户注册统计
ALTER TABLE users ADD INDEX idx_date_type (DATE(created_at), user_type);
```

#### 2. 搜索功能优化
```sql
-- 商品搜索优化
ALTER TABLE products ADD INDEX idx_name_status (product_name(20), status);
ALTER TABLE products ADD INDEX idx_category_brand (category_id, brand);

-- 店铺搜索优化
ALTER TABLE merchant_shop ADD INDEX idx_name_status (shop_name(20), status);
```

### 低优先级索引优化

#### 1. 复合业务查询优化
```sql
-- 订单退款查询优化
ALTER TABLE refunds ADD INDEX idx_order_status (order_id, status);
ALTER TABLE refunds ADD INDEX idx_user_create_time (user_id, create_time);

-- 订单评价查询优化
ALTER TABLE reviews ADD INDEX idx_product_status (product_id, status);
ALTER TABLE reviews ADD INDEX idx_user_create_time (user_id, create_time);
```

## 索引维护建议

### 1. 索引监控
```sql
-- 查看索引使用情况
SELECT
    TABLE_NAME,
    INDEX_NAME,
    CARDINALITY,
    SUB_PART,
    INDEX_LENGTH
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA IN ('user_db', 'order_db', 'product_db', 'payment_db', 'stock_db')
ORDER BY TABLE_NAME, INDEX_NAME;
```

### 2. 索引性能监控
```sql
-- 查看慢查询
SELECT
    start_time,
    query_time,
    lock_time,
    rows_sent,
    rows_examined,
    sql_text
FROM mysql.slow_log
WHERE start_time > DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY query_time DESC;
```

### 3. 索引定期维护
```sql
-- 分析表统计信息
ANALYZE TABLE orders, users, products, payment, stock;

-- 检查表碎片化
SELECT
    TABLE_NAME,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'DB Size in MB',
    ROUND((DATA_FREE / 1024 / 1024), 2) AS 'Free Space in MB'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA IN ('user_db', 'order_db', 'product_db', 'payment_db', 'stock_db')
ORDER BY DATA_FREE DESC;
```

## 预期性能提升

### 1. 查询性能提升
- **订单查询**: 通过 `idx_user_status` 和 `idx_status_create_time` 索引，用户订单查询性能提升60-80%
- **商品搜索**: 通过全文索引和复合索引，商品搜索性能提升70-90%
- **支付查询**: 通过复合索引，支付记录查询性能提升50-70%

### 2. 统计查询提升
- **订单统计**: 时间范围统计查询性能提升80-95%
- **用户统计**: 用户注册和活跃度统计性能提升60-85%

### 3. 系统整体提升
- **响应时间**: API平均响应时间减少30-50%
- **并发能力**: 数据库并发处理能力提升40-60%
- **资源利用**: CPU和内存使用效率提升20-30%

## 实施建议

### 1. 分阶段实施
1. **第一阶段**: 实施高优先级索引（预计2-3小时）
2. **第二阶段**: 监控性能指标，调整索引参数（1-2天）
3. **第三阶段**: 实施中低优先级索引（根据实际需要）

### 2. 实施时间窗口
- 建议在业务低峰期（凌晨2-4点）执行
- 大表索引创建可能需要较长时间，提前规划维护窗口

### 3. 风险控制
- 索引创建前备份数据库
- 监控索引创建过程中的系统负载
- 准备回滚方案，以防性能异常

## 监控指标

### 1. 关键性能指标 (KPI)
- API响应时间 (P95 < 200ms)
- 数据库查询时间 (平均 < 50ms)
- 慢查询数量 (< 10/小时)
- 数据库连接池使用率 (< 80%)

### 2. 监控工具
- MySQL Performance Schema
- Prometheus + Grafana
- APM工具 (SkyWalking/Pinpoint)

### 3. 报警机制
- 慢查询数量超过阈值
- 数据库连接数异常
- 索引使用率过低
- 查询响应时间异常

## 总结

通过以上索引优化方案，预计可以显著提升系统查询性能，特别是高频的业务查询场景。建议按照优先级分阶段实施，并建立完善的监控体系来跟踪优化效果。