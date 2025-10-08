-- ==================== 订单数据库 (order_db) ====================
DROP DATABASE IF EXISTS `order_db`;
CREATE DATABASE IF NOT EXISTS `order_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
USE `order_db`;

-- 订单主表
CREATE TABLE `orders`
(
    id            BIGINT UNSIGNED PRIMARY KEY COMMENT '订单ID',
    order_no      VARCHAR(32)     NOT NULL UNIQUE COMMENT '订单号（业务唯一编号）',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    total_amount  DECIMAL(10, 2)  NOT NULL COMMENT '订单总额',
    pay_amount    DECIMAL(10, 2)  NOT NULL COMMENT '实付金额',
    status        TINYINT         NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消',
    address_id    BIGINT UNSIGNED NOT NULL COMMENT '地址ID',
    pay_time      DATETIME        NULL COMMENT '支付时间',
    ship_time     DATETIME        NULL COMMENT '发货时间',
    complete_time DATETIME        NULL COMMENT '完成时间',
    cancel_time   DATETIME        NULL COMMENT '取消时间',
    cancel_reason VARCHAR(255)    NULL COMMENT '取消原因',
    remark        VARCHAR(500)    NULL COMMENT '备注',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT UNSIGNED NULL COMMENT '创建人',
    update_by     BIGINT UNSIGNED NULL COMMENT '更新人',
    version       INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted       TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_status (user_id, status),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status),
    INDEX idx_pay_time (pay_time),
    INDEX idx_ship_time (ship_time),
    INDEX idx_complete_time (complete_time),
    INDEX idx_cancel_time (cancel_time)
) COMMENT ='订单主表';

-- 订单明细表
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

-- =============================================
-- 退货相关表
-- =============================================

-- 退货单表
CREATE TABLE `refunds`
(
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '退货单ID',
    refund_no              VARCHAR(64)    NOT NULL UNIQUE COMMENT '退货单号',
    order_id               BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no               VARCHAR(64)    NOT NULL COMMENT '订单号',
    user_id                BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    merchant_id            BIGINT UNSIGNED NULL COMMENT '商家ID',

    -- 退货信息
    refund_type            TINYINT        NOT NULL DEFAULT 1 COMMENT '退货类型：1-仅退款，2-退货退款',
    refund_reason          VARCHAR(255)   NOT NULL COMMENT '退货原因',
    refund_description     TEXT           NULL COMMENT '详细说明',
    refund_amount          DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
    refund_quantity        INT            NOT NULL DEFAULT 1 COMMENT '退货数量',

    -- 审核信息
    status                 TINYINT        NOT NULL DEFAULT 0 COMMENT '退货状态：0-待审核，1-审核通过，2-审核拒绝，3-退货中，4-已收货，5-退款中，6-已完成，7-已取消，8-已关闭',
    audit_time             DATETIME       NULL COMMENT '审核时间',
    audit_remark           VARCHAR(500)   NULL COMMENT '审核备注',

    -- 物流信息
    logistics_company      VARCHAR(100)   NULL COMMENT '物流公司',
    logistics_no           VARCHAR(100)   NULL COMMENT '物流单号',

    -- 退款信息
    refund_time            DATETIME       NULL COMMENT '退款时间',
    refund_channel         VARCHAR(50)    NULL COMMENT '退款渠道',
    refund_transaction_no  VARCHAR(100)   NULL COMMENT '退款交易号',

    -- 时间戳
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted             TINYINT        NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',

    UNIQUE KEY uk_refund_no (refund_no),
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='退货单表';

-- 退货商品明细表
CREATE TABLE `refund_items`
(
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '退货明细ID',
    refund_id     BIGINT UNSIGNED NOT NULL COMMENT '退货单ID',
    order_item_id BIGINT UNSIGNED NOT NULL COMMENT '订单明细ID',
    product_id    BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
    product_name  VARCHAR(255)    NOT NULL COMMENT '商品名称',
    product_image VARCHAR(500)    NULL COMMENT '商品图片',
    product_price DECIMAL(10, 2)  NOT NULL COMMENT '商品单价',
    quantity      INT             NOT NULL COMMENT '退货数量',
    refund_amount DECIMAL(10, 2)  NOT NULL COMMENT '退款金额',

    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_refund_id (refund_id),
    INDEX idx_order_item_id (order_item_id),
    INDEX idx_product_id (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='退货商品明细表';

-- 退货日志表
CREATE TABLE `refund_logs`
(
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    refund_id      BIGINT UNSIGNED NOT NULL COMMENT '退货单ID',
    operator_id    BIGINT UNSIGNED NULL COMMENT '操作人ID',
    operator_name  VARCHAR(100)    NULL COMMENT '操作人姓名',
    operator_type  TINYINT         NOT NULL COMMENT '操作人类型：1-用户，2-商家，3-系统',
    action         VARCHAR(50)     NOT NULL COMMENT '操作动作',
    before_status  TINYINT         NULL COMMENT '操作前状态',
    after_status   TINYINT         NULL COMMENT '操作后状态',
    remark         VARCHAR(500)    NULL COMMENT '备注说明',

    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_refund_id (refund_id),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='退货日志表';

-- 退货图片表
CREATE TABLE `refund_images`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    refund_id  BIGINT UNSIGNED NOT NULL COMMENT '退货单ID',
    image_url  VARCHAR(500)    NOT NULL COMMENT '图片URL',
    image_type TINYINT         NOT NULL COMMENT '图片类型：1-问题图片，2-物流凭证',

    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_refund_id (refund_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='退货图片表';

-- =============================================
-- 评价相关表
-- =============================================

-- 评价表
CREATE TABLE `reviews`
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评价ID',
    review_no       VARCHAR(64)    NOT NULL UNIQUE COMMENT '评价编号',
    order_id        BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(64)    NOT NULL COMMENT '订单号',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    merchant_id     BIGINT UNSIGNED NOT NULL COMMENT '商家ID',

    -- 评价类型
    review_type     TINYINT        NOT NULL COMMENT '评价类型：1-商品评价，2-店铺评价',
    product_id      BIGINT UNSIGNED NULL COMMENT '商品ID（商品评价时必填）',
    order_item_id   BIGINT UNSIGNED NULL COMMENT '订单明细ID（商品评价时必填）',

    -- 评价内容
    rating          TINYINT        NOT NULL COMMENT '评分：1-5星',
    content         TEXT           NOT NULL COMMENT '评价内容',
    is_anonymous    TINYINT        NOT NULL DEFAULT 0 COMMENT '是否匿名：0-否，1-是',

    -- 追加评价
    append_content  TEXT           NULL COMMENT '追加评价内容',
    append_time     DATETIME       NULL COMMENT '追加评价时间',

    -- 商家回复
    reply_content   TEXT           NULL COMMENT '商家回复内容',
    reply_time      DATETIME       NULL COMMENT '回复时间',
    reply_user_id   BIGINT UNSIGNED NULL COMMENT '回复人ID',

    -- 状态
    status          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-已隐藏，3-已删除',

    -- 统计
    helpful_count   INT            NOT NULL DEFAULT 0 COMMENT '有用数',
    view_count      INT            NOT NULL DEFAULT 0 COMMENT '浏览数',

    -- 时间戳
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',

    UNIQUE KEY uk_review_no (review_no),
    UNIQUE KEY uk_order_product (order_id, product_id),
    UNIQUE KEY uk_order_merchant (order_id, merchant_id, review_type),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_product_id (product_id),
    INDEX idx_order_id (order_id),
    INDEX idx_rating (rating),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评价表';

-- 评价图片表
CREATE TABLE `review_images`
(
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '图片ID',
    review_id  BIGINT UNSIGNED NOT NULL COMMENT '评价ID',
    image_url  VARCHAR(500)    NOT NULL COMMENT '图片URL',
    image_type TINYINT         NOT NULL DEFAULT 1 COMMENT '图片类型：1-评价图片，2-追加评价图片',
    sort_order INT             NOT NULL DEFAULT 0 COMMENT '排序',

    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_review_id (review_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评价图片表';

-- 评价统计表
CREATE TABLE `review_statistics`
(
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '统计ID',
    target_id           BIGINT UNSIGNED NOT NULL COMMENT '目标ID（商品ID或商家ID）',
    target_type         TINYINT         NOT NULL COMMENT '目标类型：1-商品，2-店铺',

    -- 评价统计
    total_count         INT             NOT NULL DEFAULT 0 COMMENT '总评价数',
    rating_1_count      INT             NOT NULL DEFAULT 0 COMMENT '1星评价数',
    rating_2_count      INT             NOT NULL DEFAULT 0 COMMENT '2星评价数',
    rating_3_count      INT             NOT NULL DEFAULT 0 COMMENT '3星评价数',
    rating_4_count      INT             NOT NULL DEFAULT 0 COMMENT '4星评价数',
    rating_5_count      INT             NOT NULL DEFAULT 0 COMMENT '5星评价数',
    average_rating      DECIMAL(3, 2)   NOT NULL DEFAULT 0.00 COMMENT '平均评分',

    -- 评价类型统计
    good_review_count   INT             NOT NULL DEFAULT 0 COMMENT '好评数（4-5星）',
    medium_review_count INT             NOT NULL DEFAULT 0 COMMENT '中评数（3星）',
    bad_review_count    INT             NOT NULL DEFAULT 0 COMMENT '差评数（1-2星）',
    with_image_count    INT             NOT NULL DEFAULT 0 COMMENT '带图评价数',
    append_count        INT             NOT NULL DEFAULT 0 COMMENT '追加评价数',

    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_target (target_id, target_type),
    INDEX idx_average_rating (average_rating)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评价统计表';
