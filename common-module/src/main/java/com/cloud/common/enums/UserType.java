package com.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 * 与数据库表users字段user_type的ENUM定义完全匹配
 *
 * @author CloudDevAgent
 * @since 2025-09-28
 */
@Getter
@AllArgsConstructor
public enum UserType {

    /**
     * 普通用户 - 对应数据库ENUM值: USER
     */
    USER("USER", "普通用户"),

    /**
     * 商家 - 对应数据库ENUM值: MERCHANT
     */
    MERCHANT("MERCHANT", "商家"),

    /**
     * 管理员 - 对应数据库ENUM值: ADMIN
     */
    ADMIN("ADMIN", "管理员");

    /**
     * 用户类型代码 (与数据库ENUM值一致)
     */
    private final String code;

    /**
     * 用户类型名称
     */
    private final String name;

    /**
     * 根据代码查找用户类型
     *
     * @param code 用户类型代码
     * @return 对应的用户类型枚举，未找到返回null
     */
    public static UserType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (UserType userType : UserType.values()) {
            if (userType.getCode().equals(code)) {
                return userType;
            }
        }
        return null;
    }

    /**
     * 根据名称查找用户类型
     *
     * @param name 用户类型名称
     * @return 对应的用户类型枚举，未找到返回null
     */
    public static UserType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (UserType userType : UserType.values()) {
            if (userType.getName().equals(name)) {
                return userType;
            }
        }
        return null;
    }

    /**
     * 检查是否为管理员类型
     *
     * @return true-是管理员，false-不是管理员
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * 检查是否为商家类型
     *
     * @return true-是商家，false-不是商家
     */
    public boolean isMerchant() {
        return this == MERCHANT;
    }

    /**
     * 检查是否为普通用户类型
     *
     * @return true-是普通用户，false-不是普通用户
     */
    public boolean isUser() {
        return this == USER;
    }
}
