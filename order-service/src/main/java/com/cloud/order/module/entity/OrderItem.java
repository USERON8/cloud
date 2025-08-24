package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细表
 *
 * @TableName order_item
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "order_item")
@Data
public class OrderItem extends BaseEntity<OrderItem> {
    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private String orderId;

    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private String productId;

    /**
     * 商品快照（名称/价格/规格）
     */
    @TableField(value = "product_snapshot")
    private Object productSnapshot;

    /**
     * 购买数量
     */
    @TableField(value = "quantity")
    private Integer quantity;

    /**
     * 购买时单价
     */
    @TableField(value = "price")
    private BigDecimal price;
    
    /**
     * 创建人ID
     */
    @TableField(value = "create_by")
    private Long createBy;
    
    /**
     * 更新人ID
     */
    @TableField(value = "update_by")
    private Long updateBy;
}