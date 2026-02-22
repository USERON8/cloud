package com.cloud.auth.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OAuth2ComplianceChecker {

    private static final Set<String> REQUIRED_BASE_SCOPES = Set.of(
            "SCOPE_openid",
            "SCOPE_profile",
            "SCOPE_read"
    );
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";

    public OAuth2ComplianceResult validateCompliance(UserDetails userDetails, String userType) {
        OAuth2ComplianceResult result = new OAuth2ComplianceResult();
        result.setUsername(userDetails.getUsername());
        result.setUserType(userType);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        result.setAuthorities(authorityStrings);

        validateBaseScopes(result, authorityStrings);
        validateRoleFormat(result, authorityStrings);
        validateScopeFormat(result, authorityStrings);
        validateUserTypeSpecificPermissions(result, authorityStrings, userType);
        validatePermissionInheritance(result, authorityStrings, userType);

        return result;
    }

    private void validateBaseScopes(OAuth2ComplianceResult result, Set<String> authorities) {
        for (String requiredScope : REQUIRED_BASE_SCOPES) {
            if (!authorities.contains(requiredScope)) {
                result.addWarning("Missing required base scope: " + requiredScope);
            }
        }
    }

    private void validateRoleFormat(OAuth2ComplianceResult result, Set<String> authorities) {
        List<String> roles = authorities.stream()
                .filter(auth -> auth.startsWith(ROLE_PREFIX))
                .toList();
        if (roles.isEmpty()) {
            result.addWarning("No role authority found with ROLE_ prefix");
        }
        if (!authorities.contains("ROLE_USER")) {
            result.addWarning("Missing base role ROLE_USER");
        }
    }

    private void validateScopeFormat(OAuth2ComplianceResult result, Set<String> authorities) {
        List<String> scopes = authorities.stream()
                .filter(auth -> auth.startsWith(SCOPE_PREFIX))
                .toList();
        if (scopes.isEmpty()) {
            result.addError("No OAuth2 scope authority found with SCOPE_ prefix");
        }
        result.setScopeCount(scopes.size());
    }

    private void validateUserTypeSpecificPermissions(OAuth2ComplianceResult result,
                                                     Set<String> authorities,
                                                     String userType) {
        String normalizedType = userType != null ? userType.toUpperCase() : "USER";
        switch (normalizedType) {
            case "ADMIN" -> {
                if (!authorities.contains("ROLE_ADMIN")) {
                    result.addWarning("Admin user missing ROLE_ADMIN");
                }
                if (!authorities.contains("SCOPE_admin.read") || !authorities.contains("SCOPE_admin.write")) {
                    result.addWarning("Admin user missing admin scopes");
                }
            }
            case "MERCHANT" -> {
                if (!authorities.contains("ROLE_MERCHANT")) {
                    result.addWarning("Merchant user missing ROLE_MERCHANT");
                }
                if (!authorities.contains("SCOPE_merchant.read") || !authorities.contains("SCOPE_merchant.write")) {
                    result.addWarning("Merchant user missing merchant scopes");
                }
            }
            default -> {
                if (!authorities.contains("SCOPE_user.read")) {
                    result.addWarning("User missing SCOPE_user.read");
                }
            }
        }
    }

    private void validatePermissionInheritance(OAuth2ComplianceResult result,
                                               Set<String> authorities,
                                               String userType) {
        String normalizedType = userType != null ? userType.toUpperCase() : "USER";
        if ("ADMIN".equals(normalizedType)) {
            if (!authorities.contains("SCOPE_order.read")) {
                result.addInfo("Admin should include SCOPE_order.read for support operations");
            }
        } else if ("MERCHANT".equals(normalizedType)) {
            if (!authorities.contains("SCOPE_order.read")) {
                result.addInfo("Merchant should include SCOPE_order.read");
            }
        }
    }

    public static class OAuth2ComplianceResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();
        private String username;
        private String userType;
        private Set<String> authorities;
        private int scopeCount;

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
    }
}
