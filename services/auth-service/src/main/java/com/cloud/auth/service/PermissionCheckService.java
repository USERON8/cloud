package com.cloud.auth.service;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.dto.user.UserProfileDTO;
import com.cloud.common.remote.RemoteCallSupport;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckService {

  private final RemoteCallSupport remoteCallSupport;

  @DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  public boolean hasRole(Authentication authentication, String role) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    return authorities.stream().anyMatch(authority -> authority.getAuthority().equals(role));
  }

  public boolean hasAuthority(Authentication authentication, String authority) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
    return authorities.stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
  }

  public boolean isAdmin(Authentication authentication) {
    return hasRole(authentication, "ROLE_ADMIN");
  }

  public boolean isMerchant(Authentication authentication) {
    return hasRole(authentication, "ROLE_MERCHANT");
  }

  public boolean isUser(Authentication authentication) {
    return hasRole(authentication, "ROLE_USER");
  }

  public boolean canAccessResource(Authentication authentication, String resource, String action) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }

    if (isAdmin(authentication)) {
      return true;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      Long userId = resolveUserId(jwt);

      if (isAdmin(authentication)) {
        return true;
      }
      if (isMerchant(authentication)) {
        return isMerchantResource(resource, userId);
      }
      if (isUser(authentication)) {
        return isUserResource(resource, userId);
      }
    }

    return false;
  }

  private boolean isMerchantResource(String resource, Long merchantId) {

    return true;
  }

  private boolean isUserResource(String resource, Long userId) {

    return true;
  }

  public Long getCurrentUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      return resolveUserId(jwt);
    }

    return null;
  }

  public String getCurrentPrimaryRole(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    if (isAdmin(authentication)) {
      return "ADMIN";
    }
    if (isMerchant(authentication)) {
      return "MERCHANT";
    }
    if (isUser(authentication)) {
      return "USER";
    }
    return null;
  }

  public UserProfileDTO getCurrentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      Long userId = resolveUserId(jwt);
      if (userId == null) {
        return null;
      }
      try {
        return remoteCallSupport.query(
            "user-service.findById", () -> userDubboApi.findById(userId));
      } catch (RuntimeException ex) {
        log.error("Failed to load current user profile, userId={}", userId, ex);
        throw ex;
      }
    }

    return null;
  }

  private Long resolveUserId(Jwt jwt) {
    Long userId = jwt.getClaim("user_id");
    if (userId != null) {
      return userId;
    }
    Object claim = jwt.getClaim("userId");
    if (claim == null) {
      return null;
    }
    try {
      return Long.valueOf(claim.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
