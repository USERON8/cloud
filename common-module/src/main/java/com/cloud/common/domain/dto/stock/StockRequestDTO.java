package com.cloud.common.domain.dto.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存请求DTO
 *
 * @author what's up
 */
@Data
public class StockRequestDTO {
    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 总库存量
     */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stockQuantity;

    /**
     * 冻结库存量
     */
    @Min(value = 0, message = "冻结库存数量不能为负数")
    private Integer frozenQuantity = 0;

    /**
     * 库存状态：1-正常，2-缺货，3-下架
     */
    private Integer stockStatus = 1;
}
