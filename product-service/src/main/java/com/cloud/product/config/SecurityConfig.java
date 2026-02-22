package com.cloud.product.config;

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
    }

    @Bean("securityExpressions")
    @ConditionalOnProperty(name = "app.security.expressions.enabled", havingValue = "true", matchIfMissing = true)
    public UnifiedSecurityExpressions securityExpressions() {
        return new UnifiedSecurityExpressions();
    }

    



    public static class UnifiedSecurityExpressions {

        



        public boolean isAdmin() {
            return SecurityPermissionUtils.isAdmin();
        }

        



        public boolean isMerchant() {
            return SecurityPermissionUtils.isMerchant();
        }

        



        public boolean isUser() {
            return SecurityPermissionUtils.isUser();
        }

        



        public boolean isAdminOrOwner(Long resourceUserId) {
            return SecurityPermissionUtils.isAdminOrOwner(resourceUserId);
        }

        



        public boolean isAdminOrMerchantOwner(Long merchantId) {
            return SecurityPermissionUtils.isAdminOrMerchantOwner(merchantId);
        }

        



        public boolean hasPermission(String permission) {
            return SecurityPermissionUtils.hasAuthority(permission);
        }

        



        public boolean hasAnyPermission(String... permissions) {
            return SecurityPermissionUtils.hasAnyAuthority(permissions);
        }

        



        public boolean hasUserType(String userType) {
            return SecurityPermissionUtils.hasUserType(userType);
        }

        



        public boolean isSameUser(String userId) {
            return SecurityPermissionUtils.isSameUser(userId);
        }

        



        public boolean canAccessResource(Long resourceUserId) {
            return SecurityPermissionUtils.canAccessResource(resourceUserId);
        }

        



        public boolean isAdminOrSelf(String userId) {
            return SecurityPermissionUtils.isAdmin() || SecurityPermissionUtils.isSameUser(userId);
        }

        



        public boolean isAdminOrMerchantData(Long merchantId) {
            return SecurityPermissionUtils.isAdmin() ||
                    (SecurityPermissionUtils.isMerchant() && SecurityPermissionUtils.isMerchantOwner(merchantId));
        }

        



        public boolean hasUserRole(String role) {
            return SecurityPermissionUtils.hasRole(role);
        }

        



        public boolean hasAnyUserRole(String... roles) {
            return SecurityPermissionUtils.hasAnyRole(roles);
        }

        


        public String getCurrentUserSummary() {
            return SecurityPermissionUtils.getUserSummary();
        }
    }
}

