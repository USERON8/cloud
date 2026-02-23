# 🗄️ Cloud微服务电商平台 - 数据库设计文档

<div align="center">

**版本**: v1.0 | **更新时间**: 2025.01.25 | **作者**: Cloud Platform Team

</div>

---

## 📋 概述

本文档详细描述了Cloud微服务电商平台的数据库架构设计，包括各个微服务的数据库设计、表结构说明、索引设计和数据初始化脚本。

## 🏗️ 数据库架构

### 数据库拆分原则

根据微服务架构的设计原则，每个服务拥有独立的数据库，实现数据隔离和服务自治：
| 数据库名称 | 所属服务 | 说明 | 表数量 |
|-----------|---------|------|---------|
| user_db | user-service | 用户相关数据 | 5 |
| product_db | product-service | 商品相关数据 | 4 |
| order_db | order-service | 订单相关数据 | 3 |
| stock_db | stock-service | 库存相关数据 | 2 |
| payment_db | payment-service | 支付相关数据 | 3 |

### 数据库版本

- **MySQL**: 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_general_ci
- **存储引擎**: InnoDB

## 📊 数据库详细设计

### 1. 用户数据库 (user_db)

#### 表结构

##### users - 用户信息表

```sql
CREATE TABLE IF NOT EXISTS users
(
    id                BIGINT UNSIGNED PRIMARY KEY COMMENT '用户ID',
    username          VARCHAR(50)                        NOT NULL UNIQUE COMMENT '用户名',
    password          VARCHAR(255)                       NOT NULL COMMENT '加密密码',
    phone             VARCHAR(20) UNIQUE COMMENT '手机号',
    nickname          VARCHAR(50)                        NOT NULL COMMENT '昵称',
    avatar_url        VARCHAR(255) COMMENT '头像URL',
    email             VARCHAR(100) UNIQUE COMMENT '邮箱地址（用于GitHub登录）',
    github_id         BIGINT UNSIGNED                    NULL COMMENT 'GitHub用户ID（OAuth登录专用）',
    github_username   VARCHAR(100)                       NULL COMMENT 'GitHub用户名（OAuth登录专用）',
    oauth_provider    VARCHAR(20)                        NULL COMMENT 'OAuth提供商（github, wechat等）',
    oauth_provider_id VARCHAR(100)                       NULL COMMENT 'OAuth提供商用户ID',
    status            TINYINT                            NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    user_type         ENUM ('USER', 'MERCHANT', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户类型',
    created_at        DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted           TINYINT                            NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_user_type (user_type),
    INDEX idx_github_id (github_id),
    INDEX idx_github_username (github_username),
    INDEX idx_oauth_provider (oauth_provider),
    INDEX idx_oauth_provider_combined (oauth_provider, oauth_provider_id),
    UNIQUE INDEX uk_github_id (github_id),
    UNIQUE INDEX uk_github_username (github_username),
    UNIQUE INDEX uk_oauth_provider_id (oauth_provider, oauth_provider_id)
) COMMENT ='用户表';
```

##### user_address - 用户地址表

```sql
CREATE TABLE `user_address`
(
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    consignee      VARCHAR(50)     NOT NULL COMMENT '收货人姓名',
    phone          VARCHAR(20)     NOT NULL COMMENT '联系电话',
    province       VARCHAR(20)     NOT NULL COMMENT '省份',
    city           VARCHAR(20)     NOT NULL COMMENT '城市',
    district       VARCHAR(20)     NOT NULL COMMENT '区县',
    street         VARCHAR(100)    NOT NULL COMMENT '街道',
    detail_address VARCHAR(255)    NOT NULL COMMENT '详细地址',
    is_default     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否默认地址：0-否，1-是',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX idx_user_id (user_id),
    INDEX idx_user_default (user_id, is_default),

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) COMMENT ='用户地址表';
```

### 2. 商品数据库 (product_db)

##### product - 商品表

```sql
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `merchant_id` BIGINT DEFAULT NULL COMMENT '商家ID',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `description` TEXT COMMENT '商品描述',
  `main_image` VARCHAR(500) DEFAULT NULL COMMENT '主图URL',
  `images` TEXT COMMENT '图片URLs(JSON数组)',
  `sales` INT DEFAULT 0 COMMENT '销量',
  `stock` INT DEFAULT 0 COMMENT '库存(冗余字段)',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态:1-上架,0-下架',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '版本号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';
```

