# 订单数据库结构升级说明 v2.0

## 概述

本次升级为订单服务的数据库表结构进行了重要更新，以支持完整的订单业务流程和统一的数据规范。

**升级日期**: 2025-09-26  
**版本**: v2.0  
**影响表**: `orders`、`order_item`

## 主要变更

### 1. orders表结构升级

#### 新增字段
- `order_no` VARCHAR(32) - 订单号（业务唯一编号）
- `pay_time` DATETIME - 支付时间
- `ship_time` DATETIME - 发货时间  
- `complete_time` DATETIME - 完成时间
- `cancel_time` DATETIME - 取消时间
- `cancel_reason` VARCHAR(255) - 取消原因
- `remark` VARCHAR(500) - 备注信息

#### 标准化字段
- `create_by` BIGINT UNSIGNED - 创建人
- `update_by` BIGINT UNSIGNED - 更新人
- `version` INT - 乐观锁版本号
- `create_time` DATETIME - 创建时间 (由created_at重命名)
- `update_time` DATETIME - 更新时间 (由updated_at重命名)
- `deleted` TINYINT - 逻辑删除标记 (更新注释)

#### 新增索引
- `uk_order_no` - 订单号唯一索引
- `idx_pay_time` - 支付时间索引
- `idx_ship_time` - 发货时间索引
- `idx_complete_time` - 完成时间索引
- `idx_cancel_time` - 取消时间索引

### 2. order_item表结构升级

#### 标准化字段
- `create_by` BIGINT UNSIGNED - 创建人
- `update_by` BIGINT UNSIGNED - 更新人
- `version` INT - 乐观锁版本号
- `create_time` DATETIME - 创建时间 (由created_at重命名)
- `update_time` DATETIME - 更新时间 (由updated_at重命名)
- `deleted` TINYINT - 逻辑删除标记 (更新注释)

#### 新增索引
- `idx_create_time` - 创建时间索引

## 订单状态定义

| 状态码 | 状态名称 | 描述 |
|--------|----------|------|
| 0 | 待支付 | 订单已创建，等待用户支付 |
| 1 | 已支付 | 支付成功，准备发货 |
| 2 | 已发货 | 商品已发出，等待用户收货 |
| 3 | 已完成 | 订单已完成，交易结束 |
| 4 | 已取消 | 订单已取消 |

## 订单生命周期时间字段使用

- **create_time**: 订单创建时间（自动设置）
- **pay_time**: 用户完成支付后设置
- **ship_time**: 商家发货后设置
- **complete_time**: 用户确认收货或订单完成后设置
- **cancel_time**: 订单被取消时设置

## 升级步骤

### 1. 备份数据
```sql
-- 备份orders表
CREATE TABLE orders_backup_20250926 AS SELECT * FROM orders;

-- 备份order_item表  
CREATE TABLE order_item_backup_20250926 AS SELECT * FROM order_item;
```

### 2. 执行升级脚本
```bash
# 升级orders表
mysql -u root -p order_db < sql/migration/upgrade_orders_table_v2.sql

# 升级order_item表
mysql -u root -p order_db < sql/migration/upgrade_order_item_table_v2.sql
```

### 3. 验证升级结果
```sql
-- 检查表结构
DESC orders;
DESC order_item;

-- 检查索引
SHOW INDEX FROM orders;
SHOW INDEX FROM order_item;

-- 检查数据完整性
SELECT COUNT(*) FROM orders;
SELECT COUNT(*) FROM order_item;
```

## 业务影响

### 1. 应用程序代码适配
- Order实体类已更新支持新字段
- OrderBusinessService已实现完整业务流程
- 缓存注解已更新为标准Spring Cache

### 2. 订单号生成
- 新订单将自动生成唯一订单号格式: `ORD{timestamp}{3位随机数}`
- 现有订单会在升级时自动生成订单号

### 3. 时间字段记录
- 各个业务阶段的时间会自动记录
- 支持订单全生命周期跟踪

## 兼容性说明

### 向下兼容
- 现有字段保持不变，只新增字段
- 原有业务逻辑继续可用

### 向上兼容  
- 支持新的订单业务流程
- 支持完整的订单状态流转
- 支持订单时间轴追踪

## 性能优化

### 索引优化
- 为常用查询字段添加索引
- 订单号唯一索引提高查询性能
- 时间字段索引支持时间范围查询

### 查询建议
```sql
-- 按订单号查询
SELECT * FROM orders WHERE order_no = 'ORD1727332800123';

-- 按状态和时间查询
SELECT * FROM orders WHERE status = 1 AND pay_time >= '2025-09-26';

-- 统计订单完成情况
SELECT DATE(complete_time), COUNT(*) 
FROM orders 
WHERE status = 3 AND complete_time >= '2025-09-01'
GROUP BY DATE(complete_time);
```

## 回滚方案

如需回滚至原结构：
```sql
-- 删除新增字段（谨慎操作）
ALTER TABLE orders DROP COLUMN order_no, DROP COLUMN pay_time, 
DROP COLUMN ship_time, DROP COLUMN complete_time, DROP COLUMN cancel_time,
DROP COLUMN cancel_reason, DROP COLUMN remark, DROP COLUMN create_by,
DROP COLUMN update_by, DROP COLUMN version;

-- 重命名时间字段
ALTER TABLE orders CHANGE COLUMN create_time created_at DATETIME;
ALTER TABLE orders CHANGE COLUMN update_time updated_at DATETIME;
```

## 注意事项

1. **数据备份**: 升级前务必备份数据
2. **应用重启**: 升级完成后需重启订单服务应用
3. **缓存清理**: 建议清理Redis缓存以避免数据不一致
4. **监控观察**: 升级后需要观察应用运行状况
5. **测试验证**: 建议在测试环境先执行验证

## 相关文档

- [订单服务API文档](../docs/order-service-api.md)
- [订单业务流程说明](../docs/order-business-flow.md)
- [缓存策略说明](../docs/cache-strategy.md)

---

**维护信息**  
- 创建人: CloudDevAgent
- 创建时间: 2025-09-26
- 最后更新: 2025-09-26
- 版本: v2.0
