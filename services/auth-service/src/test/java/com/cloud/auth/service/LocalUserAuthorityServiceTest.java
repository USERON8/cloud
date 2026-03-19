package com.cloud.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloud.common.config.PermissionConfig;
import com.cloud.common.domain.dto.user.UserDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class LocalUserAuthorityServiceTest {

  private PermissionConfig permissionConfig;

  private LocalUserAuthorityService localUserAuthorityService;

  @BeforeEach
  void setUp() {
    permissionConfig = new PermissionConfig();
    permissionConfig.setEnabled(true);
    permissionConfig.setStrictMode(false);
    permissionConfig.setDefaultPermissions(List.of("user:read"));
    localUserAuthorityService = new LocalUserAuthorityService(permissionConfig);
  }

  @Test
  void buildAuthorities_expandsRoleAndPermissions() {
    var authorities =
        localUserAuthorityService.buildAuthorities(
            List.of("ADMIN"), List.of("order:read", "order:write"));

    assertThat(authorities)
        .extracting(auth -> auth.getAuthority())
        .contains(
            "ROLE_ADMIN",
            "SCOPE_openid",
            "SCOPE_profile",
            "SCOPE_read",
            "order:read",
            "order:write",
            "SCOPE_order:read",
            "SCOPE_order:write");
  }

  @Test
  void buildAuthorities_appliesDefaultPermissionsWhenMissing() {
    var authorities = localUserAuthorityService.buildAuthorities(List.of());

    assertThat(authorities)
        .extracting(auth -> auth.getAuthority())
        .contains(
            "ROLE_USER",
            "user:read",
            "SCOPE_user:read",
            "SCOPE_openid",
            "SCOPE_profile",
            "SCOPE_read");
  }

  @Test
  void createAuthenticatedPrincipal_disabledUserThrows() {
    UserDTO userDTO = new UserDTO();
    userDTO.setUsername("blocked");
    userDTO.setStatus(0);

    assertThatThrownBy(() -> localUserAuthorityService.createAuthenticatedPrincipal(userDTO))
        .isInstanceOf(BizException.class)
        .hasMessageContaining(ResultCode.USER_DISABLED.getMessage());
  }

  @Test
  void createAuthenticatedPrincipal_returnsAuthentication() {
    UserDTO userDTO = new UserDTO();
    userDTO.setUsername("alice");
    userDTO.setStatus(1);
    userDTO.setRoles(List.of("USER"));

    Authentication authentication = localUserAuthorityService.createAuthenticatedPrincipal(userDTO);

    assertThat(authentication.getName()).isEqualTo("alice");
    assertThat(authentication.getAuthorities())
        .extracting(auth -> auth.getAuthority())
        .contains("ROLE_USER", "user:read", "SCOPE_user:read");
  }
}
