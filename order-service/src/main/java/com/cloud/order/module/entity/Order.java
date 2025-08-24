package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单主表
 *
 * @TableName order
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "order")
@Data
public class Order extends BaseEntity<Order> {
    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 订单总额
     */
    @TableField(value = "total_amount")
    private BigDecimal totalAmount;

    /**
     * 实付金额
     */
    @TableField(value = "pay_amount")
    private BigDecimal payAmount;

    /**
     * 状态：0-待支付，1-已支付，2-已发货，3-已完成，4-已取消
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 地址快照
     */
    @TableField(value = "address_id")
    private String address_id;
}