package com.cloud.payment.v2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_order")
public class PaymentOrderV2 extends BaseEntity<PaymentOrderV2> {

    @TableField("payment_no")
    private String paymentNo;
    @TableField("main_order_no")
    private String mainOrderNo;
    @TableField("sub_order_no")
    private String subOrderNo;
    @TableField("user_id")
    private Long userId;
    @TableField("payment_status")
    private String paymentStatus;
    @TableField("payment_channel")
    private String paymentChannel;
    @TableField("total_amount")
    private BigDecimal totalAmount;
    @TableField("paid_amount")
    private BigDecimal paidAmount;
    @TableField("paid_at")
    private LocalDateTime paidAt;
    @TableField("transaction_no")
    private String transactionNo;
    @TableField("trace_id")
    private String traceId;
    @TableField("idempotency_key")
    private String idempotencyKey;
}

