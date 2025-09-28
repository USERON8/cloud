package com.cloud.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 * 
 * @author CloudDevAgent  
 * @since 2025-09-26
 */
@Getter
@AllArgsConstructor
public enum UserType {
    
    /**
     * 普通用户
     */
    USER("USER", "普通用户"),
    
    /**
     * 管理员
     */
    ADMIN("ADMIN", "管理员"),
    
    /**
     * 超级管理员
     */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员");
    
    /**
     * 用户类型代码
     */
    private final String code;
    
    /**
     * 角色名称
     */
    private final String roleName;
    
    /**
     * 根据代码查找用户类型
     */
    public static UserType fromCode(String code) {
        for (UserType userType : UserType.values()) {
            if (userType.getCode().equals(code)) {
                return userType;
            }
        }
        return null;
    }
    
    /**
     * 根据角色名称查找用户类型
     */
    public static UserType fromRoleName(String roleName) {
        for (UserType userType : UserType.values()) {
            if (userType.getRoleName().equals(roleName)) {
                return userType;
            }
        }
        return null;
    }
}
