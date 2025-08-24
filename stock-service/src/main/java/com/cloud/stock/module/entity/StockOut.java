package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库明细表
 *
 * @TableName stock_out
 * @author cloud
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_out")
@Data
public class StockOut extends BaseEntity<StockOut> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 出库数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 关联订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;
}