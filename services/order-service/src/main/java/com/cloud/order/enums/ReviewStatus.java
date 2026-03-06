package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStatus {

    NORMAL(1, "Normal"),
    HIDDEN(2, "Hidden"),
    DELETED(3, "Deleted");

    private final Integer code;
    private final String description;

    public static ReviewStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown review status code: " + code);
    }
}
