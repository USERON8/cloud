package com.cloud.auth.service;

import com.cloud.common.domain.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2TokenManagementService {

    private final OAuth2AuthorizationService authorizationService;
    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final TokenBlacklistService tokenBlacklistService;
    @Qualifier("oauth2MainRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.oauth2.default-redirect-uri:http://127.0.0.1:80/authorized}")
    private String defaultRedirectUri;

    public OAuth2Authorization generateTokensForUser(UserDTO userDTO, Set<String> scopes) {
        if (userDTO == null) {
            throw new IllegalArgumentException("User DTO cannot be null");
        }

        RegisteredClient registeredClient = registeredClientRepository.findByClientId("web-client");
        if (registeredClient == null) {
            throw new IllegalStateException("Registered client 'web-client' not found");
        }

        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (userDTO.getUserType() != null) {
            switch (userDTO.getUserType()) {
                case ADMIN -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                case MERCHANT -> authorities.add(new SimpleGrantedAuthority("ROLE_MERCHANT"));
                case USER -> {
                }
            }
        }

        UsernamePasswordAuthenticationToken userAuthentication =
                new UsernamePasswordAuthenticationToken(userDTO.getUsername(), "[PROTECTED]", authorities);

        if (scopes == null || scopes.isEmpty()) {
            scopes = Set.of("openid", "profile", "read", "write", "user.read", "user.write");
        }
        Set<String> allowedScopes = registeredClient.getScopes();
        scopes = scopes.stream().filter(allowedScopes::contains).collect(Collectors.toSet());

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(UUID.randomUUID().toString())
                .principalName(userDTO.getUsername())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(scopes)
                .attribute(Principal.class.getName(), userAuthentication)
                .attribute("user_id", userDTO.getId())
                .attribute("user_type", userDTO.getUserType() != null ? userDTO.getUserType().getCode() : null)
                .attribute("nickname", userDTO.getNickname())
                .attribute("authorization_code", "SIMULATED_" + UUID.randomUUID())
                .attribute("redirect_uri", defaultRedirectUri);

        OAuth2TokenContext accessTokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(userAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorizationBuilder.build())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(scopes)
                .build();

        OAuth2Token accessToken = tokenGenerator.generate(accessTokenContext);
        if (!(accessToken instanceof OAuth2AccessToken oauth2AccessToken)) {
            throw new IllegalStateException("Generated token is not an OAuth2 access token");
        }
        authorizationBuilder.accessToken(oauth2AccessToken);

        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshTokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(registeredClient)
                    .principal(userAuthentication)
                    .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                    .authorization(authorizationBuilder.build())
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizedScopes(scopes)
                    .build();
            OAuth2Token refreshToken = tokenGenerator.generate(refreshTokenContext);
            if (refreshToken instanceof OAuth2RefreshToken oauth2RefreshToken) {
                authorizationBuilder.refreshToken(oauth2RefreshToken);
            }
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);
        return authorization;
    }

    public OAuth2Authorization generateTokensForClient(String clientId, Set<String> scopes) {
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("Client not found: " + clientId);
        }
        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.CLIENT_CREDENTIALS)) {
            throw new IllegalArgumentException("Client does not support client_credentials grant");
        }

        if (scopes == null || scopes.isEmpty()) {
            scopes = registeredClient.getScopes();
        } else {
            scopes = scopes.stream().filter(registeredClient.getScopes()::contains).collect(Collectors.toSet());
        }

        OAuth2ClientAuthenticationToken clientAuthentication = new OAuth2ClientAuthenticationToken(
                registeredClient,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                registeredClient.getClientSecret()
        );

        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(UUID.randomUUID().toString())
                .principalName(clientId)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(scopes);

        OAuth2TokenContext tokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .principal(clientAuthentication)
                .authorizationServerContext(AuthorizationServerContextHolder.getContext())
                .authorization(authorizationBuilder.build())
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizedScopes(scopes)
                .build();

        OAuth2Token accessToken = tokenGenerator.generate(tokenContext);
        if (!(accessToken instanceof OAuth2AccessToken oauth2AccessToken)) {
            throw new IllegalStateException("Generated token is not an OAuth2 access token");
        }

        OAuth2Authorization authorization = authorizationBuilder.accessToken(oauth2AccessToken).build();
        authorizationService.save(authorization);
        return authorization;
    }

    public void revokeToken(String tokenValue) {
        OAuth2Authorization authorization = findByToken(tokenValue);
        if (authorization != null) {
            revokeAuthorization(authorization, "manual_revocation");
        }
    }

    public OAuth2Authorization findByToken(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            return null;
        }
        return authorizationService.findByToken(tokenValue, null);
    }

    public boolean isTokenValid(String tokenValue) {
        OAuth2Authorization authorization = findByToken(tokenValue);
        if (authorization == null) {
            return false;
        }
        if (tokenBlacklistService.isBlacklisted(tokenValue)) {
            return false;
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null && accessToken.getToken() != null) {
            Instant expiresAt = accessToken.getToken().getExpiresAt();
            if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                return false;
            }
        }
        return true;
    }

    public boolean logout(String accessToken, String refreshToken) {
        boolean revoked = false;

        if (StringUtils.hasText(accessToken)) {
            OAuth2Authorization authorization = findByToken(accessToken);
            if (authorization != null) {
                revokeAuthorization(authorization, "logout");
                revoked = true;
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            OAuth2Authorization authorization = findByToken(refreshToken);
            if (authorization != null) {
                revokeAuthorization(authorization, "logout");
                revoked = true;
            }
        }

        return revoked;
    }

    public int logoutAllSessions(String username) {
        if (!StringUtils.hasText(username)) {
            return 0;
        }

        Set<String> authKeys = redisTemplate.keys("oauth2:auth:*");
        if (authKeys == null || authKeys.isEmpty()) {
            return 0;
        }

        int revokedCount = 0;
        for (String key : authKeys) {
            Object principalName = redisTemplate.opsForHash().get(key, "principalName");
            if (principalName == null || !username.equals(principalName.toString())) {
                continue;
            }

            String authorizationId = key.substring("oauth2:auth:".length());
            OAuth2Authorization authorization = authorizationService.findById(authorizationId);
            if (authorization != null) {
                revokeAuthorization(authorization, "logout_all_sessions");
                revokedCount++;
            }
        }
        return revokedCount;
    }

    private void revokeAuthorization(OAuth2Authorization authorization, String reason) {
        try {
            if (authorization.getAccessToken() != null && authorization.getAccessToken().getToken() != null) {
                String accessTokenValue = authorization.getAccessToken().getToken().getTokenValue();
                long ttl = computeTtlSeconds(authorization.getAccessToken().getToken().getExpiresAt(), 3600);
                tokenBlacklistService.addToBlacklist(accessTokenValue, authorization.getPrincipalName(), ttl, reason);
            }

            if (authorization.getRefreshToken() != null && authorization.getRefreshToken().getToken() != null) {
                String refreshTokenValue = authorization.getRefreshToken().getToken().getTokenValue();
                long ttl = computeTtlSeconds(authorization.getRefreshToken().getToken().getExpiresAt(), 2592000);
                tokenBlacklistService.addToBlacklist(refreshTokenValue, authorization.getPrincipalName(), ttl, reason);
            }

            authorizationService.remove(authorization);
        } catch (Exception e) {
            log.warn("Failed to revoke authorization id={}: {}", authorization.getId(), e.getMessage());
        }
    }

    private long computeTtlSeconds(Instant expiresAt, long defaultTtl) {
        if (expiresAt == null) {
            return defaultTtl;
        }
        long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        return Math.max(seconds, 60);
    }
}
