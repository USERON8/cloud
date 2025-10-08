package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价类型枚举
 *
 * @author Cloud
 * @date 2025-01-07
 */
@Getter
@AllArgsConstructor
public enum ReviewType {

    /**
     * 商品评价
     */
    PRODUCT(1, "商品评价"),

    /**
     * 店铺评价
     */
    SHOP(2, "店铺评价");

    /**
     * 类型代码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 类型代码
     * @return 评价类型枚举
     */
    public static ReviewType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewType type : ReviewType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的评价类型: " + code);
    }
}

