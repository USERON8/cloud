package com.cloud.auth.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
@Import({
  SecurityFilterChainConfig.class,
  JwtPasswordConfig.class,
  AuthenticationProviderConfig.class,
  RedisOAuth2Config.class
})
public class OAuth21AuthorizationServerConfig {

  private final PasswordEncoder passwordEncoder;

  @Value("${app.jwt.issuer:${AUTH_ISSUER_URI:http://127.0.0.1:8081}}")
  private String issuer;

  @Value("${app.oauth2.clients.web.id:web-client}")
  private String webClientId;

  @Value("${app.oauth2.clients.web.secret:${APP_OAUTH2_WEB_CLIENT_SECRET:cloud-shop-secret}}")
  private String webClientSecret;

  @Value(
      "${app.oauth2.clients.web.redirect-uris:http://127.0.0.1:${PORT_NGINX_HTTP:18080}/callback}")
  private String webRedirectUris;

  @Value("${app.oauth2.clients.internal.id:client-service}")
  private String internalClientId;

  @Value(
      "${app.oauth2.clients.internal.secret:${APP_OAUTH2_INTERNAL_CLIENT_SECRET:${CLIENT_SERVICE_SECRET:cloud-client-service-secret-dev}}}")
  private String internalClientSecret;

  @Value("${app.oauth2.clients.mobile.id:mobile-client}")
  private String mobileClientId;

  @Value("${app.oauth2.clients.mobile.secret:${APP_OAUTH2_MOBILE_CLIENT_SECRET:cloud-mini-secret}}")
  private String mobileClientSecret;

  @Value("${app.oauth2.clients.mobile.redirect-uris:weixin://oauth2/callback}")
  private String mobileRedirectUris;

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
    RegisteredClient.Builder userClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(webClientId)
            .clientSecret(encodeClientSecret(webClientSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scope(OidcScopes.OPENID)
            .scope("user.read")
            .scope("order.write")
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(true)
                    .build())
            .tokenSettings(userTokenSettings());
    applyRedirectUris(userClient, webRedirectUris);

    RegisteredClient serviceClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(internalClientId)
            .clientSecret(encodeClientSecret(internalClientSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .scope("internal")
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(false)
                    .build())
            .tokenSettings(serviceTokenSettings())
            .build();

    RegisteredClient.Builder miniClient =
        RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId(mobileClientId)
            .clientSecret(encodeClientSecret(mobileClientSecret))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scope(OidcScopes.OPENID)
            .scope("user.read")
            .clientSettings(
                ClientSettings.builder()
                    .requireAuthorizationConsent(false)
                    .requireProofKey(true)
                    .build())
            .tokenSettings(userTokenSettings());
    applyRedirectUris(miniClient, mobileRedirectUris);

    return new InMemoryRegisteredClientRepository(
        userClient.build(), serviceClient, miniClient.build());
  }

  private TokenSettings userTokenSettings() {
    return TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofHours(2))
        .refreshTokenTimeToLive(Duration.ofDays(7))
        .reuseRefreshTokens(false)
        .authorizationCodeTimeToLive(Duration.ofMinutes(5))
        .idTokenSignatureAlgorithm(
            org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256)
        .build();
  }

  private TokenSettings serviceTokenSettings() {
    return TokenSettings.builder()
        .accessTokenTimeToLive(Duration.ofHours(1))
        .reuseRefreshTokens(false)
        .build();
  }

  private void applyRedirectUris(RegisteredClient.Builder builder, String redirectUris) {
    if (!StringUtils.hasText(redirectUris)) {
      throw new IllegalStateException("OAuth2 redirect URIs must not be empty");
    }
    Arrays.stream(redirectUris.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .forEach(builder::redirectUri);
  }

  private String encodeClientSecret(String secret) {
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("OAuth2 client secret must not be empty");
    }
    String trimmedSecret = secret.trim();
    if (trimmedSecret.startsWith("{")) {
      return trimmedSecret;
    }
    return passwordEncoder.encode(trimmedSecret);
  }
}
