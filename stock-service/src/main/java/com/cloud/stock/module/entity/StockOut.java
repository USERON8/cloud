package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库记录表
 *
 * @author what's up
 * @TableName stock_out
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
     * 订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 出库数量
     */
    @TableField(value = "quantity")
    private Integer quantity;
}

