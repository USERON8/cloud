package com.cloud.common.domain.dto.stock;

import com.cloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存分页查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StockPageDTO extends PageQuery {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称（模糊查询）
     */
    private String productName;

    /**
     * 库存状态：0-缺货，1-不足，2-充足
     */
    private Integer stockStatus;

    /**
     * 最小可用库存
     */
    private Integer minAvailableCount;

    /**
     * 最大可用库存
     */
    private Integer maxAvailableCount;

    public StockPageDTO() {
        // 设置默认排序：按更新时间降序
        super.setOrderBy("update_time");
        super.setOrderType("desc");
    }
}