package com.cloud.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPasswordConfigTest {

    private final JwtPasswordConfig config = new JwtPasswordConfig();

    @Test
    void enhancedConverterShouldReadRolesAndUsernameClaims() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject")
                .claim("username", "admin-user")
                .claim("scope", "order.write internal")
                .claim("roles", List.of("ADMIN"))
                .build();

        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) config.enhancedJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("admin-user");
        assertThat(authentication.getAuthorities())
                .extracting(grantedAuthority -> grantedAuthority.getAuthority())
                .contains("ROLE_ADMIN", "SCOPE_order:write", "SCOPE_internal");
    }
}
