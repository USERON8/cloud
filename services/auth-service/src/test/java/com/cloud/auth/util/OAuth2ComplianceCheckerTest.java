package com.cloud.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class OAuth2ComplianceCheckerTest {

  private final OAuth2ComplianceChecker complianceChecker = new OAuth2ComplianceChecker();

  @Test
  void validateCompliance_missingScopes_reportsErrors() {
    UserDetails userDetails =
        User.builder()
            .username("alice")
            .password("pwd")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();

    OAuth2ComplianceChecker.OAuth2ComplianceResult result =
        complianceChecker.validateCompliance(userDetails, List.of("USER"));

    assertThat(result.getErrors()).anyMatch(msg -> msg.contains("No OAuth2 scope authority"));
    assertThat(result.getWarnings()).anyMatch(msg -> msg.contains("Missing required base scope"));
  }

  @Test
  void validateCompliance_adminMissingAdminPermission_warns() {
    UserDetails userDetails =
        User.builder()
            .username("admin")
            .password("pwd")
            .authorities(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("SCOPE_openid"),
                new SimpleGrantedAuthority("SCOPE_profile"),
                new SimpleGrantedAuthority("SCOPE_read"))
            .build();

    OAuth2ComplianceChecker.OAuth2ComplianceResult result =
        complianceChecker.validateCompliance(userDetails, List.of("ADMIN"));

    assertThat(result.getWarnings()).anyMatch(msg -> msg.contains("admin:all"));
  }
}
