package com.cloud.payment.config;

import com.cloud.common.config.PermissionChecker;
import com.cloud.common.config.PermissionConfig;
import com.cloud.common.security.SecurityPermissionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Import({PermissionConfig.class, PermissionChecker.class})
public class SecurityConfig {

    public SecurityConfig() {
        log.info("🔐 [payment-service] 启用方法级权限控制 & 统一权限表达式");
    }

    @Bean("securityExpressions")
    @ConditionalOnProperty(name = "app.security.expressions.enabled", havingValue = "true", matchIfMissing = true)
    public UnifiedSecurityExpressions securityExpressions() {
        return new UnifiedSecurityExpressions();
    }

    /**
     * 权限方法表达式处理器
     * 扩展Spring Security的表达式处理能力
     */
    public static class UnifiedSecurityExpressions {

        /**
         * 检查是否为管理员
         * 使用方式: @PreAuthorize("@securityExpressions.isAdmin()")
         */
        public boolean isAdmin() {
            return SecurityPermissionUtils.isAdmin();
        }

        /**
         * 检查是否为商户
         * 使用方式: @PreAuthorize("@securityExpressions.isMerchant()")
         */
        public boolean isMerchant() {
            return SecurityPermissionUtils.isMerchant();
        }

        /**
         * 检查是否为普通用户
         * 使用方式: @PreAuthorize("@securityExpressions.isUser()")
         */
        public boolean isUser() {
            return SecurityPermissionUtils.isUser();
        }

        /**
         * 检查是否为管理员或资源所有者
         * 使用方式: @PreAuthorize("@securityExpressions.isAdminOrOwner(#userId)")
         */
        public boolean isAdminOrOwner(Long resourceUserId) {
            return SecurityPermissionUtils.isAdminOrOwner(resourceUserId);
        }

        /**
         * 检查是否为管理员或商户所有者
         * 使用方式: @PreAuthorize("@securityExpressions.isAdminOrMerchantOwner(#merchantId)")
         */
        public boolean isAdminOrMerchantOwner(Long merchantId) {
            return SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId);
        }

        /**
         * 检查是否具有指定权限
         * 使用方式: @PreAuthorize("@securityExpressions.hasPermission('user:read')")
         */
        public boolean hasPermission(String permission) {
            return SecurityPermissionUtils.hasAuthority(permission);
        }

        /**
         * 检查是否具有任意一个指定权限
         * 使用方式: @PreAuthorize("@securityExpressions.hasAnyPermission('user:read', 'user:write')")
         */
        public boolean hasAnyPermission(String... permissions) {
            return SecurityPermissionUtils.hasAnyAuthority(permissions);
        }

        /**
         * 检查是否具有指定用户类型
         * 使用方式: @PreAuthorize("@securityExpressions.hasUserType('ADMIN')")
         */
        public boolean hasUserType(String userType) {
            return SecurityPermissionUtils.hasUserType(userType);
        }

        /**
         * 检查是否为同一用户
         * 使用方式: @PreAuthorize("@securityExpressions.isSameUser(#userId)")
         */
        public boolean isSameUser(String userId) {
            return SecurityPermissionUtils.isSameUser(userId);
        }

        /**
         * 检查是否可以访问资源
         * 使用方式: @PreAuthorize("@securityExpressions.canAccessResource(#resourceUserId)")
         */
        public boolean canAccessResource(Long resourceUserId) {
            return SecurityPermissionUtils.canAccessResource(resourceUserId);
        }

        /**
         * 组合权限检查：管理员或自己的数据
         * 使用方式: @PreAuthorize("@securityExpressions.isAdminOrSelf(#userId)")
         */
        public boolean isAdminOrSelf(String userId) {
            return SecurityPermissionUtils.isAdmin() || SecurityPermissionUtils.isSameUser(userId);
        }

        /**
         * 组合权限检查：管理员或商户的数据
         * 使用方式: @PreAuthorize("@securityExpressions.isAdminOrMerchantData(#merchantId)")
         */
        public boolean isAdminOrMerchantData(Long merchantId) {
            return SecurityPermissionUtils.isAdmin() ||
                    (SecurityPermissionUtils.isMerchant() && SecurityPermissionUtils.isMerchantOwner(merchantId));
        }

        /**
         * 检查用户是否有角色
         * 使用方式: @PreAuthorize("@securityExpressions.hasUserRole('ADMIN')")
         */
        public boolean hasUserRole(String role) {
            return SecurityPermissionUtils.hasRole(role);
        }

        /**
         * 检查用户是否有任意一个角色
         * 使用方式: @PreAuthorize("@securityExpressions.hasAnyUserRole('ADMIN', 'MERCHANT')")
         */
        public boolean hasAnyUserRole(String... roles) {
            return SecurityPermissionUtils.hasAnyRole(roles);
        }

        /**
         * 获取当前用户摘要信息（用于日志）
         */
        public String getCurrentUserSummary() {
            return SecurityPermissionUtils.getUserSummary();
        }
    }
}

