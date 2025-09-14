package com.cloud.common.config;

import com.cloud.common.exception.PermissionException;
import com.cloud.common.utils.SecurityContextUtils;
import org.springframework.stereotype.Component;

/**
 * 权限检查器
 * 提供统一的权限验证逻辑，避免在每个控制器方法中重复编写权限验证代码
 *
 * @author what's up
 */
@Component("permissionChecker")
public class PermissionChecker {

    /**
     * 检查当前用户是否具有指定权限
     *
     * @param permission 权限标识
     * @return 是否具有权限
     */
    public boolean hasPermission(String permission) {
        return SecurityContextUtils.hasPermission(permission);
    }

    /**
     * 断言当前用户具有指定权限
     *
     * @param permission 权限标识
     * @throws PermissionException 权限不足异常
     */
    public void assertPermission(String permission) throws PermissionException {
        if (!hasPermission(permission)) {
            throw new PermissionException("权限不足，需要权限: " + permission);
        }
    }

    /**
     * 检查当前用户是否具有指定角色
     *
     * @param role 角色标识
     * @return 是否具有角色
     */
    public boolean hasRole(String role) {
        return SecurityContextUtils.hasRole(role);
    }

    /**
     * 断言当前用户具有指定角色
     *
     * @param role 角色标识
     * @throws PermissionException 权限不足异常
     */
    public void assertRole(String role) throws PermissionException {
        if (!hasRole(role)) {
            throw new PermissionException("权限不足，需要角色: " + role);
        }
    }

    /**
     * 检查当前用户是否为指定用户类型
     *
     * @param userType 用户类型 (USER, MERCHANT, ADMIN)
     * @return 是否为指定用户类型
     */
    public boolean hasUserType(String userType) {
        return SecurityContextUtils.hasUserType(userType);
    }

    /**
     * 断言当前用户为指定用户类型
     *
     * @param userType 用户类型 (USER, MERCHANT, ADMIN)
     * @throws PermissionException 权限不足异常
     */
    public void assertUserType(String userType) throws PermissionException {
        if (!hasUserType(userType)) {
            throw new PermissionException("权限不足，需要用户类型: " + userType);
        }
    }

    /**
     * 获取当前用户的类型
     *
     * @return 用户类型
     */
    public String getCurrentUserType() {
        return SecurityContextUtils.getCurrentUserType();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public String getCurrentUserId() {
        return SecurityContextUtils.getCurrentUserId();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名
     */
    public String getCurrentUsername() {
        return SecurityContextUtils.getCurrentUsername();
    }

    /**
     * 检查是否为相同用户
     *
     * @param userId 用户ID
     * @return 是否为相同用户
     */
    public boolean isSameUser(String userId) {
        return SecurityContextUtils.isSameUser(userId);
    }
}