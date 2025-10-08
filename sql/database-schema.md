# 云商城系统数据库表结构文档

## 概述

该文档详细描述了云商城系统的数据库表结构，包含6个主要数据库：用户数据库、商品数据库、订单数据库、支付数据库、库存数据库和配置管理数据库。

---

## 1. 用户数据库 (user_db)

### 1.1 用户表 (users)

**功能描述**：存储系统中所有用户的基本信息

| 字段名               | 数据类型                               | 约束                                                             | 描述                        |
|-------------------|------------------------------------|----------------------------------------------------------------|---------------------------|
| id                | BIGINT UNSIGNED                    | PRIMARY KEY                                                    | 用户ID                      |
| username          | VARCHAR(50)                        | NOT NULL UNIQUE                                                | 用户名                       |
| password          | VARCHAR(255)                       | NOT NULL                                                       | 加密密码                      |
| phone             | VARCHAR(20)                        | UNIQUE                                                         | 手机号                       |
| nickname          | VARCHAR(50)                        | NOT NULL                                                       | 昵称                        |
| avatar_url        | VARCHAR(255)                       |                                                                | 头像URL                     |
| email             | VARCHAR(100)                       | UNIQUE                                                         | 邮箱地址（用于GitHub登录）          |
| github_id         | BIGINT UNSIGNED                    |                                                                | GitHub用户ID（OAuth登录专用）     |
| github_username   | VARCHAR(100)                       |                                                                | GitHub用户名（OAuth登录专用）      |
| oauth_provider    | VARCHAR(20)                        |                                                                | OAuth提供商（github, wechat等） |
| oauth_provider_id | VARCHAR(100)                       |                                                                | OAuth提供商用户ID              |
| status            | TINYINT                            | NOT NULL DEFAULT 1                                             | 状态：0-禁用，1-启用              |
| user_type         | ENUM ('USER', 'MERCHANT', 'ADMIN') | NOT NULL DEFAULT 'USER'                                        | 用户类型：USER/MERCHANT/ADMIN  |
| created_at        | DATETIME                           | NOT NULL DEFAULT CURRENT_TIMESTAMP                             | 创建时间                      |
| updated_at        | DATETIME                           | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                      |
| deleted           | TINYINT                            | NOT NULL DEFAULT 0                                             | 软删除标记                     |

**索引**：

- idx_username (username)
- idx_phone (phone)
- idx_email (email)
- idx_status (status)
- idx_user_type (user_type)
- idx_github_id (github_id)
- idx_github_username (github_username)
- idx_oauth_provider (oauth_provider)
- idx_oauth_provider_combined (oauth_provider, oauth_provider_id)
- uk_github_id (github_id) UNIQUE
- uk_github_username (github_username) UNIQUE
- uk_oauth_provider_id (oauth_provider, oauth_provider_id) UNIQUE

### 1.2 用户地址表 (user_address)

**功能描述**：存储用户的收货地址信息

| 字段名            | 数据类型            | 约束                                 | 描述             |
|----------------|-----------------|------------------------------------|----------------|
| id             | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 地址ID           |
| user_id        | BIGINT UNSIGNED | NOT NULL                           | 用户ID           |
| consignee      | VARCHAR(50)     | NOT NULL                           | 收货人姓名          |
| phone          | VARCHAR(20)     | NOT NULL                           | 联系电话           |
| province       | VARCHAR(20)     | NOT NULL                           | 省份             |
| city           | VARCHAR(20)     | NOT NULL                           | 城市             |
| district       | VARCHAR(20)     | NOT NULL                           | 区县             |
| street         | VARCHAR(100)    | NOT NULL                           | 街道             |
| detail_address | VARCHAR(255)    | NOT NULL                           | 详细地址           |
| is_default     | TINYINT         | NOT NULL DEFAULT 0                 | 是否默认地址：0-否，1-是 |
| created_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间           |
| updated_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间           |
| deleted        | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记          |

**索引**：

- idx_user_id (user_id)
- idx_user_default (user_id, is_default)

**外键**：

- user_id 引用 users(id) ON DELETE CASCADE

### 1.3 管理员表 (admin)

**功能描述**：存储管理员账户信息

