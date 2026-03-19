package com.cloud.auth.service;

import com.cloud.common.config.PermissionConfig;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalUserAuthorityService {

  private final PermissionConfig permissionConfig;

  public List<SimpleGrantedAuthority> buildAuthorities(Collection<String> roles) {
    return buildAuthorities(roles, null);
  }

  public List<SimpleGrantedAuthority> buildAuthorities(
      Collection<String> roles, Collection<String> permissions) {
    Set<String> normalizedRoles = normalizeRoles(roles);
    if (normalizedRoles.isEmpty()) {
      normalizedRoles.add("USER");
    }

    Set<String> expandedRoleAuthorities =
        normalizedRoles.stream()
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .flatMap(role -> expandRoleAuthorities(role).stream())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    List<String> resolvedPermissions = resolvePermissions(expandedRoleAuthorities, permissions);

    Set<String> authorityNames = new LinkedHashSet<>();
    authorityNames.add("SCOPE_openid");
    authorityNames.add("SCOPE_profile");
    authorityNames.add("SCOPE_read");

    for (String role : normalizedRoles) {
      String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
      for (String expandedRole : expandRoleAuthorities(roleAuthority)) {
        authorityNames.add(expandedRole);
      }
    }

    for (String permission : resolvedPermissions) {
      authorityNames.add(permission);
      authorityNames.add("SCOPE_" + permission);
    }

    return authorityNames.stream().map(SimpleGrantedAuthority::new).toList();
  }

  public Authentication createAuthenticatedPrincipal(UserDTO userDTO) {
    if (userDTO == null) {
      throw new IllegalArgumentException("User DTO cannot be null");
    }
    if (userDTO.getStatus() != null && userDTO.getStatus() != 1) {
      throw new BizException(ResultCode.USER_DISABLED);
    }

    List<SimpleGrantedAuthority> authorities = buildAuthorities(userDTO.getRoles());
    UserDetails userDetails =
        User.builder()
            .username(userDTO.getUsername())
            .password("[SOCIAL_LOGIN]")
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();

    return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, authorities);
  }

  private Set<String> normalizeRoles(Collection<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return new LinkedHashSet<>();
    }
    return roles.stream()
        .filter(role -> role != null && !role.isBlank())
        .map(String::trim)
        .map(String::toUpperCase)
        .map(role -> role.startsWith("ROLE_") ? role.substring("ROLE_".length()) : role)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }

  private Set<String> expandRoleAuthorities(String roleAuthority) {
    Set<String> roles = new LinkedHashSet<>();
    roles.add(roleAuthority);
    return roles;
  }

  private List<String> resolvePermissions(
      Set<String> roleAuthorities, Collection<String> permissions) {
    if (permissions != null && !permissions.isEmpty()) {
      List<String> normalized =
          permissions.stream()
              .filter(value -> value != null && !value.isBlank())
              .map(String::trim)
              .toList();
      return permissionConfig.resolvePermissions("EXPLICIT", normalized);
    }

    if (roleAuthorities == null || roleAuthorities.isEmpty()) {
      return new ArrayList<>();
    }

    Set<String> resolved = new LinkedHashSet<>();
    for (String roleAuthority : roleAuthorities) {
      resolved.addAll(permissionConfig.resolvePermissions(roleAuthority, null));
    }
    return new ArrayList<>(resolved);
  }
}
