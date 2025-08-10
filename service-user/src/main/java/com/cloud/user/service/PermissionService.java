package com.cloud.user.service;

import com.cloud.common.utils.UserContextUtil;
import com.cloud.user.module.entity.User;
import com.cloud.user.module.enums.UserType;
import org.springframework.stereotype.Service;

/**
 * 权限检查服务类
 * 集中处理用户权限检查逻辑，避免在各个控制器中重复实现
 */
@Service("permissionService")
public class PermissionService {

    private final UserService userService;

    public PermissionService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 检查当前用户是否有权限操作指定用户
     *
     * @param targetUserId 目标用户ID
     * @return 是否有权限
     */
    public boolean hasPermission(Long targetUserId) {
        // 从统一用户上下文中获取当前用户ID
        Long currentUserId = UserContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            // 如果没有上下文信息，可能是内部调用或其他情况
            return true;
        }

        // 如果是管理员用户，则可以操作所有用户
        User currentUser = userService.getById(currentUserId);
        if (currentUser != null && UserType.ADMIN.getValue().equals(currentUser.getUserType())) {
            return true;
        }

        // 普通用户只能操作自己的信息
        return currentUserId.equals(targetUserId);
    }

    /**
     * 检查当前用户是否为管理员
     *
     * @return 是否为管理员
     */
    public boolean isAdmin() {
        Long currentUserId = UserContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        User currentUser = userService.getById(currentUserId);
        return currentUser != null && UserType.ADMIN.getValue().equals(currentUser.getUserType());
    }

    /**
     * 获取当前用户类型
     *
     * @return 用户类型枚举
     */
    public UserType getCurrentUserType() {
        Long currentUserId = UserContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }

        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getUserType() == null) {
            return null;
        }

        return UserType.fromValue(currentUser.getUserType());
    }
}