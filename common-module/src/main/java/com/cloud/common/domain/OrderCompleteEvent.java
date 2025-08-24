package com.cloud.common.domain;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单完成事件对象
 * 用于在服务间传递订单完成信息，库存服务接收到此消息后需要扣减相应商品的库存
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompleteEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 商品ID
     */
    private Long productId;
    
    /**
     * 商品数量
     */
    private Integer quantity;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
    
    /**
     * 跟踪ID，用于幂等性处理
     */
    private String traceId;
}