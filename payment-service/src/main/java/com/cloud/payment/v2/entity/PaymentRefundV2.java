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
@TableName("payment_refund")
public class PaymentRefundV2 extends BaseEntity<PaymentRefundV2> {

    @TableField("refund_payment_no")
    private String refundPaymentNo;
    @TableField("after_sale_no")
    private String afterSaleNo;
    @TableField("payment_no")
    private String paymentNo;
    @TableField("main_order_no")
    private String mainOrderNo;
    @TableField("sub_order_no")
    private String subOrderNo;
    @TableField("user_id")
    private Long userId;
    @TableField("refund_status")
    private String refundStatus;
    @TableField("refund_amount")
    private BigDecimal refundAmount;
    @TableField("refund_channel")
    private String refundChannel;
    @TableField("refund_transaction_no")
    private String refundTransactionNo;
    @TableField("refunded_at")
    private LocalDateTime refundedAt;
    @TableField("reason")
    private String reason;
    @TableField("trace_id")
    private String traceId;
}

