package com.cloud.common.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record InternalAuthenticatedPrincipal(
    String subject,
    String userId,
    String username,
    String clientId,
    Set<String> roles,
    Set<String> permissions,
    Set<String> scopes) {

  public InternalAuthenticatedPrincipal {
    roles = immutableCopy(roles);
    permissions = immutableCopy(permissions);
    scopes = immutableCopy(scopes);
  }

  private static Set<String> immutableCopy(Set<String> source) {
    if (source == null || source.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(new LinkedHashSet<>(source));
  }
}
