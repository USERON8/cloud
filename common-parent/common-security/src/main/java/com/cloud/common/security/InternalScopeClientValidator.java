package com.cloud.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class InternalScopeClientValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> allowedClientIds;

    public InternalScopeClientValidator(Set<String> allowedClientIds) {
        this.allowedClientIds = allowedClientIds;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Set<String> scopes = extractScopes(jwt.getClaim("scope"));
        scopes.addAll(extractScopes(jwt.getClaim("scp")));
        if (!scopes.contains("internal_api")) {
            return OAuth2TokenValidatorResult.success();
        }

        String clientId = jwt.getClaimAsString("client_id");
        if (clientId == null || clientId.isBlank()) {
            return failure("invalid_client", "JWT client_id is required for internal_api scope");
        }
        if (allowedClientIds == null || allowedClientIds.isEmpty() || allowedClientIds.contains(clientId)) {
            return OAuth2TokenValidatorResult.success();
        }
        return failure("invalid_client", "JWT client_id is not allowed for internal_api scope");
    }

    private Set<String> extractScopes(Object scopeClaim) {
        Set<String> scopes = new LinkedHashSet<>();
        if (scopeClaim instanceof String scopeString) {
            for (String part : scopeString.trim().split("\\s+")) {
                if (!part.isBlank()) {
                    scopes.add(part);
                }
            }
        } else if (scopeClaim instanceof Collection<?> scopeCollection) {
            for (Object item : scopeCollection) {
                if (item != null) {
                    String scope = item.toString().trim();
                    if (!scope.isBlank()) {
                        scopes.add(scope);
                    }
                }
            }
        }
        return scopes;
    }

    private OAuth2TokenValidatorResult failure(String code, String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(code, description, null));
    }
}
