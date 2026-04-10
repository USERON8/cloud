package com.cloud.common.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class JwtAuthorityUtils {

  private JwtAuthorityUtils() {}

  public static JwtAuthenticationConverter buildJwtAuthenticationConverter(
      boolean lowerCaseScope,
      boolean includeAuthoritiesClaim,
      BiConsumer<Set<GrantedAuthority>, Jwt> customizer) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        buildAuthoritiesConverter(lowerCaseScope, includeAuthoritiesClaim, customizer));
    return converter;
  }

  private static Converter<Jwt, Collection<GrantedAuthority>> buildAuthoritiesConverter(
      boolean lowerCaseScope,
      boolean includeAuthoritiesClaim,
      BiConsumer<Set<GrantedAuthority>, Jwt> customizer) {
    return jwt -> {
      Set<GrantedAuthority> authorities = new LinkedHashSet<>();

      authorities.addAll(extractScopeAuthorities(jwt.getClaim("scope"), lowerCaseScope));
      authorities.addAll(extractScopeAuthorities(jwt.getClaim("scp"), lowerCaseScope));
      authorities.addAll(extractRoleAuthorities(jwt.getClaim("roles")));
      authorities.addAll(extractPermissionAuthorities(jwt.getClaim("permissions"), lowerCaseScope));

      if (includeAuthoritiesClaim) {
        authorities.addAll(extractRawAuthorities(jwt.getClaim("authorities")));
      }

      if (customizer != null) {
        customizer.accept(authorities, jwt);
      }

      return authorities;
    };
  }

  private static Set<GrantedAuthority> extractScopeAuthorities(
      Object scopeClaim, boolean lowerCaseScope) {
    Set<String> normalizedScopes = new LinkedHashSet<>();
    if (scopeClaim == null) {
      return Set.of();
    }
    if (scopeClaim instanceof String scopeString) {
      normalizedScopes.addAll(
          Arrays.stream(scopeString.trim().split("\\s+"))
              .map(scope -> normalizeScope(scope, lowerCaseScope))
              .filter(scope -> !scope.isBlank())
              .collect(Collectors.toSet()));
    } else if (scopeClaim instanceof Collection<?> scopeCollection) {
      normalizedScopes.addAll(
          scopeCollection.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .map(scope -> normalizeScope(scope, lowerCaseScope))
              .filter(scope -> !scope.isBlank())
              .collect(Collectors.toSet()));
    }

    return normalizedScopes.stream()
        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<GrantedAuthority> extractRawAuthorities(Object authoritiesClaim) {
    Set<String> rawAuthorities = new LinkedHashSet<>();
    if (authoritiesClaim == null) {
      return Set.of();
    }
    if (authoritiesClaim instanceof String authorityString) {
      rawAuthorities.addAll(
          Arrays.stream(authorityString.trim().split("\\s+"))
              .filter(authority -> !authority.isBlank())
              .collect(Collectors.toSet()));
    } else if (authoritiesClaim instanceof Collection<?> authorityCollection) {
      rawAuthorities.addAll(
          authorityCollection.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .map(String::trim)
              .filter(authority -> !authority.isBlank())
              .collect(Collectors.toSet()));
    }

    return rawAuthorities.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<GrantedAuthority> extractRoleAuthorities(Object rolesClaim) {
    Set<String> roles = new LinkedHashSet<>();
    if (rolesClaim == null) {
      return Set.of();
    }
    if (rolesClaim instanceof String roleString) {
      roles.addAll(
          Arrays.stream(roleString.trim().split("\\s+"))
              .map(String::trim)
              .filter(role -> !role.isBlank())
              .collect(Collectors.toSet()));
    } else if (rolesClaim instanceof Collection<?> roleCollection) {
      roles.addAll(
          roleCollection.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .map(String::trim)
              .filter(role -> !role.isBlank())
              .collect(Collectors.toSet()));
    }
    return roles.stream()
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT))
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private static Set<GrantedAuthority> extractPermissionAuthorities(
      Object permissionsClaim, boolean lowerCaseScope) {
    Set<String> permissions = new LinkedHashSet<>();
    if (permissionsClaim == null) {
      return Set.of();
    }
    if (permissionsClaim instanceof String permissionString) {
      permissions.addAll(
          Arrays.stream(permissionString.trim().split("\\s+|,"))
              .map(String::trim)
              .filter(permission -> !permission.isBlank())
              .collect(Collectors.toSet()));
    } else if (permissionsClaim instanceof Collection<?> permissionCollection) {
      permissions.addAll(
          permissionCollection.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .map(String::trim)
              .filter(permission -> !permission.isBlank())
              .collect(Collectors.toSet()));
    }

    Set<GrantedAuthority> authorities = new LinkedHashSet<>();
    for (String permission : permissions) {
      String normalized = normalizeScope(permission, lowerCaseScope);
      if (normalized.isBlank()) {
        continue;
      }
      String raw =
          normalized.startsWith("SCOPE_") ? normalized.substring("SCOPE_".length()) : normalized;
      if (raw.isBlank()) {
        continue;
      }
      authorities.add(new SimpleGrantedAuthority(raw));
      authorities.add(new SimpleGrantedAuthority("SCOPE_" + raw));
    }
    return authorities;
  }

  private static String normalizeScope(String scope, boolean lowerCaseScope) {
    if (scope == null) {
      return "";
    }
    String normalized = scope.trim().replace('.', ':');
    return lowerCaseScope ? normalized.toLowerCase(Locale.ROOT) : normalized;
  }
}
