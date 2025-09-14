package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细表
 * 对应数据库表: order_item
 *
 * @author 代码规范团队
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "order_item")
@Data
public class OrderItem extends BaseEntity<OrderItem> {
    /**
     * 订单ID
     */
    @TableField(value = "order_id")
    private Long orderId;

    /**
     * 商品ID
     */
    @TableField(value = "product_id")
    private Long productId;

    /**
     * 商品快照（名称/价格/规格）
     * 使用JSON类型存储商品信息快照，防止商品信息变更后影响历史订单记录
     */
    @TableField(value = "product_snapshot")
    private String productSnapshot;

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

    // deleted 字段继承自 BaseEntity
}