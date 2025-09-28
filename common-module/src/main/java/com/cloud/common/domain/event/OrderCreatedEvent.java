package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单创建事件
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 订单状态
     */
    private Integer orderStatus;
    
    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 地址ID
     */
    private Long addressId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 追踪ID
     */
    private String traceId;
    
    /**
     * 订单项列表
     */
    private List<OrderItem> orderItems;
    
    /**
     * 订单项内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
    }
}
