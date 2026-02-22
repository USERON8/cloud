package com.cloud.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enable method-level security for @PreAuthorize checks in auth controllers.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
