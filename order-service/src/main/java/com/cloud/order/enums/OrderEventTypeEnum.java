package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单事件类型枚举
 * 定义订单处理过程中的各种事件类型
 *
 * @author CloudDevAgent
 * @since 2025-09-26
 */
@Getter
@AllArgsConstructor
public enum OrderEventTypeEnum {
    
    /**
     * 订单创建事件
     */
    ORDER_CREATED("ORDER_CREATED", "订单创建", "订单创建事件"),
    
    /**
     * 订单支付成功事件
     */
    ORDER_PAID("ORDER_PAID", "订单支付成功", "订单支付成功事件"),
    
    /**
     * 订单发货事件
     */
    ORDER_SHIPPED("ORDER_SHIPPED", "订单发货", "订单发货事件"),
    
    /**
     * 订单完成事件
     */
    ORDER_COMPLETED("ORDER_COMPLETED", "订单完成", "订单完成事件"),
    
    /**
     * 订单取消事件
     */
    ORDER_CANCELLED("ORDER_CANCELLED", "订单取消", "订单取消事件"),
    
    /**
     * 库存预扣减事件
     */
    STOCK_RESERVE("STOCK_RESERVE", "库存预扣减", "库存预扣减事件"),
    
    /**
     * 库存扣减确认事件
     */
    STOCK_CONFIRM("STOCK_CONFIRM", "库存扣减确认", "库存扣减确认事件"),
    
    /**
     * 库存回滚事件
     */
    STOCK_ROLLBACK("STOCK_ROLLBACK", "库存回滚", "库存回滚事件"),
    
    /**
     * 支付记录创建事件
     */
    PAYMENT_RECORD_CREATED("PAYMENT_RECORD_CREATED", "支付记录创建", "支付记录创建事件"),
    
    /**
     * 支付成功事件
     */
    PAYMENT_SUCCESS("PAYMENT_SUCCESS", "支付成功", "支付成功事件"),
    
    /**
     * 支付失败事件
     */
    PAYMENT_FAILED("PAYMENT_FAILED", "支付失败", "支付失败事件");
    
    /**
     * 事件代码
     */
    private final String code;
    
    /**
     * 事件名称
     */
    private final String name;
    
    /**
     * 事件描述
     */
    private final String description;
    
    /**
     * 根据事件代码获取枚举
     * 
     * @param code 事件代码
     * @return 订单事件类型枚举
     */
    public static OrderEventTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (OrderEventTypeEnum eventType : values()) {
            if (eventType.getCode().equals(code)) {
                return eventType;
            }
        }
        
        throw new IllegalArgumentException("未知的订单事件类型代码: " + code);
    }
}
