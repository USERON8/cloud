package com.cloud.user.service.support;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RoleCodeSupport {

  private static final String ROLE_PREFIX = "ROLE_";

  private RoleCodeSupport() {}

  public static Set<String> normalizeRoleCodes(Collection<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return Set.of();
    }
    return roles.stream()
        .map(RoleCodeSupport::normalizeRoleCode)
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static String normalizeRoleCode(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
      return null;
    }
    String trimmed = roleCode.trim().toUpperCase();
    return trimmed.startsWith(ROLE_PREFIX) ? trimmed : ROLE_PREFIX + trimmed;
  }

  public static String stripRolePrefix(String roleCode) {
    if (roleCode == null || roleCode.isBlank()) {
      return roleCode;
    }
    return roleCode.startsWith(ROLE_PREFIX) ? roleCode.substring(ROLE_PREFIX.length()) : roleCode;
  }
}
