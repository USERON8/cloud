package com.cloud.common.domain.dto.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水DTO
 */
@Data
public class PaymentFlowDTO {
    /**
     * 支付流水ID
     */
    private Long id;
    
    /**
     * 支付ID
     */
    private String paymentId;

    /**
     * 流水类型：1-支付，2-退款
     */
    private Integer flowType;

    /**
     * 变动金额
     */
    private BigDecimal amount;
    
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