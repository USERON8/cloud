package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 * 定义标准的订单状态流转和业务规则
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    
    /**
     * 待支付 - 订单已创建，等待用户支付
     */
    PENDING_PAYMENT(0, "待支付", "订单已创建，等待支付"),
    
    /**
     * 已支付 - 支付成功，准备发货
     */
    PAID(1, "已支付", "支付成功，准备发货"),
    
    /**
     * 已发货 - 商品已发出，等待用户收货
     */
    SHIPPED(2, "已发货", "商品已发出，等待收货"),
    
    /**
     * 已完成 - 订单已完成，交易结束
     */
    COMPLETED(3, "已完成", "订单已完成"),
    
    /**
     * 已取消 - 订单已取消
     */
    CANCELLED(4, "已取消", "订单已取消");
    
    /**
     * 状态码
     */
    private final Integer code;
    
    /**
     * 状态名称
     */
    private final String name;
    
    /**
     * 状态描述
     */
    private final String description;
    
    /**
     * 根据状态码获取枚举
     * 
     * @param code 状态码
     * @return 订单状态枚举
     */
    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        
        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("未知的订单状态码: " + code);
    }
    
    /**
     * 检查是否可以支付
     * 
     * @return 是否可以支付
     */
    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }
    
    /**
     * 检查是否可以取消
     * 
     * @return 是否可以取消
     */
    public boolean canCancel() {
        return this == PENDING_PAYMENT || this == PAID;
    }
    
    /**
     * 检查是否可以发货
     * 
     * @return 是否可以发货
     */
    public boolean canShip() {
        return this == PAID;
    }
    
    /**
     * 检查是否可以完成
     * 
     * @return 是否可以完成
     */
    public boolean canComplete() {
        return this == SHIPPED;
    }
    
    /**
     * 检查是否为终态
     * 
     * @return 是否为终态
     */
    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED;
    }
    
    /**
     * 获取下一个可能的状态
     * 
     * @return 下一个可能的状态数组
     */
    public OrderStatusEnum[] getNextPossibleStatuses() {
        switch (this) {
            case PENDING_PAYMENT:
                return new OrderStatusEnum[]{PAID, CANCELLED};
            case PAID:
                return new OrderStatusEnum[]{SHIPPED, CANCELLED};
            case SHIPPED:
                return new OrderStatusEnum[]{COMPLETED};
            case COMPLETED:
            case CANCELLED:
                return new OrderStatusEnum[]{};
            default:
                return new OrderStatusEnum[]{};
        }
    }
}
