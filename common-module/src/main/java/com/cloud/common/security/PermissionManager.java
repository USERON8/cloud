package com.cloud.common.security;

import com.cloud.common.annotation.RequireScope;
import com.cloud.common.annotation.RequireUserType;
import com.cloud.common.exception.PermissionException;
import com.cloud.common.utils.UserContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;

/**
 * 权限管理器
 * 用于统一管理权限检查逻辑，支持复杂的权限组合判断
 *
 * @author what's up
 */
@Slf4j
@Component
public class PermissionManager {

    /**
     * 检查用户认证状态
     *
     * @throws PermissionException 未认证异常
     */
    public void checkAuthentication() {
        checkAuthentication("用户未认证，请先登录");
    }

    /**
     * 检查用户认证状态
     *
     * @param message 自定义错误消息
     * @throws PermissionException 未认证异常
     */
    public void checkAuthentication(String message) {
        if (!UserContextUtils.isAuthenticated()) {
            log.warn("权限检查失败：用户未认证");
            throw StringUtils.hasText(message) ?
                    PermissionException.notAuthenticated(message) :
                    PermissionException.notAuthenticated();
        }
    }

    /**
     * 检查用户权限范围
     *
     * @param requiredScopes 必需的权限范围
     * @param mode           权限模式（ANY或ALL）
     * @throws PermissionException 权限不足异常
     */
    public void checkScope(String[] requiredScopes, RequireScope.ScopeMode mode) {
        checkScope(requiredScopes, mode, null);
    }

    /**
     * 检查用户权限范围
     *
     * @param requiredScopes 必需的权限范围
     * @param mode           权限模式（ANY或ALL）
     * @param customMessage  自定义错误消息
     * @throws PermissionException 权限不足异常
     */
    public void checkScope(String[] requiredScopes, RequireScope.ScopeMode mode, String customMessage) {
        // 先检查是否已认证
        checkAuthentication();

        if (requiredScopes == null || requiredScopes.length == 0) {
            return;
        }

        Set<String> userScopes = UserContextUtils.getCurrentUserScopes();
        boolean hasPermission = false;

        if (mode == RequireScope.ScopeMode.ALL) {
            // 必须拥有所有权限
            hasPermission = userScopes.containsAll(Arrays.asList(requiredScopes));
        } else {
            // 拥有任意一个权限即可（默认模式）
            hasPermission = Arrays.stream(requiredScopes)
                    .anyMatch(userScopes::contains);
        }

        if (!hasPermission) {
            String userId = UserContextUtils.getCurrentUserId();
            log.warn("权限检查失败：用户[{}]缺少必要权限。需要权限: {}, 用户权限: {}, 模式: {}",
                    userId, Arrays.toString(requiredScopes), userScopes, mode);

            if (StringUtils.hasText(customMessage)) {
                throw new PermissionException("INSUFFICIENT_SCOPE", customMessage);
            } else {
                throw PermissionException.insufficientScope(requiredScopes);
            }
        }

        log.debug("权限检查通过：用户[{}]拥有所需权限: {}", UserContextUtils.getCurrentUserId(), Arrays.toString(requiredScopes));
    }

    /**
     * 检查用户类型
     *
     * @param allowedTypes 允许的用户类型
     * @throws PermissionException 用户类型不匹配异常
     */
    public void checkUserType(RequireUserType.UserType[] allowedTypes) {
        checkUserType(allowedTypes, null);
    }

