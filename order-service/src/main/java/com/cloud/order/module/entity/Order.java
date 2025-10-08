package com.cloud.order.module.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cloud.common.domain.BaseEntity;
import com.cloud.order.enums.OrderStatusEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 * 对应数据库表: orders
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "orders")
@Data
public class Order extends BaseEntity<Order> {

    /**
     * 订单号（业务唯一编号）
     */
    @TableField(value = "order_no")
    private String orderNo;

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
     * 地址ID
     */
    @TableField(value = "address_id")
    private Long addressId;

    /**
     * 支付时间
     */
    @TableField(value = "pay_time")
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    @TableField(value = "ship_time")
    private LocalDateTime shipTime;

    /**
     * 完成时间
     */
    @TableField(value = "complete_time")
    private LocalDateTime completeTime;

    /**
     * 取消时间
     */
    @TableField(value = "cancel_time")
    private LocalDateTime cancelTime;

    /**
     * 取消原因
     */
    @TableField(value = "cancel_reason")
    private String cancelReason;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 店铺ID
     */
    @TableField(value = "shop_id")
    private Long shopId;

    // ===================== 业务工具方法 =====================

    /**
     * 生成订单号
     *
     * @return 订单号
     */
    public static String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%03d", (int) (Math.random() * 1000));
    }

    /**
     * 获取订单状态枚举
     *
     * @return 订单状态枚举
     */
    public OrderStatusEnum getStatusEnum() {
        return OrderStatusEnum.fromCode(this.status);
    }

    /**
     * 设置订单状态
     *
     * @param statusEnum 订单状态枚举
     */
    public void setStatusEnum(OrderStatusEnum statusEnum) {
        this.status = statusEnum != null ? statusEnum.getCode() : null;
    }

    /**
     * 检查订单是否可以支付
     *
     * @return 是否可以支付
     */
    public boolean canPay() {
        return getStatusEnum() != null && getStatusEnum().canPay();
    }

    /**
     * 检查订单是否可以取消
     *
     * @return 是否可以取消
     */
    public boolean canCancel() {
        return getStatusEnum() != null && getStatusEnum().canCancel();
    }

    /**
     * 检查订单是否可以发货
     *
     * @return 是否可以发货
     */
    public boolean canShip() {
        return getStatusEnum() != null && getStatusEnum().canShip();
    }

    /**
     * 检查订单是否可以完成
     *
     * @return 是否可以完成
     */
    public boolean canComplete() {
        return getStatusEnum() != null && getStatusEnum().canComplete();
    }

    /**
     * 检查订单是否为终态
     *
     * @return 是否为终态
     */
    public boolean isFinalStatus() {
        return getStatusEnum() != null && getStatusEnum().isFinalStatus();
    }
}
