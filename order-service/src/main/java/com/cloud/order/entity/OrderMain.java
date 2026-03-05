package com.cloud.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_main")
public class OrderMain extends BaseEntity<OrderMain> {

    @TableField("main_order_no")
    private String mainOrderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("order_status")
    private String orderStatus;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("payable_amount")
    private BigDecimal payableAmount;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("cancelled_at")
    private LocalDateTime cancelledAt;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("remark")
    private String remark;

    @TableField("idempotency_key")
    private String idempotencyKey;
}
