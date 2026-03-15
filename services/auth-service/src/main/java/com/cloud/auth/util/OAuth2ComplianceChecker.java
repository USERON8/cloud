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

    public OAuth2ComplianceResult validateCompliance(UserDetails userDetails, List<String> roles) {
        OAuth2ComplianceResult result = new OAuth2ComplianceResult();
        result.setUsername(userDetails.getUsername());
        result.setRoles(roles == null ? List.of() : roles);

        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        Set<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        result.setAuthorities(authorityStrings);

        validateBaseScopes(result, authorityStrings);
        validateRoleFormat(result, authorityStrings);
        validateScopeFormat(result, authorityStrings);
        validateRoleSpecificPermissions(result, authorityStrings, result.getRoles());
        validatePermissionInheritance(result, authorityStrings, result.getRoles());

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

    private void validateRoleSpecificPermissions(OAuth2ComplianceResult result,
                                                 Set<String> authorities,
                                                 List<String> roles) {
        Set<String> normalizedRoles = roles.stream()
                .map(String::toUpperCase)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
                .collect(Collectors.toSet());
        if (normalizedRoles.contains("ADMIN")) {
                if (!authorities.contains("ROLE_ADMIN")) {
                    result.addWarning("Admin user missing ROLE_ADMIN");
                }
                if (!hasPermission(authorities, "admin:all")) {
                    result.addWarning("Admin user missing admin:all permission");
                }
        } else if (normalizedRoles.contains("MERCHANT")) {
                if (!authorities.contains("ROLE_MERCHANT")) {
                    result.addWarning("Merchant user missing ROLE_MERCHANT");
                }
                if (!hasPermission(authorities, "merchant:manage")) {
                    result.addWarning("Merchant user missing merchant:manage permission");
                }
        } else if (!hasPermission(authorities, "user:profile") && !hasPermission(authorities, "user:address")) {
            result.addWarning("User missing user permissions");
        }
    }

    private void validatePermissionInheritance(OAuth2ComplianceResult result,
                                               Set<String> authorities,
                                               List<String> roles) {
        Set<String> normalizedRoles = roles.stream()
                .map(String::toUpperCase)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
                .collect(Collectors.toSet());
        if (normalizedRoles.contains("ADMIN")) {
            if (!hasPermission(authorities, "order:query")) {
                result.addInfo("Admin should include order:query for support operations");
            }
        } else if (normalizedRoles.contains("MERCHANT")) {
            if (!hasPermission(authorities, "order:query")) {
                result.addInfo("Merchant should include order:query");
            }
        }
    }

    private boolean hasPermission(Set<String> authorities, String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        return authorities.contains(permission) || authorities.contains(SCOPE_PREFIX + permission);
    }

    public static class OAuth2ComplianceResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();
        private String username;
        private List<String> roles = List.of();
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

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? List.of() : roles;
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
