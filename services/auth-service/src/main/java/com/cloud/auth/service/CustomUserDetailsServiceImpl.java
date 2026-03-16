package com.cloud.auth.service;

import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BusinessException;
import com.cloud.common.exception.ResourceNotFoundException;
import com.cloud.common.exception.ValidationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {

  private final LocalUserAuthorityService localUserAuthorityService;
  private final AuthUserAuthorityCacheService authorityCacheService;
  private final AuthIdentityService authIdentityService;

  @Autowired(required = false)
  private OAuth2ComplianceChecker complianceChecker;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (username == null || username.trim().isEmpty()) {
      throw new ValidationException("username", username, "username cannot be blank");
    }

    try {
      AuthPrincipalDTO principal = authIdentityService.findByUsername(username.trim());
      if (principal == null) {
        throw new ResourceNotFoundException("User", username);
      }
      if (isDisabled(principal)) {
        throw new BusinessException(ResultCode.USER_DISABLED);
      }

      List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
          resolveAuthorities(principal);

      UserDetails userDetails =
          User.builder()
              .username(principal.getUsername())
              .password(principal.getPassword())
              .authorities(authorities)
              .accountExpired(false)
              .accountLocked(false)
              .credentialsExpired(false)
              .disabled(false)
              .build();

      if (complianceChecker != null) {
        try {
          complianceChecker.validateCompliance(userDetails, principal.getRoles());
        } catch (Exception e) {
          log.debug("OAuth2 compliance check skipped: {}", e.getMessage());
        }
      }

      return userDetails;
    } catch (UsernameNotFoundException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("Failed to load user details for {}", username, ex);
      throw new UsernameNotFoundException("Failed to load user details for " + username, ex);
    }
  }

  private boolean isDisabled(AuthPrincipalDTO principal) {
    if (principal.getEnabled() != null) {
      return principal.getEnabled() != 1;
    }
    return principal.getStatus() != null && principal.getStatus() != 1;
  }

  private List<org.springframework.security.core.authority.SimpleGrantedAuthority>
      resolveAuthorities(AuthPrincipalDTO principal) {
    Long userId = principal == null ? null : principal.getId();
    if (userId != null) {
      List<org.springframework.security.core.authority.SimpleGrantedAuthority> cached =
          authorityCacheService.loadAuthorities(userId);
      if (cached != null && !cached.isEmpty()) {
        return cached;
      }
    }

    List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
        localUserAuthorityService.buildAuthorities(
            principal.getRoles(), principal.getPermissions());

    if (userId != null) {
      authorityCacheService.cacheAuthorities(userId, authorities);
    }
    return authorities;
  }
}
