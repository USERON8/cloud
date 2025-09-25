package com.cloud.common.service;

import com.cloud.common.utils.UserContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户信息服务
 * 用于获取当前用户的完整信息，包括从JWT和数据库中获取用户详细信息
 *
 * @author what's up
 */
@Slf4j
@Service
public class UserInfoService {

    /**
     * 获取当前用户的基本信息
     *
     * @return 用户信息Map
     */
    public Map<String, Object> getCurrentUserBasicInfo() {
        log.debug("开始获取当前用户基本信息");

        Map<String, Object> userInfo = new HashMap<>();

        try {
            // 基本身份信息
            userInfo.put("userId", UserContextUtils.getCurrentUserId());
            userInfo.put("username", UserContextUtils.getCurrentUsername());
            userInfo.put("userType", UserContextUtils.getCurrentUserType());
            userInfo.put("nickname", UserContextUtils.getCurrentUserNickname());
            userInfo.put("status", UserContextUtils.getCurrentUserStatus());

            // 认证信息
            userInfo.put("clientId", UserContextUtils.getClientId());
            userInfo.put("tokenVersion", UserContextUtils.getTokenVersion());
            userInfo.put("isAuthenticated", UserContextUtils.isAuthenticated());

            // 权限信息
            Set<String> scopes = UserContextUtils.getCurrentUserScopes();
            userInfo.put("scopes", scopes);
            userInfo.put("scopeCount", scopes.size());

            // 用户类型标识
            userInfo.put("isRegularUser", UserContextUtils.isRegularUser());
            userInfo.put("isMerchant", UserContextUtils.isMerchant());
            userInfo.put("isAdmin", UserContextUtils.isAdmin());

            log.debug("成功获取用户基本信息，用户ID: {}", userInfo.get("userId"));

        } catch (Exception e) {
            log.error("获取用户基本信息失败", e);
            userInfo.put("error", "获取用户信息失败: " + e.getMessage());
        }

        return userInfo;
    }

    /**
     * 获取当前用户的敏感信息
     * 注意：敏感信息仅从JWT token中获取，不会从HTTP头传递
     *
     * @return 敏感信息Map
     */
    public Map<String, Object> getCurrentUserSensitiveInfo() {
        log.debug("开始获取当前用户敏感信息");

        Map<String, Object> sensitiveInfo = new HashMap<>();

        try {
            // 检查是否已认证
            if (!UserContextUtils.isAuthenticated()) {
                sensitiveInfo.put("error", "用户未认证，无法获取敏感信息");
                return sensitiveInfo;
            }

            // 敏感信息仅从JWT获取
            String phone = UserContextUtils.getCurrentUserPhone();
            if (StringUtils.hasText(phone)) {
                // 脱敏处理：只显示前3位和后4位
                String maskedPhone = maskPhoneNumber(phone);
                sensitiveInfo.put("phone", phone);  // 完整手机号
                sensitiveInfo.put("maskedPhone", maskedPhone);  // 脱敏手机号
            } else {
                sensitiveInfo.put("phone", null);
                sensitiveInfo.put("maskedPhone", null);
            }

            log.debug("成功获取用户敏感信息，用户ID: {}", UserContextUtils.getCurrentUserId());

        } catch (Exception e) {
            log.error("获取用户敏感信息失败", e);
            sensitiveInfo.put("error", "获取敏感信息失败: " + e.getMessage());
        }

        return sensitiveInfo;
    }

    /**
     * 获取当前用户的完整信息
     * 包括基本信息和敏感信息
     *
     * @return 完整用户信息Map
     */
    public Map<String, Object> getCurrentUserFullInfo() {
        log.debug("开始获取当前用户完整信息");

        Map<String, Object> fullInfo = new HashMap<>();

        // 获取基本信息
        Map<String, Object> basicInfo = getCurrentUserBasicInfo();
        fullInfo.putAll(basicInfo);

        // 获取敏感信息
        Map<String, Object> sensitiveInfo = getCurrentUserSensitiveInfo();
        fullInfo.putAll(sensitiveInfo);

        // 添加获取时间戳
        fullInfo.put("timestamp", System.currentTimeMillis());
        fullInfo.put("source", "UserInfoService");

        log.debug("成功获取用户完整信息，用户ID: {}", fullInfo.get("userId"));

        return fullInfo;
    }

