package com.cloud.auth.config;

import com.cloud.auth.service.CustomUserDetailsServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuthenticationProviderConfig {

    private final CustomUserDetailsServiceImpl customUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Component
    @Slf4j
    public static class UserDetailsServiceValidator {
        private final CustomUserDetailsServiceImpl userDetailsService;

        public UserDetailsServiceValidator(CustomUserDetailsServiceImpl userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        @PostConstruct
        private void validateConfiguration() {
            try {
                if (userDetailsService == null) {
                    throw new IllegalStateException("CustomUserDetailsService is not initialized");
                }
            } catch (Exception e) {
                log.error("User details service validation failed", e);
                throw new IllegalStateException("User details service validation failed", e);
            }
        }

        public String getServiceInfo() {
            return String.format("UserDetailsService: %s, status: OK", userDetailsService.getClass().getSimpleName());
        }
    }

    @Component
    @Slf4j
    public static class AuthenticationEventListener {

        @org.springframework.context.event.EventListener
        public void handleAuthenticationSuccess(
                org.springframework.security.authentication.event.AuthenticationSuccessEvent event) {
            String username = event.getAuthentication().getName();
            String authorities = event.getAuthentication().getAuthorities().toString();
            log.debug("Authentication success: username={}, authorities={}", username, authorities);
        }

        @org.springframework.context.event.EventListener
        public void handleAuthenticationFailure(
                org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent event) {
            String username = event.getAuthentication().getName();
            String exceptionMessage = event.getException().getMessage();
            log.warn("Authentication failure: username={}, reason={}", username, exceptionMessage);
        }

        @org.springframework.context.event.EventListener
        public void handleBadCredentials(
                org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent event) {
            String username = event.getAuthentication().getName();
            log.warn("Bad credentials: username={}", username);
        }

        @org.springframework.context.event.EventListener
        public void handleCredentialsExpired(
                org.springframework.security.authentication.event.AuthenticationFailureCredentialsExpiredEvent event) {
            String username = event.getAuthentication().getName();
            log.warn("Credentials expired: username={}", username);
        }
    }
}
