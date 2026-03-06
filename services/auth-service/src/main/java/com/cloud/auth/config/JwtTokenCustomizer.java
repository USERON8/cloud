package com.cloud.auth.config;

import com.cloud.auth.module.entity.AuthUser;
import com.cloud.auth.service.support.AuthIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        Set<String> roles = extractRoles(context);

        context.getClaims()
                .claim("client_id", context.getRegisteredClient().getClientId())
                .claim("token_version", "v2")
                .claim("roles", roles)
                .claim("aud", audiences);

        Object principal = context.getPrincipal().getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();
            context.getClaims().claim("username", username);

            try {
                AuthUser user = authIdentityService.findByUsername(username);
                if (user != null) {
                    context.getClaims()
                            .claim("user_id", user.getId())
                            .claim("status", user.getStatus());
                }
            } catch (Exception ex) {
                log.warn("load user info for jwt claims failed, username={}", username, ex);
            }
        } else {
            String principalName = context.getPrincipal().getName();
            if (principalName != null && !principalName.isBlank()) {
                context.getClaims().claim("username", principalName);
                try {
                    AuthUser user = authIdentityService.findByUsername(principalName);
                    if (user != null) {
                        context.getClaims()
                                .claim("user_id", user.getId())
                                .claim("status", user.getStatus());
                    }
                } catch (Exception ex) {
                    log.warn("load user info for jwt claims failed, username={}", principalName, ex);
                }
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
                    .map(authority -> authority.substring("ROLE_".length()))
                    .filter(role -> !role.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
            roles.add("SERVICE");
        }
        return roles;
    }
}

