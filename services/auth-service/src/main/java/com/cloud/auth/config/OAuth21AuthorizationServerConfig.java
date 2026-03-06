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
import org.springframework.util.StringUtils;

import java.time.Duration;

@Slf4j
@Configuration
@Import({
        SecurityFilterChainConfig.class,
        JwtPasswordConfig.class,
        AuthenticationProviderConfig.class,
        RedisOAuth2Config.class
})
public class OAuth21AuthorizationServerConfig {

    private static final String WEB_CLIENT_ID = "9ef97d40-818a-47c8-bce7-5c0d0c80f851";
    private static final String MOBILE_CLIENT_ID = "1111c7d3-cf12-4b2d-ad1f-f86713e52b9f";
    private static final String SERVICE_CLIENT_ID = "53ee7f0b-3f93-4859-b5f3-b0ab816901a4";
    private static final String INTERNAL_SERVICE_CLIENT_ID = "c4d3b385-90a5-4b8d-a664-29a6b2f8dd4a";

    @Value("${app.jwt.issuer:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
    private String issuer;

    @Value("${app.oauth2.clients.web.id:web-client}")
    private String webClientId;

    @Value("${app.oauth2.clients.web.secret:}")
    private String webClientSecret;

    @Value("${app.oauth2.clients.mobile.id:mobile-client}")
    private String mobileClientId;

    @Value("${app.oauth2.clients.service.id:service-client}")
    private String serviceClientId;

    @Value("${app.oauth2.clients.service.secret:}")
    private String serviceClientSecret;

    @Value("${app.oauth2.clients.internal.id:client-service}")
    private String internalClientId;

    @Value("${app.oauth2.clients.internal.secret:}")
    private String internalClientSecret;

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
        RegisteredClient webAppClient = RegisteredClient.withId(WEB_CLIENT_ID)
                .clientId(webClientId)
                .clientSecret(requiredAndNormalizedSecret(webClientSecret, webClientId))
                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                    grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS);
                })
                .redirectUris(uris -> {
                    uris.add("http://127.0.0.1/authorized");
                    uris.add("http://127.0.0.1:3000/callback");
                    uris.add("http://127.0.0.1:80/login/callback");
                    uris.add("http://127.0.0.1:80/login/callback");
                })
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID);
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add(OidcScopes.EMAIL);
                    scopes.add("read");
                    scopes.add("write");
                    scopes.add("admin");
                    scopes.add("user:read");
                    scopes.add("user:write");
                    scopes.add("order:read");
                    scopes.add("order:write");
                })
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(createSecureTokenSettings())
                .build();

        RegisteredClient mobileAppClient = RegisteredClient.withId(MOBILE_CLIENT_ID)
                .clientId(mobileClientId)
                .clientAuthenticationMethods(methods -> methods.add(ClientAuthenticationMethod.NONE))
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .redirectUris(uris -> uris.add("com.example.app://callback"))
                .scopes(scopes -> {
                    scopes.add(OidcScopes.OPENID);
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add("read");
                })
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(createMobileTokenSettings())
                .build();

        RegisteredClient serviceClient = RegisteredClient.withId(SERVICE_CLIENT_ID)
                .clientId(serviceClientId)
                .clientSecret(requiredAndNormalizedSecret(serviceClientSecret, serviceClientId))
                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })
                .authorizationGrantTypes(grantTypes -> grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS))
                .scopes(scopes -> {
                    scopes.add("service:read");
                    scopes.add("service:write");
                    scopes.add("service:admin");
                })
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(createServiceTokenSettings())
                .build();

        RegisteredClient internalServiceClient = RegisteredClient.withId(INTERNAL_SERVICE_CLIENT_ID)
                .clientId(internalClientId)
                .clientSecret(requiredAndNormalizedSecret(internalClientSecret, internalClientId))
                .clientAuthenticationMethods(methods -> {
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_POST);
                })
                .authorizationGrantTypes(grantTypes -> grantTypes.add(AuthorizationGrantType.CLIENT_CREDENTIALS))
                .scopes(scopes -> scopes.add("internal_api"))
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .tokenSettings(createServiceTokenSettings())
                .build();

        


        return new InMemoryRegisteredClientRepository(
                webAppClient,
                mobileAppClient,
                serviceClient,
                internalServiceClient
        );
    }

    private String requiredAndNormalizedSecret(String secret, String clientId) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Missing client secret for " + clientId);
        }

        if (secret.startsWith("{")) {
            return secret;
        }

        log.warn("Client {} secret is plaintext, auto-prefixing with {noop}. Use encoded secret in production", clientId);
        return "{noop}" + secret;
    }

    private TokenSettings createSecureTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .reuseRefreshTokens(false)
                .authorizationCodeTimeToLive(Duration.ofMinutes(10))
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();
    }

    private TokenSettings createMobileTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(15))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .authorizationCodeTimeToLive(Duration.ofMinutes(5))
                .idTokenSignatureAlgorithm(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
                .build();
    }

    private TokenSettings createServiceTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(2))
                .refreshTokenTimeToLive(Duration.ofDays(1))
                .reuseRefreshTokens(false)
                .build();
    }
}
