package com.cloud.common.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单项DTO
 * 用于服务间调用传输
 *
 * @author what's up
 * @since 1.0.0
 */
@Data
@Schema(description = "订单项DTO")
public class OrderItemDTO implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单项ID
     */
    @Schema(description = "订单项ID")
    private Long id;
    
    /**
     * 订单ID
     */
    @Schema(description = "订单ID")
    private Long orderId;
    
    /**
     * 商品ID
     */
    @Schema(description = "商品ID")
    private Long productId;
    
    /**
     * 商品名称
     */
    @Schema(description = "商品名称")
    private String productName;
    
    /**
     * 商品图片
     */
    @Schema(description = "商品图片")
    private String productImage;
    
    /**
     * 商品价格（下单时的价格）
     */
    @Schema(description = "商品价格（下单时的价格）")
    private BigDecimal productPrice;
    
    /**
     * 购买数量
     */
    @Schema(description = "购买数量")
    private Integer quantity;
    
    /**
     * 小计金额
     */
    @Schema(description = "小计金额")
    private BigDecimal subtotal;
    
    /**
     * 商品规格（JSON字符串）
     */
    @Schema(description = "商品规格（JSON字符串）")
    private String productSpecs;
}
