package com.cloud.common.domain.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细DTO
 */
@Data
public class OrderItemDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品快照（名称/价格/规格）
     */
    private Object productSnapshot;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 购买时单价
     */
    private BigDecimal price;
    
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