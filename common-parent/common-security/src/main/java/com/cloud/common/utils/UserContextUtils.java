package com.cloud.common.utils;

import cn.hutool.core.util.StrUtil;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class UserContextUtils {

  public static String getCurrentUserId() {
    String userId = getClaimFromJwt("user_id");
    if (StrUtil.isBlank(userId)) {
      userId = getClaimFromJwt("userId");
    }
    return userId;
  }

  public static String getCurrentUsername() {

    String usernameFromJwt = getClaimFromJwt("username");
    if (StrUtil.isNotBlank(usernameFromJwt)) {
      return usernameFromJwt;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && StrUtil.isNotBlank(authentication.getName())) {
      return authentication.getName();
    }

    return null;
  }

  public static String getCurrentPrimaryRole() {
    if (hasRole("ADMIN")) {
      return "ADMIN";
    }
    if (hasRole("MERCHANT")) {
      return "MERCHANT";
    }
    if (hasRole("USER")) {
      return "USER";
    }
    return null;
  }

  public static String getCurrentUserNickname() {
    return getClaimFromJwt("nickname");
  }

  public static String getCurrentUserStatus() {
    return getClaimFromJwt("status");
  }

  public static Set<String> getCurrentUserScopes() {
    String scopesFromJwt = getClaimFromJwt("scope");
    if (StrUtil.isNotBlank(scopesFromJwt)) {
      return Stream.of(scopesFromJwt.split("\\s+"))
          .filter(StrUtil::isNotBlank)
          .map(UserContextUtils::normalizeScope)
          .collect(Collectors.toSet());
    }

    return Collections.emptySet();
  }

  public static boolean hasScope(String scope) {
    if (StrUtil.isBlank(scope)) {
      return false;
    }
    Set<String> userScopes = getCurrentUserScopes();
    return userScopes.contains(normalizeScope(scope));
  }

  private static String normalizeScope(String scope) {
    return scope.replace('.', ':');
  }

  public static Jwt getCurrentJwt() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken();
    }
    return null;
  }

  public static String getClaimFromJwt(String claimName) {
    Jwt jwt = getCurrentJwt();
    if (jwt != null) {
      Object claim = jwt.getClaim(claimName);
      return claim != null ? claim.toString() : null;
    }
    return null;
  }

  public static boolean isMerchant() {
    return hasRole("MERCHANT");
  }

  public static boolean isAdmin() {
    return hasRole("ADMIN");
  }

  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication instanceof JwtAuthenticationToken;
  }

  public static boolean hasRole(String role) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || role == null || role.isBlank()) {
      return false;
    }
    String expected = role.startsWith("ROLE_") ? role : "ROLE_" + role;
    return authentication.getAuthorities().stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .anyMatch(expected::equals);
  }
}
