package com.cloud.stock.module.dto;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存分页查询DTO
 *
 * @author what's up
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageDTO extends PageQuery {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 库存状态：1-正常，2-缺货，3-下架
     */
    private Integer stockStatus;
}