| 字段名        | 数据类型            | 约束                                 | 描述           |
|------------|-----------------|------------------------------------|--------------|
| id         | BIGINT UNSIGNED | PRIMARY KEY                        | 管理员ID        |
| username   | VARCHAR(50)     | NOT NULL UNIQUE                    | 用户名          |
| password   | VARCHAR(255)    | NOT NULL                           | 加密密码         |
| real_name  | VARCHAR(50)     | NOT NULL                           | 真实姓名         |
| phone      | VARCHAR(20)     |                                    | 联系电话         |
| role       | VARCHAR(20)     | NOT NULL DEFAULT 'ADMIN'           | 角色           |
| status     | TINYINT         | NOT NULL DEFAULT 1                 | 状态：0-禁用，1-启用 |
| created_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间         |
| updated_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间         |
| deleted    | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记        |

**索引**：

- idx_username (username)
- idx_role (role)
- idx_status (status)

### 1.4 商家表 (merchant)

**功能描述**：存储商家账户的基本信息

| 字段名           | 数据类型            | 约束                                 | 描述           |
|---------------|-----------------|------------------------------------|--------------|
| id            | BIGINT UNSIGNED | PRIMARY KEY                        | 商家ID         |
| username      | VARCHAR(50)     | NOT NULL UNIQUE                    | 用户名          |
| password      | VARCHAR(255)    | NOT NULL                           | 加密密码         |
| merchant_name | VARCHAR(100)    | NOT NULL                           | 商家名称         |
| phone         | VARCHAR(20)     |                                    | 联系电话         |
| status        | TINYINT         | NOT NULL DEFAULT 1                 | 状态：0-禁用，1-启用 |
| created_at    | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间         |
| updated_at    | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间         |
| deleted       | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记        |

**索引**：

- idx_username (username)
- idx_status (status)

### 1.5 商家认证表 (merchant_auth)

**功能描述**：存储商家实名认证信息

| 字段名                     | 数据类型            | 约束                                 | 描述                       |
|-------------------------|-----------------|------------------------------------|--------------------------|
| id                      | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键                       |
| merchant_id             | BIGINT UNSIGNED | NOT NULL                           | 商家ID                     |
| business_license_number | VARCHAR(50)     | NOT NULL                           | 营业执照号码                   |
| business_license_url    | VARCHAR(255)    | NOT NULL                           | 营业执照图片URL                |
| id_card_front_url       | VARCHAR(255)    | NOT NULL                           | 身份证正面URL                 |
| id_card_back_url        | VARCHAR(255)    | NOT NULL                           | 身份证反面URL                 |
| contact_phone           | VARCHAR(20)     | NOT NULL                           | 联系电话                     |
| contact_address         | VARCHAR(255)    | NOT NULL                           | 联系地址                     |
| auth_status             | TINYINT         | NOT NULL DEFAULT 0                 | 认证状态：0-待审核，1-审核通过，2-审核拒绝 |
| auth_remark             | VARCHAR(255)    |                                    | 审核备注                     |
| created_at              | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间                     |
| updated_at              | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间                     |
| deleted                 | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记                    |

**索引**：

- uk_merchant_id (merchant_id) UNIQUE
- idx_auth_status (auth_status)

### 1.6 商家结算账户表 (merchant_settlement_account)

**功能描述**：存储商家的结算银行账户信息

| 字段名            | 数据类型            | 约束                                 | 描述             |
|----------------|-----------------|------------------------------------|----------------|
| id             | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键             |
| merchant_id    | BIGINT UNSIGNED | NOT NULL                           | 商家ID           |
| account_name   | VARCHAR(100)    | NOT NULL                           | 账户名称           |
| account_number | VARCHAR(50)     | NOT NULL                           | 账户号码           |
| account_type   | TINYINT         | NOT NULL                           | 账户类型：1-对公，2-对私 |
| bank_name      | VARCHAR(100)    | NOT NULL                           | 开户银行           |
| is_default     | TINYINT         | NOT NULL DEFAULT 0                 | 是否默认账户：0-否，1-是 |
| status         | TINYINT         | NOT NULL DEFAULT 1                 | 状态：0-禁用，1-启用   |
| created_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间           |
| updated_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间           |
| deleted        | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记          |

**索引**：

- idx_merchant_id (merchant_id)
- idx_is_default (is_default)

---

## 2. 商品数据库 (product_db)

### 2.1 商品表 (products)

**功能描述**：存储商品的基本信息

| 字段名            | 数据类型            | 约束                                 | 描述           |
|----------------|-----------------|------------------------------------|--------------|
| id             | BIGINT UNSIGNED | PRIMARY KEY                        | 商品ID         |
| shop_id        | BIGINT UNSIGNED | NOT NULL                           | 店铺ID         |
| product_name   | VARCHAR(100)    | NOT NULL                           | 商品名称         |
| price          | DECIMAL(10, 2)  | NOT NULL                           | 售价           |
| stock_quantity | INT             | NOT NULL DEFAULT 0                 | 库存数量         |
| category_id    | BIGINT UNSIGNED |                                    | 分类ID         |
| status         | TINYINT         | NOT NULL DEFAULT 0                 | 状态：0-下架，1-上架 |
| created_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间         |
| updated_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间         |
| deleted        | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记        |

