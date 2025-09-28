package com.cloud.common.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单完成事件
 * 
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    
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
     * 订单总金额
     */
    private BigDecimal totalAmount;
    
    /**
     * 支付金额
     */
    private BigDecimal payAmount;
    
    /**
     * 订单状态
     */
    private Integer orderStatus;
    
    /**
     * 变更前状态
     */
    private Integer beforeStatus;
    
    /**
     * 变更后状态
     */
    private Integer afterStatus;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
    
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
        /**
         * 商品ID
         */
        private Long productId;
        
        /**
         * 商品名称
         */
        private String productName;
        
        /**
         * 数量
         */
        private Integer quantity;
        
        /**
         * 价格
         */
        private BigDecimal price;
    }
}
