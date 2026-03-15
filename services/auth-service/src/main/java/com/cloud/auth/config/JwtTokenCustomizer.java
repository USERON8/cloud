package com.cloud.auth.config;

import com.cloud.auth.service.support.AuthIdentityService;
import com.cloud.common.domain.dto.auth.AuthPrincipalDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final AuthIdentityService authIdentityService;

    @Override
    public void customize(JwtEncodingContext context) {
        if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            return;
        }

        List<String> audiences = resolveAudiences(context);
        Set<String> roles = new LinkedHashSet<>();
        Set<String> permissions = new LinkedHashSet<>();

        AuthPrincipalDTO principal = resolvePrincipal(context);
        if (principal != null) {
            roles.addAll(normalizeRoleClaims(principal.getRoles()));
            permissions.addAll(normalizePermissionClaims(principal.getPermissions()));
        } else {
            roles.addAll(extractRoles(context));
        }

        context.getClaims()
                .claim("client_id", context.getRegisteredClient().getClientId())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("aud", audiences);

        if (principal != null) {
            if (StrUtil.isNotBlank(principal.getUsername())) {
                context.getClaims().claim("username", principal.getUsername());
            }
            if (principal.getId() != null) {
                context.getClaims()
                        .claim("user_id", principal.getId())
                        .claim("userId", principal.getId());
            }
            if (principal.getStatus() != null) {
                context.getClaims().claim("status", principal.getStatus());
            }
            if (principal.getEnabled() != null) {
                context.getClaims().claim("enabled", principal.getEnabled());
            }
            if (StrUtil.isNotBlank(principal.getNickname())) {
                context.getClaims().claim("nickname", principal.getNickname());
            }
        }
    }

    private List<String> resolveAudiences(JwtEncodingContext context) {
        String clientId = context.getRegisteredClient().getClientId();
        if ("client-service".equals(clientId)) {
            return List.of("internal-api");
        }
        if ("service-client".equals(clientId)) {
            return List.of("service-api");
        }
        return List.of("gateway");
    }

    private Set<String> extractRoles(JwtEncodingContext context) {
        Set<String> roles = new LinkedHashSet<>();
        if (context.getPrincipal() != null && context.getPrincipal().getAuthorities() != null) {
            roles.addAll(context.getPrincipal().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            roles.add("ROLE_SERVICE");
        }
        return roles;
    }

    private AuthPrincipalDTO resolvePrincipal(JwtEncodingContext context) {
        String username = null;
        Object principalObj = context.getPrincipal() == null ? null : context.getPrincipal().getPrincipal();
        if (principalObj instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (context.getPrincipal() != null && StrUtil.isNotBlank(context.getPrincipal().getName())) {
            username = context.getPrincipal().getName();
        }
        if (StrUtil.isBlank(username)) {
            return null;
        }
        try {
            return authIdentityService.findByUsername(username);
        } catch (Exception ex) {
            log.warn("load user info for jwt claims failed, username={}", username, ex);
            return null;
        }
    }

    private Set<String> normalizeRoleClaims(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizePermissionClaims(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        return permissions.stream()
                .filter(permission -> permission != null && !permission.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

