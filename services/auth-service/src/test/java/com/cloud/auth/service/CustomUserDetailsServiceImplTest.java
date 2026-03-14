package com.cloud.auth.service;

import com.cloud.auth.module.entity.AuthUser;
import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.auth.util.OAuth2ComplianceChecker;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceImplTest {

    @Mock
    private LocalUserAuthorityService localUserAuthorityService;

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
        AuthUser authUser = new AuthUser();
        authUser.setId(12L);
        authUser.setUsername("disabled");
        authUser.setPassword("pwd");
        authUser.setStatus(0);
        when(authIdentityService.findByUsername("disabled")).thenReturn(authUser);

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("disabled"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void loadUserByUsername_success_executesComplianceCheck() {
        AuthUser authUser = new AuthUser();
        authUser.setId(1L);
        authUser.setUsername("alice");
        authUser.setPassword("secret");
        authUser.setStatus(1);
        when(authIdentityService.findByUsername("alice")).thenReturn(authUser);
        when(authIdentityService.getRoleCodes(1L)).thenReturn(List.of("ADMIN"));

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("SCOPE_openid")
        );
        when(localUserAuthorityService.buildAuthorities(List.of("ADMIN"))).thenReturn(authorities);

        OAuth2ComplianceChecker complianceChecker = mock(OAuth2ComplianceChecker.class);
        ReflectionTestUtils.setField(customUserDetailsService, "complianceChecker", complianceChecker);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "SCOPE_openid");
        verify(complianceChecker).validateCompliance(eq(userDetails), eq(List.of("ADMIN")));
        verify(authIdentityService, times(2)).getRoleCodes(1L);
    }
}
