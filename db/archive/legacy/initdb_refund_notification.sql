-- 退款通知记录表
CREATE TABLE `refund_notification`
(
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    refund_id         BIGINT UNSIGNED NOT NULL COMMENT '退款ID',
    refund_no         VARCHAR(64)     NOT NULL COMMENT '退款单号',
    order_id          BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    order_no          VARCHAR(64)     NOT NULL COMMENT '订单号',
    user_id           BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    merchant_id       BIGINT UNSIGNED NULL COMMENT '商家ID',

    -- 通知信息
    notification_type VARCHAR(50)     NOT NULL COMMENT '通知类型：CREATED, AUDITED, PROCESSING, COMPLETED, CANCELLED',
    receiver_type     VARCHAR(50)     NOT NULL COMMENT '接收方类型：USER, MERCHANT',
    receiver_id       BIGINT UNSIGNED NOT NULL COMMENT '接收方ID',
    title             VARCHAR(200)    NOT NULL COMMENT '通知标题',
    content           TEXT            NOT NULL COMMENT '通知内容',

    -- 发送状态
    send_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '发送状态：0-待发送，1-发送成功，2-发送失败',
    send_time         DATETIME        NULL COMMENT '发送时间',
    send_channel      VARCHAR(50)     NULL COMMENT '发送渠道：SMS, EMAIL, PUSH, SYSTEM',
    fail_reason       VARCHAR(500)    NULL COMMENT '失败原因',
    retry_count       INT             NOT NULL DEFAULT 0 COMMENT '重试次数',

    -- 时间戳
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除,1-已删除',

    INDEX idx_refund_id (refund_id),
    INDEX idx_user_id (user_id),
    INDEX idx_merchant_id (merchant_id),
    INDEX idx_receiver (receiver_type, receiver_id),
    INDEX idx_send_status (send_status),
    INDEX idx_created_at (created_at)
) COMMENT ='退款通知记录表';
