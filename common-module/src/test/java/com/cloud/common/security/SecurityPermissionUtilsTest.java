package com.cloud.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPermissionUtilsTest {

    @Test
    void isAdminShouldNotUseFuzzyAuthorityMatching() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "100")
                .claim("user_type", "USER")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                AuthorityUtils.createAuthorityList("SCOPE_domain:read")
        );

        assertFalse(SecurityPermissionUtils.isAdmin(authentication));
    }

    @Test
    void isMerchantShouldNotUseFuzzyAuthorityMatching() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "101")
                .claim("user_type", "USER")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                AuthorityUtils.createAuthorityList("SCOPE_supermerchant_audit")
        );

        assertFalse(SecurityPermissionUtils.isMerchant(authentication));
    }

    @Test
    void isAdminShouldSupportRoleOrScopeOrUserType() {
        Jwt jwtByType = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "1")
                .claim("user_type", "ADMIN")
                .build();
        JwtAuthenticationToken authByType = new JwtAuthenticationToken(
                jwtByType,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        assertTrue(SecurityPermissionUtils.isAdmin(authByType));

        Jwt jwtByScope = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", "2")
                .claim("user_type", "USER")
                .build();
        JwtAuthenticationToken authByScope = new JwtAuthenticationToken(
                jwtByScope,
                AuthorityUtils.createAuthorityList("SCOPE_admin:read")
        );
        assertTrue(SecurityPermissionUtils.isAdmin(authByScope));

        UsernamePasswordAuthenticationToken authByRole = new UsernamePasswordAuthenticationToken(
                "admin",
                "N/A",
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        );
        assertTrue(SecurityPermissionUtils.isAdmin(authByRole));
    }
}