    /**
     * 获取用户权限摘要信息
     *
     * @return 权限摘要信息
     */
    public Map<String, Object> getCurrentUserPermissionSummary() {
        log.debug("开始获取当前用户权限摘要");

        Map<String, Object> permissionSummary = new HashMap<>();

        try {
            // 检查认证状态
            boolean isAuthenticated = UserContextUtils.isAuthenticated();
            permissionSummary.put("isAuthenticated", isAuthenticated);

            if (!isAuthenticated) {
                permissionSummary.put("message", "用户未认证");
                return permissionSummary;
            }

            // 基本身份信息
            String userId = UserContextUtils.getCurrentUserId();
            String userType = UserContextUtils.getCurrentUserType();

            permissionSummary.put("userId", userId);
            permissionSummary.put("userType", userType);

            // 用户类型权限
            permissionSummary.put("isRegularUser", UserContextUtils.isRegularUser());
            permissionSummary.put("isMerchant", UserContextUtils.isMerchant());
            permissionSummary.put("isAdmin", UserContextUtils.isAdmin());

            // 权限范围分析
            Set<String> scopes = UserContextUtils.getCurrentUserScopes();
            permissionSummary.put("scopes", scopes);
            permissionSummary.put("scopeCount", scopes.size());

            // 常用权限检查
            Map<String, Boolean> commonPermissions = new HashMap<>();
            commonPermissions.put("canRead", UserContextUtils.hasScope("read"));
            commonPermissions.put("canWrite", UserContextUtils.hasScope("write"));
            commonPermissions.put("canUserRead", UserContextUtils.hasScope("user.read"));
            commonPermissions.put("canUserWrite", UserContextUtils.hasScope("user.write"));
            commonPermissions.put("canAdminRead", UserContextUtils.hasScope("admin.read"));
            commonPermissions.put("canAdminWrite", UserContextUtils.hasScope("admin.write"));
            permissionSummary.put("commonPermissions", commonPermissions);

            log.debug("成功获取用户权限摘要，用户ID: {}, 权限数量: {}", userId, scopes.size());

        } catch (Exception e) {
            log.error("获取用户权限摘要失败", e);
            permissionSummary.put("error", "获取权限信息失败: " + e.getMessage());
        }

        return permissionSummary;
    }

    /**
     * 检查当前用户是否拥有指定权限
     *
     * @param permission 权限名称
     * @return 是否拥有权限
     */
    public boolean hasPermission(String permission) {
        try {
            if (!UserContextUtils.isAuthenticated()) {
                return false;
            }
            return UserContextUtils.hasScope(permission);
        } catch (Exception e) {
            log.error("检查权限失败，权限: {}", permission, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限
     *
     * @param permissions 权限名称数组
     * @return 是否拥有任意权限
     */
    public boolean hasAnyPermission(String... permissions) {
        try {
            if (!UserContextUtils.isAuthenticated()) {
                return false;
            }
            return UserContextUtils.hasAnyScope(permissions);
        } catch (Exception e) {
            log.error("检查任意权限失败，权限: {}", java.util.Arrays.toString(permissions), e);
            return false;
        }
    }

    /**
     * 检查当前用户是否为指定类型
     *
     * @param userType 用户类型
     * @return 是否为指定类型
     */
    public boolean isUserType(String userType) {
        try {
            return UserContextUtils.isUserType(userType);
        } catch (Exception e) {
            log.error("检查用户类型失败，类型: {}", userType, e);
            return false;
        }
    }

    /**
     * 获取当前用户的调试信息
     * 仅用于开发和调试
     *
     * @return 调试信息字符串
     */
    public String getCurrentUserDebugInfo() {
        try {
            return UserContextUtils.getCurrentUserInfo();
        } catch (Exception e) {
            return "获取调试信息失败: " + e.getMessage();
        }
    }

    /**
     * 手机号脱敏处理
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    private String maskPhoneNumber(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }

        // 显示前3位和后4位，中间用*代替
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            // 其他长度的手机号，显示前2位和后2位
            return phone.substring(0, 2) + "***" + phone.substring(phone.length() - 2);
        }
    }
}
