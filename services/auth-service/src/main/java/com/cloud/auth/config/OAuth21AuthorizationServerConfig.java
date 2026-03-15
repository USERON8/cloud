package com.cloud.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@Import({
        SecurityFilterChainConfig.class,
        JwtPasswordConfig.class,
        AuthenticationProviderConfig.class,
        RedisOAuth2Config.class
})
public class OAuth21AuthorizationServerConfig {

    @Value("${app.jwt.issuer:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuer;

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .jwkSetEndpoint("/.well-known/jwks.json")
                .oidcLogoutEndpoint("/connect/logout")
                .oidcUserInfoEndpoint("/userinfo")
                .oidcClientRegistrationEndpoint("/connect/register")
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient userClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("cloud-shop-client")
                .clientSecret("{noop}cloud-shop-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:18080/login/oauth2/code/cloud")
                .scope(OidcScopes.OPENID)
                .scope("user.read")
                .scope("order.write")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(userTokenSettings())
                .build();

        RegisteredClient serviceClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("client-service")
                .clientSecret("{noop}cloud-client-service-secret-dev")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("internal")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(serviceTokenSettings())
                .build();

        RegisteredClient miniClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("cloud-mini-client")
                .clientSecret("{noop}cloud-mini-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("weixin://oauth2/callback")
                .scope(OidcScopes.OPENID)
                .scope("user.read")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(userTokenSettings())
                .build();

        return new InMemoryRegisteredClientRepository(userClient, serviceClient, miniClient);
    }

    private TokenSettings userTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .reuseRefreshTokens(false)
                .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();
    }

    private TokenSettings serviceTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .reuseRefreshTokens(false)
                .build();
    }
}


