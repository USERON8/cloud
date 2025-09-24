package com.cloud.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 简化订单创建DTO
 * 专门用于单商品订单的创建，简化参数减少复杂度
 * 适用于快速下单和演示事件驱动流程
 *
 * @author cloud
 * @since 1.0.0
 */
@Data
@Schema(description = "简化订单创建DTO")
public class SimpleOrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    @Schema(description = "商品ID", example = "1001")
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品名称
     */
    @Schema(description = "商品名称", example = "苹果 iPhone 15 Pro")
    private String productName;

    /**
     * 商品价格
     */
    @Schema(description = "商品价格", example = "8999.00")
    @NotNull(message = "商品价格不能为空")
    @Positive(message = "商品价格必须大于0")
    private BigDecimal productPrice;

    /**
     * 购买数量
     */
    @Schema(description = "购买数量", example = "1")
    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 收货地址ID（可选，使用默认地址）
     */
    @Schema(description = "收货地址ID", example = "1001")
    private Long addressId;

    /**
     * 订单备注（可选）
     */
    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;

    /**
     * 计算订单总金额
     *
     * @return 订单总金额
     */
    public BigDecimal getTotalAmount() {
        if (productPrice != null && quantity != null) {
            return productPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
}
