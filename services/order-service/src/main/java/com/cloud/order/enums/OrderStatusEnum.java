package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "Pending payment", "Order is created and waiting for payment"),
    PAID(1, "Paid", "Payment is successful and order can be shipped"),
    SHIPPED(2, "Shipped", "Order has been shipped"),
    COMPLETED(3, "Completed", "Order is completed"),
    CANCELLED(4, "Cancelled", "Order is cancelled");

    private final Integer code;
    private final String name;
    private final String description;

    public static OrderStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }

        for (OrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown order status code: " + code);
    }

    public boolean canPay() {
        return this == PENDING_PAYMENT;
    }

    public boolean canCancel() {
        return this == PENDING_PAYMENT || this == PAID;
    }

    public boolean canShip() {
        return this == PAID;
    }

    public boolean canComplete() {
        return this == SHIPPED;
    }

    public boolean isFinalStatus() {
        return this == COMPLETED || this == CANCELLED;
    }

    public OrderStatusEnum[] getNextPossibleStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> new OrderStatusEnum[]{PAID, CANCELLED};
            case PAID -> new OrderStatusEnum[]{SHIPPED, CANCELLED};
            case SHIPPED -> new OrderStatusEnum[]{COMPLETED};
            case COMPLETED, CANCELLED -> new OrderStatusEnum[]{};
        };
    }
}