##### product_category - 商品分类表

```sql
CREATE TABLE `product_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `level` TINYINT(1) NOT NULL COMMENT '层级:1-一级,2-二级,3-三级',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `icon` VARCHAR(500) DEFAULT NULL COMMENT '图标URL',
  `status` TINYINT(1) DEFAULT 1 COMMENT '状态:1-启用,0-禁用',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';
```

### 3. 订单数据库 (order_db)

##### orders - 订单信息表

```sql
CREATE TABLE `orders`
(
    id             BIGINT UNSIGNED PRIMARY KEY COMMENT '订单ID',
    order_no       VARCHAR(32)     NOT NULL UNIQUE COMMENT '订单号（业务唯一编号）',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    total_amount   DECIMAL(10, 2)  NOT NULL COMMENT '订单总额',
    pay_amount     DECIMAL(10, 2)  NOT NULL COMMENT '实付金额',
    status         TINYINT         NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    address_id     BIGINT UNSIGNED NOT NULL COMMENT '地址ID',
    pay_time       DATETIME        NULL COMMENT '支付时间',
    ship_time      DATETIME        NULL COMMENT '发货时间',
    complete_time  DATETIME        NULL COMMENT '完成时间',
    cancel_time    DATETIME        NULL COMMENT '取消时间',
    cancel_reason  VARCHAR(255)    NULL COMMENT '取消原因',
    remark         VARCHAR(500)    NULL COMMENT '备注',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT UNSIGNED NULL COMMENT '创建人',
    update_by      BIGINT UNSIGNED NULL COMMENT '更新人',
    version        INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_status (user_id, status),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status),
    INDEX idx_pay_time (pay_time),
    INDEX idx_ship_time (ship_time),
    INDEX idx_complete_time (complete_time),
    INDEX idx_cancel_time (cancel_time)
) COMMENT ='订单主表';
```

##### order_item - 订单明细表

```sql
CREATE TABLE `order_item`
(
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    order_id         BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    product_id       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    product_snapshot JSON            NOT NULL COMMENT '商品快照',
    quantity         INT             NOT NULL COMMENT '购买数量',
    price            DECIMAL(10, 2)  NOT NULL COMMENT '购买时单价',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT UNSIGNED NULL COMMENT '创建人',
    update_by        BIGINT UNSIGNED NULL COMMENT '更新人',
    version          INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    INDEX idx_order_product (order_id, product_id),
    INDEX idx_product_id (product_id),
    INDEX idx_create_time (create_time)
) COMMENT ='订单明细表';
```

### 4. 库存数据库 (stock_db)

##### stock - 库存表

```sql
CREATE TABLE `stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
  `available_stock` INT NOT NULL DEFAULT 0 COMMENT '可用库存',
  `frozen_stock` INT NOT NULL DEFAULT 0 COMMENT '冻结库存',
  `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '已售库存',
  `version` INT DEFAULT 0 COMMENT '版本号(乐观锁)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';
```

##### stock_record - 库存变动记录表

```sql
CREATE TABLE `stock_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `order_no` VARCHAR(50) DEFAULT NULL COMMENT '订单编号',
  `type` TINYINT NOT NULL COMMENT '类型:1-入库,2-出库,3-冻结,4-解冻',
  `quantity` INT NOT NULL COMMENT '数量',
  `before_stock` INT NOT NULL COMMENT '变动前库存',
  `after_stock` INT NOT NULL COMMENT '变动后库存',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动记录表';
```

### 5. 支付数据库 (payment_db)

##### payment_order - 支付订单表

```sql
CREATE TABLE `payment_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付订单ID',
  `payment_no` VARCHAR(50) NOT NULL COMMENT '支付单号',
  `order_no` VARCHAR(50) NOT NULL COMMENT '业务订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `pay_type` TINYINT(1) NOT NULL COMMENT '支付类型:1-支付宝,2-微信',
  `status` TINYINT DEFAULT 0 COMMENT '支付状态:0-待支付,1-支付成功,2-支付失败,3-已退款',
  `trade_no` VARCHAR(100) DEFAULT NULL COMMENT '第三方交易号',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `callback_time` DATETIME DEFAULT NULL COMMENT '回调时间',
  `callback_content` TEXT COMMENT '回调内容',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单表';
