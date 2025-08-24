package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库明细表
 *
 * @TableName stock_in
 * @author cloud
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_in")
@Data
public class StockIn extends BaseEntity<StockIn> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 入库数量
     */
    @TableField(value = "quantity")
    private Integer quantity;
}