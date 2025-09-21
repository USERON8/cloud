package com.cloud.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2.1兼容性检查工具
 * 用于验证UserDetails实现是否符合OAuth2.1标准
 *
 * @author what's up
 */
@Slf4j
@Component
public class OAuth2ComplianceChecker {

    /**
     * OAuth2.1标准必需的基础权限
     */
    private static final Set<String> REQUIRED_BASE_SCOPES = Set.of(
            "SCOPE_openid",
            "SCOPE_profile",
            "SCOPE_read"
    );

    /**
     * OAuth2.1标准角色前缀
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * OAuth2.1标准权限前缀
     */
    private static final String SCOPE_PREFIX = "SCOPE_";

    /**
     * 验证UserDetails是否符合OAuth2.1标准
     *
     * @param userDetails 用户详情
     * @param userType    用户类型
     * @return 验证结果
     */
    public OAuth2ComplianceResult validateCompliance(UserDetails userDetails, String userType) {
        log.debug("🔍 开始OAuth2.1兼容性检查, username: {}, userType: {}",
                userDetails.getUsername(), userType);

        OAuth2ComplianceResult result = new OAuth2ComplianceResult();
        result.setUsername(userDetails.getUsername());
        result.setUserType(userType);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        result.setAuthorities(authorityStrings);

        // 1. 检查基础权限
        validateBaseScopes(result, authorityStrings);

        // 2. 检查角色格式
        validateRoleFormat(result, authorityStrings);

        // 3. 检查权限格式
        validateScopeFormat(result, authorityStrings);

        // 4. 检查用户类型特定权限
        validateUserTypeSpecificPermissions(result, authorityStrings, userType);

        // 5. 检查权限继承
        validatePermissionInheritance(result, authorityStrings, userType);

        log.info("✅ OAuth2.1兼容性检查完成, username: {}, 合规: {}, 警告: {}",
                userDetails.getUsername(), result.isCompliant(), result.getWarnings().size());

        return result;
    }

    /**
     * 验证基础权限
     */
    private void validateBaseScopes(OAuth2ComplianceResult result, Set<String> authorities) {
        for (String requiredScope : REQUIRED_BASE_SCOPES) {
            if (!authorities.contains(requiredScope)) {
                result.addWarning("缺少OAuth2.1必需的基础权限: " + requiredScope);
            }
        }
    }

    /**
     * 验证角色格式
     */
    private void validateRoleFormat(OAuth2ComplianceResult result, Set<String> authorities) {
        List<String> roles = authorities.stream()
                .filter(auth -> auth.startsWith(ROLE_PREFIX))
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            result.addWarning("未找到任何角色 (ROLE_前缀)");
        }

        // 检查是否有ROLE_USER基础角色
        if (!authorities.contains("ROLE_USER")) {
            result.addWarning("缺少基础角色: ROLE_USER");
        }
    }

    /**
     * 验证权限格式
     */
    private void validateScopeFormat(OAuth2ComplianceResult result, Set<String> authorities) {
        List<String> scopes = authorities.stream()
                .filter(auth -> auth.startsWith(SCOPE_PREFIX))
                .collect(Collectors.toList());

        if (scopes.isEmpty()) {
            result.addError("未找到任何OAuth2.1权限 (SCOPE_前缀)");
        }

        result.setScopeCount(scopes.size());
    }

    /**
     * 验证用户类型特定权限
     */
    private void validateUserTypeSpecificPermissions(OAuth2ComplianceResult result,
                                                     Set<String> authorities, String userType) {
        switch (userType != null ? userType.toUpperCase() : "USER") {
            case "ADMIN":
                if (!authorities.contains("ROLE_ADMIN")) {
                    result.addWarning("管理员用户缺少ROLE_ADMIN角色");
                }
                if (!authorities.contains("SCOPE_admin.read") || !authorities.contains("SCOPE_admin.write")) {
                    result.addWarning("管理员用户缺少管理权限");
                }
                break;

            case "MERCHANT":
                if (!authorities.contains("ROLE_MERCHANT")) {
                    result.addWarning("商家用户缺少ROLE_MERCHANT角色");
                }
                if (!authorities.contains("SCOPE_merchant.read") || !authorities.contains("SCOPE_merchant.write")) {
                    result.addWarning("商家用户缺少商家权限");
                }
                if (!authorities.contains("SCOPE_product.read") || !authorities.contains("SCOPE_product.write")) {
                    result.addWarning("商家用户缺少产品管理权限");
                }
                break;

            case "USER":
            default:
                if (!authorities.contains("SCOPE_user.read")) {
                    result.addWarning("普通用户缺少基础读权限");
                }
                if (!authorities.contains("SCOPE_order.read") || !authorities.contains("SCOPE_order.write")) {
                    result.addWarning("普通用户缺少订单操作权限");
                }
                break;
        }
    }

    /**
     * 验证权限继承
     */
    private void validatePermissionInheritance(OAuth2ComplianceResult result,
                                               Set<String> authorities, String userType) {
        switch (userType != null ? userType.toUpperCase() : "USER") {
            case "ADMIN":
                // 管理员应该继承商家和用户权限
                if (!authorities.contains("SCOPE_merchant.read") || !authorities.contains("SCOPE_product.read")) {
                    result.addInfo("建议管理员继承商家权限以便管理商家");
                }
                if (!authorities.contains("SCOPE_order.read")) {
                    result.addInfo("建议管理员继承用户权限以便管理订单");
                }
                break;

            case "MERCHANT":
                // 商家应该继承用户权限
                if (!authorities.contains("SCOPE_order.read")) {
                    result.addInfo("建议商家继承用户订单权限");
                }
                break;
        }
    }

    /**
     * OAuth2.1兼容性检查结果
     */
    public static class OAuth2ComplianceResult {
        private String username;
        private String userType;
        private Set<String> authorities;
        private int scopeCount;
        private final List<String> errors = new java.util.ArrayList<>();
        private final List<String> warnings = new java.util.ArrayList<>();
        private final List<String> infos = new java.util.ArrayList<>();

        public boolean isCompliant() {
            return errors.isEmpty();
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addInfo(String info) {
            infos.add(info);
        }

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public Set<String> getAuthorities() {
            return authorities;
        }

        public void setAuthorities(Set<String> authorities) {
            this.authorities = authorities;
        }

        public int getScopeCount() {
            return scopeCount;
        }

        public void setScopeCount(int scopeCount) {
            this.scopeCount = scopeCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getInfos() {
            return infos;
        }

        @Override
        public String toString() {
            return String.format("OAuth2ComplianceResult{username='%s', userType='%s', compliant=%s, scopeCount=%d, errors=%d, warnings=%d}",
                    username, userType, isCompliant(), scopeCount, errors.size(), warnings.size());
        }
    }
}