```

## 🔑 索引设计原则

### 索引类型说明

| 索引类型 | 前缀      | 用途     | 示例                 |
|------|---------|--------|--------------------|
| 主键索引 | PRIMARY | 唯一标识   | PRIMARY KEY (`id`) |
| 唯一索引 | uk_     | 业务唯一约束 | uk_username        |
| 普通索引 | idx_    | 查询优化   | idx_create_time    |
| 联合索引 | idx_    | 多字段查询  | idx_user_status    |

### 索引设计原则

1. **主键设计**
    - 使用BIGINT自增主键
    - 避免使用UUID作为主键

2. **唯一索引**
    - 业务唯一字段必须创建唯一索引
    - 如：用户名、订单号、支付单号等

3. **查询索引**
    - 高频查询字段创建索引
    - WHERE条件字段创建索引
    - JOIN关联字段创建索引

4. **联合索引**
    - 遵循最左前缀原则
    - 选择性高的字段放在前面

## 📝 初始化脚本

### 执行顺序

```bash
# 1. 创建数据库
mysql -uroot -p < sql/init/infra/nacos/init.sql

# 2. 创建表结构（按顺序执行）
mysql -uroot -p < sql/init/user-service/init.sql
mysql -uroot -p < sql/init/product-service/init.sql
mysql -uroot -p < sql/init/order-service/init.sql
mysql -uroot -p < sql/init/stock-service/init.sql
mysql -uroot -p < sql/init/payment-service/init.sql

# 3. 初始化数据（可选）
mysql -uroot -p < sql/test/user-service/test.sql
mysql -uroot -p < sql/test/product-service/test.sql
mysql -uroot -p < sql/test/stock-service/test.sql
mysql -uroot -p < sql/test/order-service/test.sql
mysql -uroot -p < sql/test/payment-service/test.sql
```

### 脚本文件说明

| 文件名            | 说明       | 依赖       |
|----------------|----------|----------|
| sql/init/infra/nacos/init.sql     | Nacos configuration init script | none |
| sql/init/user-service/init.sql    | user-service schema             | none |
| sql/init/product-service/init.sql | product-service schema          | none |
| sql/init/order-service/init.sql   | order-service schema            | none |
| sql/init/stock-service/init.sql   | stock-service schema            | none |
| sql/init/payment-service/init.sql | payment-service schema          | none |
| sql/test/*/test.sql               | service test seed scripts       | matching init.sql |
| sql/archive/**                    | archived migration/monitoring/legacy scripts | none |

## 🔧 数据库优化建议

### 表设计优化

1. **字段类型选择**
    - 金额使用DECIMAL(10,2)
    - 时间使用DATETIME
    - 状态使用TINYINT
    - 长文本使用TEXT

2. **字段默认值**
    - 数值类型默认0
    - 时间类型默认CURRENT_TIMESTAMP
    - 状态类型默认正常状态

3. **逻辑删除**
    - 使用deleted字段实现软删除
    - 0-未删除，1-已删除

### 查询优化

1. **避免全表扫描**
    - 合理使用索引
    - 避免在WHERE子句中使用函数

2. **分页查询**
    - 大数据量使用游标分页
    - 避免深度分页

3. **批量操作**
    - 使用批量插入
    - 使用批量更新

### 事务处理

1. **事务隔离级别**
    - 默认使用READ COMMITTED
    - 特殊场景使用SERIALIZABLE

2. **锁机制**
    - 使用乐观锁(version字段)
    - 必要时使用悲观锁(SELECT ... FOR UPDATE)

## 📊 监控指标

### 关键监控指标

| 指标   | 阈值   | 说明           |
|------|------|--------------|
| 慢查询  | >1s  | 需要优化SQL或添加索引 |
| 连接数  | <80% | 防止连接池耗尽      |
| 锁等待  | <3s  | 避免长时间锁等待     |
| 死锁次数 | 0    | 出现死锁需要立即处理   |

### 定期维护

1. **每日任务**
    - 检查慢查询日志
    - 监控表空间使用率

2. **每周任务**
    - 分析表优化建议
    - 更新统计信息

3. **每月任务**
    - 清理历史数据
    - 重建碎片化索引

## 📚 相关文档

- [数据库开发规范](../RULE.md#数据库规范)
- [数据库优化指南](../docs/database-optimization.md)
- [分库分表方案](../docs/sharding-strategy.md)

---

<div align="center">

**最后更新**: 2025.01.25 | **维护者**: Cloud Platform Team

[返回文档中心](../docs/README.md) | [返回主页](../README.md)

</div>
