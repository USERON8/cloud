package com.cloud.common.security;

import com.cloud.common.exception.BizException;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityPermissionUtils {

  private SecurityPermissionUtils() {}

  private static Authentication getCurrentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  public static String getCurrentUserId() {
    return getCurrentUserId(getCurrentAuthentication());
  }

  public static String getCurrentUserId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String userId = jwt.getClaimAsString("user_id");
      if (userId == null || userId.isBlank()) {
        userId = jwt.getClaimAsString("userId");
      }
      return userId;
    }
    Object principal = authentication == null ? null : authentication.getPrincipal();
    if (principal instanceof InternalAuthenticatedPrincipal internalPrincipal) {
      return internalPrincipal.userId();
    }
    return null;
  }

  public static Long requireCurrentUserIdAsLong(Authentication authentication) {
    String userId = getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BizException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BizException("invalid user_id in token");
    }
  }

  public static boolean hasAuthority(Authentication authentication, String authority) {
    if (authentication == null) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
  }

  public static boolean hasRole(Authentication authentication, String role) {
    String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    return hasAuthority(authentication, roleAuthority);
  }

  public static boolean isAdmin() {
    return isAdmin(getCurrentAuthentication());
  }

  public static boolean isAdmin(Authentication authentication) {
    return hasRole(authentication, "ADMIN");
  }

  public static boolean isMerchant() {
    return isMerchant(getCurrentAuthentication());
  }

  public static boolean isMerchant(Authentication authentication) {
    return hasRole(authentication, "MERCHANT");
  }

  public static boolean isOwner(Authentication authentication, Long resourceUserId) {
    if (authentication == null || resourceUserId == null) {
      return false;
    }

    String currentUserId = getCurrentUserId(authentication);
    return Objects.equals(currentUserId, resourceUserId.toString());
  }

  public static boolean isMerchantOwner(Long merchantId) {
    return isMerchantOwner(getCurrentAuthentication(), merchantId);
  }

  public static boolean isMerchantOwner(Authentication authentication, Long merchantId) {
    return false;
  }

  public static boolean isAdminOrOwner(Authentication authentication, Long resourceUserId) {
    return isAdmin(authentication) || isOwner(authentication, resourceUserId);
  }

  public static boolean isAdminOrMerchantOwner(Long merchantId) {
    return isAdminOrMerchantOwner(getCurrentAuthentication(), merchantId);
  }

  public static boolean isAdminOrMerchantOwner(Authentication authentication, Long merchantId) {
    return isAdmin(authentication);
  }
}
