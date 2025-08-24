package com.cloud.common.domain.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付DTO
 */
@Data
public class PaymentDTO {
    /**
     * 支付ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 状态：0-待支付，1-成功，2-失败，3-已退款
     */
    private Integer status;

    /**
     * 渠道：1-支付宝，2-微信，3-银行卡
     */
    private Integer channel;

    /**
     * 第三方流水号
     */
    private String transactionId;

    /**
     * 加密后的第三方流水号
     */
    private String encryptedTransactionId;

    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标识
     */
    private Integer deleted;
}