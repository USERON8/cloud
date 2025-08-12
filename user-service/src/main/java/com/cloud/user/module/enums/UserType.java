package com.cloud.user.module.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
public enum UserType {
    /**
     * 管理员
     */
    ADMIN("ADMIN"),

    /**
     * 商户
     */
    MERCHANT("MERCHANT"),

    /**
     * 普通用户
     */
    USER("USER");

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    /**
     * 根据字符串值获取对应的枚举类型
     *
     * @param value 字符串值
     * @return 对应的枚举类型，如果未找到则返回null
     */
    public static UserType fromValue(String value) {
        for (UserType userType : UserType.values()) {
            if (userType.getValue().equals(value)) {
                return userType;
            }
        }
        return null;
    }
}