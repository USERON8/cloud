package com.cloud.auth.service;

import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.auth.util.OAuth2ComplianceChecker;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import com.cloud.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private LocalUserAuthorityService localUserAuthorityService;

    @Mock
    private AuthUserAuthorityCacheService authorityCacheService;

    @Mock
    private AuthIdentityService authIdentityService;

    @InjectMocks
    private CustomUserDetailsServiceImpl customUserDetailsService;

    @Test
    void loadUserByUsername_blankUsername_throwsValidationException() {
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("  "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loadUserByUsername_userMissing_wrapsUsernameNotFound() {
        when(authIdentityService.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void loadUserByUsername_userDisabled_wrapsUsernameNotFound() {
        AuthPrincipalDTO principal = new AuthPrincipalDTO();
        principal.setId(12L);
        principal.setUsername("disabled");
        principal.setPassword("pwd");
        principal.setStatus(0);
        when(authIdentityService.findByUsername("disabled")).thenReturn(principal);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("disabled"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void loadUserByUsername_success_executesComplianceCheck() {
        AuthPrincipalDTO principal = new AuthPrincipalDTO();
        principal.setId(1L);
        principal.setUsername("alice");
        principal.setPassword("secret");
        principal.setStatus(1);
        principal.setRoles(List.of("ROLE_ADMIN"));
        when(authIdentityService.findByUsername("alice")).thenReturn(principal);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("SCOPE_openid")
        );
        when(authorityCacheService.loadAuthorities(1L)).thenReturn(List.of());
        when(localUserAuthorityService.buildAuthorities(List.of("ROLE_ADMIN"), null)).thenReturn(authorities);

        OAuth2ComplianceChecker complianceChecker = mock(OAuth2ComplianceChecker.class);
        ReflectionTestUtils.setField(customUserDetailsService, "complianceChecker", complianceChecker);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "SCOPE_openid");
        verify(authorityCacheService).cacheAuthorities(1L, authorities);
        verify(complianceChecker).validateCompliance(eq(userDetails), eq(List.of("ROLE_ADMIN")));
    }
}