**索引**：

- idx_shop_id (shop_id)
- idx_status (status)
- idx_category_id (category_id)

### 2.2 商品分类表 (category)

**功能描述**：存储商品分类的层级结构

| 字段名        | 数据类型            | 约束                                 | 描述           |
|------------|-----------------|------------------------------------|--------------|
| id         | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 分类ID         |
| parent_id  | BIGINT UNSIGNED | NOT NULL DEFAULT 0                 | 父分类ID        |
| name       | VARCHAR(50)     | NOT NULL                           | 分类名称         |
| level      | TINYINT         | NOT NULL                           | 层级           |
| status     | TINYINT         | NOT NULL DEFAULT 1                 | 状态：0-禁用，1-启用 |
| created_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间         |
| updated_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间         |
| deleted    | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记        |

**索引**：

- idx_parent_id (parent_id)
- idx_status (status)
- idx_level (level)

### 2.3 商家店铺表 (merchant_shop)

**功能描述**：存储商家的店铺信息

| 字段名           | 数据类型            | 约束                                 | 描述           |
|---------------|-----------------|------------------------------------|--------------|
| id            | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 店铺ID         |
| merchant_id   | BIGINT UNSIGNED | NOT NULL                           | 商家ID         |
| shop_name     | VARCHAR(100)    | NOT NULL                           | 店铺名称         |
| avatar_url    | VARCHAR(255)    |                                    | 店铺头像URL      |
| description   | TEXT            |                                    | 店铺描述         |
| contact_phone | VARCHAR(20)     | NOT NULL                           | 客服电话         |
| address       | VARCHAR(255)    | NOT NULL                           | 详细地址         |
| status        | TINYINT         | NOT NULL DEFAULT 1                 | 状态：0-关闭，1-营业 |
| created_at    | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间         |
| updated_at    | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间         |
| deleted       | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记        |

**索引**：

- idx_merchant_id (merchant_id)
- idx_status (status)

---

## 3. 订单数据库 (order_db)

### 3.1 订单主表 (orders)

**功能描述**：存储订单的主要信息

| 字段名           | 数据类型            | 约束                                                             | 描述                               |
|---------------|-----------------|----------------------------------------------------------------|----------------------------------|
| id            | BIGINT UNSIGNED | PRIMARY KEY                                                    | 订单ID                             |
| order_no      | VARCHAR(32)     | NOT NULL UNIQUE                                                | 订单号（业务唯一编号）                      |
| user_id       | BIGINT UNSIGNED | NOT NULL                                                       | 用户ID                             |
| total_amount  | DECIMAL(10, 2)  | NOT NULL                                                       | 订单总额                             |
| pay_amount    | DECIMAL(10, 2)  | NOT NULL                                                       | 实付金额                             |
| status        | TINYINT         | NOT NULL                                                       | 状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消 |
| address_id    | BIGINT UNSIGNED | NOT NULL                                                       | 地址ID                             |
| pay_time      | DATETIME        | NULL                                                           | 支付时间                             |
| ship_time     | DATETIME        | NULL                                                           | 发货时间                             |
| complete_time | DATETIME        | NULL                                                           | 完成时间                             |
| cancel_time   | DATETIME        | NULL                                                           | 取消时间                             |
| cancel_reason | VARCHAR(255)    | NULL                                                           | 取消原因                             |
| remark        | VARCHAR(500)    | NULL                                                           | 备注                               |
| create_time   | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP                             | 创建时间                             |
| update_time   | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间                             |
| create_by     | BIGINT UNSIGNED | NULL                                                           | 创建人                              |
| update_by     | BIGINT UNSIGNED | NULL                                                           | 更新人                              |
| version       | INT             | NOT NULL DEFAULT 0                                             | 乐观锁版本号                           |
| deleted       | TINYINT         | NOT NULL DEFAULT 0                                             | 逻辑删除：0-未删除,1-已删除                 |

**索引**：

- uk_order_no (order_no) UNIQUE
- idx_user_status (user_id, status)
- idx_create_time (create_time)
- idx_status (status)
- idx_pay_time (pay_time)
- idx_ship_time (ship_time)
- idx_complete_time (complete_time)
- idx_cancel_time (cancel_time)