    /**
     * 检查用户类型
     *
     * @param allowedTypes  允许的用户类型
     * @param customMessage 自定义错误消息
     * @throws PermissionException 用户类型不匹配异常
     */
    public void checkUserType(RequireUserType.UserType[] allowedTypes, String customMessage) {
        // 先检查是否已认证
        checkAuthentication();

        if (allowedTypes == null || allowedTypes.length == 0) {
            return;
        }

        String currentUserType = UserContextUtils.getCurrentUserType();
        if (!StringUtils.hasText(currentUserType)) {
            log.warn("权限检查失败：无法获取当前用户类型");
            throw new PermissionException("USER_TYPE_UNKNOWN", "无法获取用户类型信息");
        }

        boolean typeMatched = Arrays.stream(allowedTypes)
                .anyMatch(type -> type.name().equals(currentUserType));

        if (!typeMatched) {
            String userId = UserContextUtils.getCurrentUserId();
            String allowedTypeNames = Arrays.toString(Arrays.stream(allowedTypes)
                    .map(Enum::name)
                    .toArray(String[]::new));

            log.warn("权限检查失败：用户[{}]类型不匹配。允许类型: {}, 当前类型: {}",
                    userId, allowedTypeNames, currentUserType);

            if (StringUtils.hasText(customMessage)) {
                throw new PermissionException("USER_TYPE_MISMATCH", customMessage);
            } else {
                throw PermissionException.userTypeMismatch(allowedTypeNames, currentUserType);
            }
        }

        log.debug("用户类型检查通过：用户[{}]类型为: {}", UserContextUtils.getCurrentUserId(), currentUserType);
    }

    /**
     * 检查是否为管理员
     *
     * @throws PermissionException 非管理员异常
     */
    public void checkAdmin() {
        checkUserType(new RequireUserType.UserType[]{RequireUserType.UserType.ADMIN}, "需要管理员权限");
    }

    /**
     * 检查是否为商户
     *
     * @throws PermissionException 非商户异常
     */
    public void checkMerchant() {
        checkUserType(new RequireUserType.UserType[]{RequireUserType.UserType.MERCHANT}, "需要商户权限");
    }

    /**
     * 检查是否为普通用户
     *
     * @throws PermissionException 非普通用户异常
     */
    public void checkRegularUser() {
        checkUserType(new RequireUserType.UserType[]{RequireUserType.UserType.USER}, "需要普通用户权限");
    }

    /**
     * 检查是否为管理员或商户
     *
     * @throws PermissionException 权限不足异常
     */
    public void checkAdminOrMerchant() {
        checkUserType(new RequireUserType.UserType[]{
                RequireUserType.UserType.ADMIN,
                RequireUserType.UserType.MERCHANT
        }, "需要管理员或商户权限");
    }

    /**
     * 检查特定用户ID权限
     * 用于检查用户是否只能操作自己的数据
     *
     * @param targetUserId 目标用户ID
     * @throws PermissionException 权限不足异常
     */
    public void checkSelfOperation(String targetUserId) {
        checkAuthentication();

        String currentUserId = UserContextUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentUserId)) {
            log.warn("权限检查失败：无法获取当前用户ID");
            throw new PermissionException("USER_ID_UNKNOWN", "无法获取用户ID信息");
        }

        if (!currentUserId.equals(targetUserId)) {
            log.warn("权限检查失败：用户[{}]尝试操作其他用户[{}]的数据", currentUserId, targetUserId);
            throw new PermissionException("FORBIDDEN_OPERATION", "只能操作自己的数据");
        }

        log.debug("自身操作检查通过：用户[{}]操作自己的数据", currentUserId);
    }

    /**
     * 检查自身操作或管理员权限
     *
     * @param targetUserId 目标用户ID
     * @throws PermissionException 权限不足异常
     */
    public void checkSelfOrAdmin(String targetUserId) {
        checkAuthentication();

        String currentUserId = UserContextUtils.getCurrentUserId();
        if (!StringUtils.hasText(currentUserId)) {
            log.warn("权限检查失败：无法获取当前用户ID");
            throw new PermissionException("USER_ID_UNKNOWN", "无法获取用户ID信息");
        }

        // 如果是操作自己的数据，直接通过
        if (currentUserId.equals(targetUserId)) {
            log.debug("自身操作检查通过：用户[{}]操作自己的数据", currentUserId);
            return;
        }

        // 否则检查是否为管理员
        if (UserContextUtils.isAdmin()) {
            log.debug("管理员权限检查通过：管理员[{}]操作用户[{}]的数据", currentUserId, targetUserId);
            return;
        }

        log.warn("权限检查失败：用户[{}]尝试操作其他用户[{}]的数据，且不是管理员", currentUserId, targetUserId);
        throw new PermissionException("FORBIDDEN_OPERATION", "只能操作自己的数据或需要管理员权限");
    }
}
