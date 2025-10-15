package com.cloud.order.enums;

import lombok.Getter;

/**
 * 订单退款状态枚举
 * 用于标识订单表中的refund_status字段
 *
 * @author CloudDevAgent
 * @since 2025-01-15
 */
@Getter
public enum OrderRefundStatusEnum {

    /**
     * 无退款
     */
    NO_REFUND(null, "无退款"),

    /**
     * 退款申请中
     */
    REFUND_APPLYING(0, "退款申请中"),

    /**
     * 退款中
     */
    REFUNDING(1, "退款中"),

    /**
     * 退款成功
     */
    REFUND_SUCCESS(2, "退款成功"),

    /**
     * 退款失败
     */
    REFUND_FAILED(3, "退款失败"),

    /**
     * 退款已关闭
     */
    REFUND_CLOSED(4, "退款已关闭");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态名称
     */
    private final String name;

    OrderRefundStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 对应的枚举
     */
    public static OrderRefundStatusEnum fromCode(Integer code) {
        if (code == null) {
            return NO_REFUND;
        }
        for (OrderRefundStatusEnum status : values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单退款状态码: " + code);
    }

    /**
     * 是否有退款
     *
     * @return true-有退款，false-无退款
     */
    public boolean hasRefund() {
        return this != NO_REFUND;
    }

    /**
     * 是否退款完成
     *
     * @return true-退款已完成（成功/失败/关闭），false-退款进行中
     */
    public boolean isRefundFinished() {
        return this == REFUND_SUCCESS || this == REFUND_FAILED || this == REFUND_CLOSED;
    }

    /**
     * 是否退款进行中
     *
     * @return true-退款申请中或退款中，false-其他状态
     */
    public boolean isRefundProcessing() {
        return this == REFUND_APPLYING || this == REFUNDING;
    }
}
