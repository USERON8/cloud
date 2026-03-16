package com.cloud.common.config;

import com.cloud.common.exception.PermissionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PermissionChecker {

  public boolean checkRole(String requiredRole, String currentRole) {
    if (requiredRole == null || currentRole == null) {
      return false;
    }
    return requiredRole.equalsIgnoreCase(currentRole);
  }

  public boolean checkAdminPermission(String currentRole) {
    return "ADMIN".equalsIgnoreCase(currentRole);
  }

  public boolean checkMerchantPermission(String currentRole) {
    return "MERCHANT".equalsIgnoreCase(currentRole) || "ADMIN".equalsIgnoreCase(currentRole);
  }

  public boolean checkUserPermission(String currentRole) {
    return "USER".equalsIgnoreCase(currentRole);
  }

  public void assertRole(String requiredRole) {
    String currentRole = getCurrentRole();
    if (!checkRole(requiredRole, currentRole)) {
      throw new PermissionException(
          "ACCESS_DENIED",
          "required role "
              + requiredRole
              + " but current role is "
              + (currentRole != null ? currentRole : "UNKNOWN"));
    }
  }

  private String getCurrentRole() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(authority -> authority.startsWith("ROLE_"))
        .map(authority -> authority.substring("ROLE_".length()))
        .findFirst()
        .orElse(null);
  }
}
