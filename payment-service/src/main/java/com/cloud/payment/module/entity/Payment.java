package com.cloud.payment.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 支付主表
 *
 * @TableName payment
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "payment")
@Data
public class Payment extends BaseEntity<Payment> {
    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private String orderId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 支付金额
     */
    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 状态：0-待支付，1-成功，2-失败，3-已退款
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 渠道：1-支付宝，2-微信，3-银行卡
     */
    @TableField(value = "channel")
    private Integer channel;

    /**
     * 第三方流水号
     */
    @TableField(value = "transaction_id")
    private String transactionId;

    /**
     * 加密后的第三方流水号
     */
    @TableField(value = "encrypted_transaction_id")
    private String encryptedTransactionId;

    /**
     * 跟踪ID，用于幂等性处理
     */
    @TableField(value = "trace_id")
    private String traceId;
}