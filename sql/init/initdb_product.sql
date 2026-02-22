-- ==================== 商品数据库初始化脚本 (product_db) ====================
-- 包含: 商品表、分类表、店铺表、SKU、属性、审核、品牌、评价
-- 版本: v2.0
-- 更新时间: 2025-01-16
-- ==================================================================================

DROP DATABASE IF EXISTS `product_db`;
CREATE DATABASE IF NOT EXISTS `product_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `product_db`;

-- ==================== 1. 商品分类表 ====================
CREATE TABLE `category`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `name`       VARCHAR(100)    NOT NULL COMMENT '分类名称',
    `level`      TINYINT         NOT NULL COMMENT '层级',
    `sort_order` INT                      DEFAULT 0 COMMENT '排序',
    `status`     TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_level` (`level`),
    INDEX `idx_parent_status` (`parent_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品分类表';

-- ==================== 2. 商家店铺表 ====================
CREATE TABLE `merchant_shop`
(
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '店铺ID',
    `merchant_id`   BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    `shop_name`     VARCHAR(200)    NOT NULL COMMENT '店铺名称',
    `avatar_url`    VARCHAR(500) COMMENT '店铺头像URL',
    `description`   TEXT COMMENT '店铺描述',
    `contact_phone` VARCHAR(20)     NOT NULL COMMENT '客服电话',
    `address`       VARCHAR(500)    NOT NULL COMMENT '详细地址',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 0-关闭, 1-营业',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_name_status` (`shop_name`(20), `status`),
    FULLTEXT INDEX `ft_shop_desc` (`description`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商家店铺表';

-- ==================== 3. 品牌表 ====================
CREATE TABLE `brand`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '品牌ID',
    `brand_name`       VARCHAR(200) NOT NULL COMMENT '品牌名称',
    `brand_name_en`    VARCHAR(200) COMMENT '品牌英文名',
    `logo_url`         VARCHAR(500) COMMENT '品牌Logo URL',
    `description`      VARCHAR(1000) COMMENT '品牌描述',
    `brand_story`      TEXT COMMENT '品牌故事',
    `official_website` VARCHAR(500) COMMENT '品牌官网',
    `country`          VARCHAR(100) COMMENT '品牌国家/地区',
    `founded_year`     INT COMMENT '成立年份',
    `status`           TINYINT      NOT NULL DEFAULT 1 COMMENT '品牌状态: 1-启用, 0-禁用',
    `is_hot`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否热门: 0-否, 1-是',
    `is_recommended`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否推荐: 0-否, 1-是',
    `product_count`    INT          NOT NULL DEFAULT 0 COMMENT '关联商品数量',
    `sort_order`       INT                   DEFAULT 0 COMMENT '排序',
    `seo_keywords`     VARCHAR(500) COMMENT 'SEO关键词',
    `seo_description`  VARCHAR(1000) COMMENT 'SEO描述',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_brand_name` (`brand_name`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_hot` (`is_hot`),
    INDEX `idx_is_recommended` (`is_recommended`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='品牌表';

-- ==================== 4. 商品主表 ====================
CREATE TABLE `products`
(
    `id`                  BIGINT UNSIGNED PRIMARY KEY COMMENT '商品ID',
    `shop_id`             BIGINT UNSIGNED NOT NULL COMMENT '店铺ID',
    `product_name`        VARCHAR(200)    NOT NULL COMMENT '商品名称',
    `product_description` TEXT COMMENT '商品描述',
    `price`               DECIMAL(10, 2)  NOT NULL COMMENT '售价',
    `original_price`      DECIMAL(10, 2) COMMENT '原价',
    `stock_quantity`      INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    `category_id`         BIGINT UNSIGNED COMMENT '分类ID',
    `brand_id`            BIGINT UNSIGNED COMMENT '品牌ID',
    `status`              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态: 0-下架, 1-上架',
    `sales_count`         INT             NOT NULL DEFAULT 0 COMMENT '销量',
    `view_count`          INT             NOT NULL DEFAULT 0 COMMENT '浏览量',
    `created_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_shop_id` (`shop_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_brand_id` (`brand_id`),
    INDEX `idx_price` (`price`),
    INDEX `idx_sales_count` (`sales_count`),
    INDEX `idx_shop_status` (`shop_id`, `status`),
    INDEX `idx_category_status` (`category_id`, `status`),
    INDEX `idx_brand_status` (`brand_id`, `status`),
    INDEX `idx_status_price` (`status`, `price`),
    INDEX `idx_name_status` (`product_name`(20), `status`),
    FULLTEXT INDEX `ft_product_name` (`product_name`),
    FULLTEXT INDEX `ft_product_desc` (`product_description`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品主表';

-- ==================== 5. 商品SKU表 ====================
CREATE TABLE `product_sku`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT 'SKU ID',
    `product_id`     BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `sku_code`       VARCHAR(100)    NOT NULL COMMENT 'SKU编码',
    `sku_name`       VARCHAR(200)    NOT NULL COMMENT 'SKU名称',
    `spec_values`    TEXT COMMENT '规格值组合(JSON格式)',
    `price`          DECIMAL(10, 2)  NOT NULL COMMENT 'SKU价格',
    `original_price` DECIMAL(10, 2) COMMENT 'SKU原价',
    `cost_price`     DECIMAL(10, 2) COMMENT 'SKU成本价',
    `stock_quantity` INT             NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales_quantity` INT             NOT NULL DEFAULT 0 COMMENT '已售数量',
    `image_url`      VARCHAR(500) COMMENT 'SKU图片URL',
    `weight`         INT COMMENT '重量(克)',
    `volume`         INT COMMENT '体积(立方厘米)',
    `barcode`        VARCHAR(100) COMMENT '条形码',
    `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT 'SKU状态: 1-正常, 2-缺货, 3-下架',
    `sort_order`     INT                      DEFAULT 0 COMMENT '排序',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    UNIQUE KEY `uk_sku_code` (`sku_code`),
    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_product_status` (`product_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品SKU表';

-- ==================== 6. SKU规格定义表 ====================
CREATE TABLE `sku_specification`
(
    `id`          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '规格ID',
    `spec_name`   VARCHAR(100)    NOT NULL COMMENT '规格名称',
    `spec_values` TEXT COMMENT '规格值列表(JSON格式)',
    `category_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '所属分类ID(0表示通用规格)',
    `spec_type`   TINYINT         NOT NULL DEFAULT 1 COMMENT '规格类型: 1-销售规格, 2-展示规格',
    `is_required` TINYINT         NOT NULL DEFAULT 0 COMMENT '是否必选: 0-否, 1-是',
    `sort_order`  INT                      DEFAULT 0 COMMENT '排序',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `description` VARCHAR(500) COMMENT '规格描述',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='SKU规格定义表';

-- ==================== 7. 商品属性表 ====================
CREATE TABLE `product_attribute`
(
    `id`                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '属性ID',
    `product_id`        BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `attr_name`         VARCHAR(100)    NOT NULL COMMENT '属性名称',
    `attr_value`        VARCHAR(500)    NOT NULL COMMENT '属性值',
    `attr_group`        VARCHAR(100) COMMENT '属性分组',
    `attr_type`         TINYINT         NOT NULL DEFAULT 1 COMMENT '属性类型: 1-文本, 2-数字, 3-日期, 4-图片, 5-富文本',
    `is_filterable`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否用于筛选',
    `is_list_visible`   TINYINT         NOT NULL DEFAULT 1 COMMENT '是否在列表页显示',
    `is_detail_visible` TINYINT         NOT NULL DEFAULT 1 COMMENT '是否在详情页显示',
    `sort_order`        INT                      DEFAULT 0 COMMENT '排序',
    `unit`              VARCHAR(20) COMMENT '单位',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_attr_group` (`attr_group`),
    INDEX `idx_is_filterable` (`is_filterable`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品属性表';

-- ==================== 8. 属性模板表 ====================
CREATE TABLE `attribute_template`
(
    `id`            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
    `template_name` VARCHAR(200)    NOT NULL COMMENT '模板名称',
    `category_id`   BIGINT UNSIGNED NOT NULL COMMENT '所属分类ID',
    `attributes`    TEXT COMMENT '属性列表(JSON格式)',
    `description`   VARCHAR(500) COMMENT '模板描述',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `is_system`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否系统预置',
    `usage_count`   INT             NOT NULL DEFAULT 0 COMMENT '使用次数',
    `creator_id`    BIGINT UNSIGNED COMMENT '创建人ID',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_category_id` (`category_id`),
    INDEX `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='属性模板表';

-- ==================== 9. 商品审核记录表 ====================
CREATE TABLE `product_audit`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '审核ID',
    `product_id`       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name`     VARCHAR(200) COMMENT '商品名称',
    `merchant_id`      BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    `merchant_name`    VARCHAR(200) COMMENT '商家名称',
    `audit_status`     VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING-待审核, APPROVED-审核通过, REJECTED-审核拒绝',
    `audit_type`       VARCHAR(20)     NOT NULL COMMENT '审核类型: CREATE-新建审核, UPDATE-更新审核, PRICE-价格变更审核',
    `submit_time`      DATETIME        NOT NULL COMMENT '提交时间',
    `auditor_id`       BIGINT UNSIGNED COMMENT '审核人ID',
    `auditor_name`     VARCHAR(100) COMMENT '审核人姓名',
    `audit_time`       DATETIME COMMENT '审核时间',
    `audit_comment`    VARCHAR(1000) COMMENT '审核意见',
    `reject_reason`    VARCHAR(1000) COMMENT '拒绝原因',
    `product_snapshot` TEXT COMMENT '商品快照(JSON格式)',
    `priority`         TINYINT                  DEFAULT 2 COMMENT '优先级: 1-低, 2-中, 3-高, 4-紧急',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_audit_status` (`audit_status`),
    INDEX `idx_submit_time` (`submit_time`),
    INDEX `idx_status_time` (`audit_status`, `submit_time`),
    INDEX `idx_merchant_status` (`merchant_id`, `audit_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品审核记录表';

-- ==================== 10. 品牌授权表 ====================
CREATE TABLE `brand_authorization`
(
    `id`              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '授权ID',
    `brand_id`        BIGINT UNSIGNED NOT NULL COMMENT '品牌ID',
    `brand_name`      VARCHAR(200) COMMENT '品牌名称',
    `merchant_id`     BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    `merchant_name`   VARCHAR(200) COMMENT '商家名称',
    `auth_type`       VARCHAR(50) COMMENT '授权类型: OFFICIAL-官方旗舰店, AUTHORIZED-授权经销商, DISTRIBUTOR-分销商',
    `auth_status`     VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '授权状态: PENDING-待审核, APPROVED-已授权, REJECTED-已拒绝, EXPIRED-已过期, REVOKED-已撤销',
    `certificate_url` VARCHAR(500) COMMENT '授权证书URL',
    `start_time`      DATETIME COMMENT '授权开始时间',
    `end_time`        DATETIME COMMENT '授权结束时间',
    `auditor_id`      BIGINT UNSIGNED COMMENT '审核人ID',
    `auditor_name`    VARCHAR(100) COMMENT '审核人姓名',
    `audit_time`      DATETIME COMMENT '审核时间',
    `audit_comment`   VARCHAR(1000) COMMENT '审核意见',
    `remark`          VARCHAR(500) COMMENT '备注',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_brand_id` (`brand_id`),
    INDEX `idx_merchant_id` (`merchant_id`),
    INDEX `idx_auth_status` (`auth_status`),
    INDEX `idx_end_time` (`end_time`),
    INDEX `idx_brand_merchant` (`brand_id`, `merchant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='品牌授权表';

-- ==================== 11. 商品评价表 ====================
CREATE TABLE `product_review`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    `product_id`       BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    `product_name`     VARCHAR(200) COMMENT '商品名称',
    `sku_id`           BIGINT UNSIGNED COMMENT 'SKU ID',
    `order_id`         BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    `order_no`         VARCHAR(50) COMMENT '订单号',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `user_nickname`    VARCHAR(100) COMMENT '用户昵称',
    `user_avatar`      VARCHAR(500) COMMENT '用户头像',
    `rating`           TINYINT         NOT NULL COMMENT '评分(1-5星)',
    `content`          TEXT COMMENT '评价内容',
    `images`           TEXT COMMENT '评价图片(JSON数组)',
    `tags`             VARCHAR(500) COMMENT '评价标签(JSON数组)',
    `is_anonymous`     TINYINT         NOT NULL DEFAULT 0 COMMENT '是否匿名',
    `audit_status`     VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态: PENDING-待审核, APPROVED-审核通过, REJECTED-审核拒绝',
    `audit_time`       DATETIME COMMENT '审核时间',
    `audit_comment`    VARCHAR(500) COMMENT '审核意见',
    `merchant_reply`   TEXT COMMENT '商家回复',
    `reply_time`       DATETIME COMMENT '商家回复时间',
    `like_count`       INT             NOT NULL DEFAULT 0 COMMENT '点赞数',
    `is_visible`       TINYINT         NOT NULL DEFAULT 1 COMMENT '是否显示',
    `review_type`      VARCHAR(20)     NOT NULL DEFAULT 'INITIAL' COMMENT '评价类型: INITIAL-首次评价, ADDITIONAL-追加评价',
    `parent_review_id` BIGINT UNSIGNED COMMENT '父评价ID',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT         NOT NULL DEFAULT 0 COMMENT '软删除标记',

    INDEX `idx_product_id` (`product_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_rating` (`rating`),
    INDEX `idx_audit_status` (`audit_status`),
    INDEX `idx_product_rating` (`product_id`, `rating`),
    INDEX `idx_product_visible` (`product_id`, `is_visible`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='商品评价表';

-- ==================== 初始化数据 ====================

-- 插入分类数据
INSERT INTO `category` (`id`, `parent_id`, `name`, `level`, `sort_order`, `status`)
VALUES (1, 0, '电子产品', 1, 1, 1),
       (2, 1, '手机', 2, 1, 1),
       (3, 1, '电脑', 2, 2, 1),
       (4, 0, '服装鞋帽', 1, 2, 1),
       (5, 4, '男装', 2, 1, 1),
       (6, 4, '女装', 2, 2, 1);

-- 插入品牌数据
INSERT INTO `brand` (`brand_name`, `brand_name_en`, `country`, `status`, `is_hot`, `is_recommended`)
VALUES ('华为', 'HUAWEI', '中国', 1, 1, 1),
       ('小米', 'Xiaomi', '中国', 1, 1, 1),
       ('苹果', 'Apple', '美国', 1, 1, 1),
       ('OPPO', 'OPPO', '中国', 1, 1, 0),
       ('vivo', 'vivo', '中国', 1, 1, 0);

-- 插入店铺数据
INSERT INTO `merchant_shop` (`merchant_id`, `shop_name`, `contact_phone`, `address`, `status`)
VALUES (1, '华为官方旗舰店', '400-123-4567', '深圳市南山区', 1),
       (2, '小米官方旗舰店', '400-234-5678', '北京市海淀区', 1),
       (3, '苹果官方旗舰店', '400-345-6789', '上海市浦东新区', 1);

-- 插入商品数据
INSERT INTO `products` (`id`, `shop_id`, `product_name`, `product_description`, `price`, `original_price`,
                        `stock_quantity`, `category_id`, `brand_id`, `status`, `sales_count`)
VALUES (1, 1, '华为Mate 60 Pro', '华为旗舰手机，强大影像系统', 6999.00, 7999.00, 100, 2, 1, 1, 1230),
       (2, 2, '小米14 Ultra', '小米徕卡光学影像旗舰', 5999.00, 6499.00, 150, 2, 2, 1, 856),
       (3, 3, 'iPhone 15 Pro Max', '钛金属设计，A17 Pro芯片', 9999.00, 10999.00, 80, 2, 3, 1, 2345),
       (4, 1, '华为MateBook X Pro', '轻薄办公本', 8999.00, 9999.00, 50, 3, 1, 1, 234),
       (5, 2, '小米笔记本Pro 14', '高性能轻薄本', 5499.00, 5999.00, 60, 3, 2, 1, 567);

COMMIT;

-- ==================== 索引优化说明 ====================
-- 1. 唯一索引: uk_sku_code - 保证SKU编码唯一性
-- 2. 单列索引: 用于单条件查询(status, category_id, brand_id等)
-- 3. 复合索引: 优化多条件查询
--    - idx_product_status: 店铺查询自己的上架商品
--    - idx_category_status: 分类页面查询
--    - idx_brand_status: 品牌页面查询
--    - idx_product_rating: 商品评价统计
-- 4. 全文索引: 支持商品名称和描述的全文检索
-- 5. 时间索引: 支持审核时间、授权到期等时间相关查询
