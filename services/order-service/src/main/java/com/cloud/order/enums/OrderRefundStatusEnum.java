package com.cloud.order.enums;

import lombok.Getter;

@Getter
public enum OrderRefundStatusEnum {

    NO_REFUND(null, "No refund"),
    REFUND_APPLYING(0, "Refund applying"),
    REFUNDING(1, "Refunding"),
    REFUND_SUCCESS(2, "Refund success"),
    REFUND_FAILED(3, "Refund failed"),
    REFUND_CLOSED(4, "Refund closed");

    private final Integer code;
    private final String name;

    OrderRefundStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static OrderRefundStatusEnum fromCode(Integer code) {
        if (code == null) {
            return NO_REFUND;
        }
        for (OrderRefundStatusEnum status : values()) {
            if (code.equals(status.code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order refund status code: " + code);
    }

    public boolean hasRefund() {
        return this != NO_REFUND;
    }

    public boolean isRefundFinished() {
        return this == REFUND_SUCCESS || this == REFUND_FAILED || this == REFUND_CLOSED;
    }

    public boolean isRefundProcessing() {
        return this == REFUND_APPLYING || this == REFUNDING;
    }
}