### 3.2 订单明细表 (order_item)

**功能描述**：存储订单中的商品详细信息

| 字段名              | 数据类型            | 约束                                                             | 描述               |
|------------------|-----------------|----------------------------------------------------------------|------------------|
| id               | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT                                     | 主键               |
| order_id         | BIGINT UNSIGNED | NOT NULL                                                       | 订单ID             |
| product_id       | BIGINT UNSIGNED | NOT NULL                                                       | 商品ID             |
| product_snapshot | JSON            | NOT NULL                                                       | 商品快照             |
| quantity         | INT             | NOT NULL                                                       | 购买数量             |
| price            | DECIMAL(10, 2)  | NOT NULL                                                       | 购买时单价            |
| create_time      | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP                             | 创建时间             |
| update_time      | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间             |
| create_by        | BIGINT UNSIGNED | NULL                                                           | 创建人              |
| update_by        | BIGINT UNSIGNED | NULL                                                           | 更新人              |
| version          | INT             | NOT NULL DEFAULT 0                                             | 乐观锁版本号           |
| deleted          | TINYINT         | NOT NULL DEFAULT 0                                             | 逻辑删除：0-未删除,1-已删除 |

**索引**：

- idx_order_product (order_id, product_id)
- idx_product_id (product_id)
- idx_create_time (create_time)

---

## 4. 支付数据库 (payment_db)

### 4.1 支付主表 (payment)

**功能描述**：存储支付记录的主要信息

| 字段名            | 数据类型            | 约束                                 | 描述                       |
|----------------|-----------------|------------------------------------|--------------------------|
| id             | BIGINT UNSIGNED | PRIMARY KEY                        | 支付ID                     |
| order_id       | BIGINT UNSIGNED | NOT NULL UNIQUE                    | 订单ID                     |
| user_id        | BIGINT UNSIGNED | NOT NULL                           | 用户ID                     |
| amount         | DECIMAL(10, 2)  | NOT NULL                           | 支付金额                     |
| status         | TINYINT         | NOT NULL                           | 状态：0-待支付，1-成功，2-失败，3-已退款 |
| channel        | TINYINT         | NOT NULL                           | 渠道：1-支付宝，2-微信，3-银行卡      |
| transaction_id | VARCHAR(100)    |                                    | 第三方流水号                   |
| trace_id       | VARCHAR(64)     |                                    | 跟踪ID                     |
| created_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间                     |
| updated_at     | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间                     |
| deleted        | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记                    |

**索引**：

- idx_order_user (order_id, user_id)
- idx_status (status)
- idx_created_at (created_at)
- idx_trace_id (trace_id)

### 4.2 支付流水表 (payment_flow)

**功能描述**：记录支付相关的资金流水

| 字段名        | 数据类型            | 约束                                 | 描述             |
|------------|-----------------|------------------------------------|----------------|
| id         | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键             |
| payment_id | BIGINT UNSIGNED | NOT NULL                           | 支付ID           |
| flow_type  | TINYINT         | NOT NULL                           | 流水类型：1-支付，2-退款 |
| amount     | DECIMAL(10, 2)  | NOT NULL                           | 变动金额           |
| trace_id   | VARCHAR(64)     |                                    | 跟踪ID           |
| created_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间           |
| updated_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间           |
| deleted    | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记          |

**索引**：

- idx_payment_flow (payment_id, flow_type)
- idx_trace_id (trace_id)

---

## 5. 库存数据库 (stock_db)

### 5.1 库存主表 (stock)

**功能描述**：管理商品的库存信息

| 字段名             | 数据类型            | 约束                                 | 描述                |
|-----------------|-----------------|------------------------------------|-------------------|
| id              | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键                |
| product_id      | BIGINT UNSIGNED | NOT NULL                           | 商品ID              |
| product_name    | VARCHAR(100)    | NOT NULL                           | 商品名称              |
| stock_quantity  | INT             | NOT NULL DEFAULT 0                 | 总库存量              |
| frozen_quantity | INT             | NOT NULL DEFAULT 0                 | 冻结库存量             |
| stock_status    | TINYINT         | NOT NULL DEFAULT 1                 | 状态：1-正常，2-缺货，3-下架 |
| version         | INT             | NOT NULL DEFAULT 0                 | 乐观锁版本号            |
| created_at      | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间              |
| updated_at      | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间              |
| deleted         | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记             |

**索引**：

- uk_product_id (product_id) UNIQUE
- idx_stock_status (stock_status)

### 5.2 入库记录表 (stock_in)

**功能描述**：记录商品的入库操作

