package com.cloud.common.domain.vo;

import lombok.Data;

/**
 * 库存统计信息VO
 */
@Data
public class StockStatisticsVO {

    /**
     * 总商品数量
     */
    private Long totalProducts;

    /**
     * 缺货商品数量
     */
    private Long outOfStockCount;

    /**
     * 库存不足商品数量
     */
    private Long lowStockCount;

    /**
     * 库存充足商品数量
     */
    private Long sufficientStockCount;

    /**
     * 总库存数量
     */
    private Long totalStockCount;

    /**
     * 总可用库存
     */
    private Long totalAvailableCount;

    /**
     * 总冻结库存
     */
    private Long totalFrozenCount;
}