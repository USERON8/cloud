package com.cloud.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价状态枚举
 *
 * @author Cloud
 * @date 2025-01-07
 */
@Getter
@AllArgsConstructor
public enum ReviewStatus {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 已隐藏
     */
    HIDDEN(2, "已隐藏"),

    /**
     * 已删除
     */
    DELETED(3, "已删除");

    /**
     * 状态代码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 状态代码
     * @return 评价状态枚举
     */
    public static ReviewStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewStatus status : ReviewStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的评价状态: " + code);
    }
}