| 字段名        | 数据类型            | 约束                                 | 描述    |
|------------|-----------------|------------------------------------|-------|
| id         | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键    |
| product_id | BIGINT UNSIGNED | NOT NULL                           | 商品ID  |
| quantity   | INT             | NOT NULL                           | 入库数量  |
| created_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间  |
| updated_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间  |
| deleted    | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记 |

**索引**：

- idx_product_id (product_id)

### 5.3 出库记录表 (stock_out)

**功能描述**：记录商品的出库操作

| 字段名        | 数据类型            | 约束                                 | 描述    |
|------------|-----------------|------------------------------------|-------|
| id         | BIGINT UNSIGNED | PRIMARY KEY AUTO_INCREMENT         | 主键    |
| product_id | BIGINT UNSIGNED | NOT NULL                           | 商品ID  |
| order_id   | BIGINT UNSIGNED | NOT NULL                           | 订单ID  |
| quantity   | INT             | NOT NULL                           | 出库数量  |
| created_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间  |
| updated_at | DATETIME        | NOT NULL DEFAULT CURRENT_TIMESTAMP | 更新时间  |
| deleted    | TINYINT         | NOT NULL DEFAULT 0                 | 软删除标记 |

**索引**：

- idx_product_id (product_id)
- idx_order_id (order_id)

---

## 6. Nacos配置数据库 (nacos_config)

### 6.1 配置信息表 (config_info)

**功能描述**：存储Nacos配置中心的配置信息

| 字段名                | 数据类型          | 约束                                 | 描述      |
|--------------------|---------------|------------------------------------|---------|
| id                 | bigint        | PRIMARY KEY AUTO_INCREMENT         | ID      |
| data_id            | varchar(255)  | NOT NULL                           | 配置标识    |
| group_id           | varchar(128)  |                                    | 组标识     |
| content            | longtext      | NOT NULL                           | 配置内容    |
| md5                | varchar(32)   |                                    | MD5值    |
| gmt_create         | datetime      | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间    |
| gmt_modified       | datetime      | NOT NULL DEFAULT CURRENT_TIMESTAMP | 修改时间    |
| src_user           | text          |                                    | 源用户     |
| src_ip             | varchar(50)   |                                    | 源IP     |
| app_name           | varchar(128)  |                                    | 应用名     |
| tenant_id          | varchar(128)  | DEFAULT ''                         | 租户字段    |
| c_desc             | varchar(256)  |                                    | 配置描述    |
| c_use              | varchar(64)   |                                    | 配置使用说明  |
| effect             | varchar(64)   |                                    | 配置生效的描述 |
| type               | varchar(64)   |                                    | 配置的类型   |
| c_schema           | text          |                                    | 配置的模式   |
| encrypted_data_key | varchar(1024) | NOT NULL DEFAULT ''                | 密钥      |

**索引**：

- uk_configinfo_datagrouptenant (data_id, group_id, tenant_id) UNIQUE

### 6.2 其他Nacos相关表

- **config_info_gray**: 灰度配置信息表
- **config_tags_relation**: 配置标签关系表
- **group_capacity**: 组容量信息表
- **his_config_info**: 历史配置信息表
- **permissions**: 权限表
- **roles**: 角色表
- **tenant_capacity**: 租户容量信息表
- **tenant_info**: 租户信息表
- **users**: Nacos用户表

---

## 数据库设计特点

### 1. 数据分库设计

系统采用按业务模块分库的设计：

- **user_db**: 用户相关数据
- **product_db**: 商品相关数据
- **order_db**: 订单相关数据
- **payment_db**: 支付相关数据
- **stock_db**: 库存相关数据
- **nacos_config**: 配置管理数据

### 2. 公共字段设计

所有业务表都包含以下公共字段：

- `created_at`: 创建时间
- `updated_at`: 更新时间
- `deleted`: 软删除标记

### 3. 主键设计

- 业务表主键统一使用 `BIGINT UNSIGNED`
- 部分表使用雪花算法生成分布式ID

### 4. 索引设计

- 为常用查询条件创建合适的索引
- 复合索引考虑查询模式和字段选择性

### 5. 数据类型选择

- 金额字段使用 `DECIMAL(10,2)` 确保精度
- 状态字段使用 `TINYINT` 节省存储空间
- 文本内容根据长度选择合适的varchar/text类型

### 6. 约束设计

- 重要字段添加NOT NULL约束
- 唯一性要求的字段添加UNIQUE约束
- 关键关联关系添加外键约束

这个设计支持高并发的电商业务场景，具有良好的可扩展性和维护性。
