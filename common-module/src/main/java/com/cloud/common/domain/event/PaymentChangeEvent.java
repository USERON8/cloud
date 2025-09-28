package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付变更事件
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChangeEvent {
    
    /**
     * 支付ID
     */
    private Long paymentId;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 金额
     */
    private BigDecimal amount;
    
    /**
     * 支付方式
     */
    private String paymentMethod;
    
    /**
     * 支付状态
     */
    private String status;
    
    /**
     * 变更前状态
     */
    private String beforeStatus;
    
    /**
     * 变更后状态
     */
    private String afterStatus;
    
    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 追踪ID
     */
    private String traceId;
}
