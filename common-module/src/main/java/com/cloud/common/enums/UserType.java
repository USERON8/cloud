package com.cloud.common.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
public enum UserType {
    /**
     * 普通用户
     */
    USER("USER", "普通用户", "ROLE_USER"),
    
    /**
     * 商家用户
     */
    MERCHANT("MERCHANT", "商家", "ROLE_MERCHANT"),
    
    /**
     * 管理员用户
     */
    ADMIN("ADMIN", "管理员", "ROLE_ADMIN");

    private final String code;
    private final String description;
    private final String roleName;

    UserType(String code, String description, String roleName) {
        this.code = code;
        this.description = description;
        this.roleName = roleName;
    }

    /**
     * 根据编码获取用户类型
     *
     * @param code 编码
     * @return 用户类型
     */
    public static UserType fromCode(String code) {
        for (UserType userType : UserType.values()) {
            if (userType.getCode().equals(code)) {
                return userType;
            }
        }
        return USER; // 默认为普通用户
    }
    
    /**
     * 根据角色名称获取用户类型
     *
     * @param roleName 角色名称
     * @return 用户类型
     */
    public static UserType fromRoleName(String roleName) {
        for (UserType userType : UserType.values()) {
            if (userType.getRoleName().equals(roleName)) {
                return userType;
            }
        }
        return USER; // 默认为普通用户
    }
}