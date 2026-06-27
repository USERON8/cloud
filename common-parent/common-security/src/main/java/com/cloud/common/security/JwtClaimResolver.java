package com.cloud.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtClaimResolver {

  private JwtClaimResolver() {}

  public static Long resolveUserId(Jwt jwt) {
    String userId = resolveUserIdString(jwt);
    if (userId == null) {
      return null;
    }
    try {
      return Long.valueOf(userId);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  public static String resolveUserIdString(Jwt jwt) {
    String userId = claimValue(jwt, "user_id");
    if (userId != null) {
      return userId;
    }
    return claimValue(jwt, "userId");
  }

  public static String resolveUserIdOrSubject(Jwt jwt) {
    String userId = resolveUserIdString(jwt);
    if (userId != null) {
      return userId;
    }
    if (jwt == null || jwt.getSubject() == null) {
      return null;
    }
    String subject = jwt.getSubject().trim();
    return subject.isEmpty() ? null : subject;
  }

  private static String claimValue(Jwt jwt, String name) {
    if (jwt == null || name == null) {
      return null;
    }
    Object claim = jwt.getClaim(name);
    if (claim == null) {
      return null;
    }
    String value = String.valueOf(claim).trim();
    return value.isEmpty() ? null : value;
  }
}
