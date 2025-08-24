package com.cloud.stock.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存冻结记录表
 *
 * @TableName stock_freeze
 * @author cloud
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "stock_freeze")
@Data
public class StockFreeze extends BaseEntity<StockFreeze> {
    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 冻结数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 关联订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 冻结状态：1-已冻结，2-已解冻
     */
    @TableField(value = "status")
    private Integer status;
}