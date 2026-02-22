package com.cloud.order.enums;

import lombok.Getter;

@Getter
public enum RefundStatusEnum {

    PENDING_AUDIT(0, "Pending audit", "Waiting for merchant audit"),
    AUDIT_PASSED(1, "Audit passed", "Merchant approved the refund request"),
    AUDIT_REJECTED(2, "Audit rejected", "Merchant rejected the refund request"),
    RETURNING(3, "Returning", "User is returning the goods"),
    GOODS_RECEIVED(4, "Goods received", "Merchant confirmed goods receipt"),
    REFUNDING(5, "Refunding", "Refund is being processed"),
    COMPLETED(6, "Completed", "Refund completed successfully"),
    CANCELLED(7, "Cancelled", "Refund request cancelled by user"),
    CLOSED(8, "Closed", "Refund process closed");

    private final Integer code;
    private final String name;
    private final String description;

    RefundStatusEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static RefundStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public boolean isFinalStatus() {
        return this == AUDIT_REJECTED || this == COMPLETED || this == CANCELLED || this == CLOSED;
    }

    public boolean canCancel() {
        return this == PENDING_AUDIT || this == AUDIT_PASSED;
    }
}