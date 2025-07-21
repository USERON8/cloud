package com.cloud.stock.module.vo;

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
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 总库存数量
     */
    private Integer stockCount;

    /**
     * 冻结库存数量
     */
    private Integer frozenCount;

    /**
     * 可用库存数量
     */
    private Integer availableCount;

    /**
     * 库存状态：0-缺货，1-不足，2-充足
     */
    private Integer stockStatus;

    /**
     * 库存状态描述
     */
    private String stockStatusDesc;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}