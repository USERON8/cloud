package com.cloud.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class JwtAuthorityUtils {

    private JwtAuthorityUtils() {
    }

    public static JwtAuthenticationConverter buildJwtAuthenticationConverter(
            boolean lowerCaseScope,
            boolean includeAuthoritiesClaim,
            BiConsumer<Set<GrantedAuthority>, Jwt> customizer
    ) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(buildAuthoritiesConverter(
                lowerCaseScope,
                includeAuthoritiesClaim,
                customizer
        ));
        return converter;
    }

    public static Converter<Jwt, Collection<GrantedAuthority>> buildAuthoritiesConverter(
            boolean lowerCaseScope,
            boolean includeAuthoritiesClaim,
            BiConsumer<Set<GrantedAuthority>, Jwt> customizer
    ) {
        return jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();

            authorities.addAll(extractScopeAuthorities(jwt.getClaim("scope"), lowerCaseScope));
            authorities.addAll(extractScopeAuthorities(jwt.getClaim("scp"), lowerCaseScope));

            if (includeAuthoritiesClaim) {
                authorities.addAll(extractRawAuthorities(jwt.getClaim("authorities")));
            }

            String userType = jwt.getClaimAsString("user_type");
            if (userType != null && !userType.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + userType.trim().toUpperCase(Locale.ROOT)));
            }

            if (customizer != null) {
                customizer.accept(authorities, jwt);
            }

            return authorities;
        };
    }

    private static Set<GrantedAuthority> extractScopeAuthorities(Object scopeClaim, boolean lowerCaseScope) {
        Set<String> normalizedScopes = new LinkedHashSet<>();
        if (scopeClaim == null) {
            return Set.of();
        }
        if (scopeClaim instanceof String scopeString) {
            normalizedScopes.addAll(Arrays.stream(scopeString.trim().split("\\s+"))
                    .map(scope -> normalizeScope(scope, lowerCaseScope))
                    .filter(scope -> !scope.isBlank())
                    .collect(Collectors.toSet()));
        } else if (scopeClaim instanceof Collection<?> scopeCollection) {
            normalizedScopes.addAll(scopeCollection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(scope -> normalizeScope(scope, lowerCaseScope))
                    .filter(scope -> !scope.isBlank())
                    .collect(Collectors.toSet()));
        }

        return normalizedScopes.stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<GrantedAuthority> extractRawAuthorities(Object authoritiesClaim) {
        Set<String> rawAuthorities = new LinkedHashSet<>();
        if (authoritiesClaim == null) {
            return Set.of();
        }
        if (authoritiesClaim instanceof String authorityString) {
            rawAuthorities.addAll(Arrays.stream(authorityString.trim().split("\\s+"))
                    .filter(authority -> !authority.isBlank())
                    .collect(Collectors.toSet()));
        } else if (authoritiesClaim instanceof Collection<?> authorityCollection) {
            rawAuthorities.addAll(authorityCollection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(authority -> !authority.isBlank())
                    .collect(Collectors.toSet()));
        }

        return rawAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeScope(String scope, boolean lowerCaseScope) {
        if (scope == null) {
            return "";
        }
        String normalized = scope.trim().replace('.', ':');
        return lowerCaseScope ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }
}
