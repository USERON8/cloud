package com.cloud.common.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存展示VO
 */
@Data
public class StockVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 总库存量
     */
    private Integer stockQuantity;

    /**
     * 冻结库存量
     */
    private Integer frozenQuantity;

    /**
     * 可用库存量
     */
    private Integer availableQuantity;

    /**
     * 创建时间
     */
    private LocalDateTime createAT;

    /**
     * 更新时间
     */
    private LocalDateTime updateAT;
}