package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付流水表
 *
 * @TableName payment_flow
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "payment_flow")
@Data
public class PaymentFlow extends BaseEntity<PaymentFlow> {
    /**
     * 支付ID
     */
    @TableField(value = "payment_id")
    private Long paymentId;

    /**
     * 流水类型：1-支付，2-退款
     */
    @TableField(value = "flow_type")
    private Integer flowType;

    /**
     * 变动金额
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 跟踪ID，用于幂等性处理
     */
    @TableField(value = "trace_id")
    private String traceId;
}