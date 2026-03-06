package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_refund")
public class PaymentRefundEntity extends BaseEntity<PaymentRefundEntity> {

    @TableField("refund_no")
    private String refundNo;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("after_sale_no")
    private String afterSaleNo;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("status")
    private String status;

    @TableField("reason")
    private String reason;

    @TableField("idempotency_key")
    private String idempotencyKey;

    @TableField("refunded_at")
    private LocalDateTime refundedAt;
}
