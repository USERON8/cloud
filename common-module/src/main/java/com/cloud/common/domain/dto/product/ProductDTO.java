package com.cloud.common.domain.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品DTO
 */
@Data
public class ProductDTO {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 售价
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stockCount;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 0-下架，1-上架
     */
    private Integer status;

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