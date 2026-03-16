package com.cloud.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

class RedisOAuth2ConfigTest {

  @Test
  void oauth2ValueSerializerShouldRoundTripAuthorizationServerPayload() {
    RedisOAuth2Config config = new RedisOAuth2Config(null, null, null);
    RedisSerializer<Object> serializer = config.oauth2ValueSerializer();
    RegisteredClient registeredClient =
        RegisteredClient.withId("registered-client-id")
            .clientId("web-client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1:18080/callback")
            .scope(OidcScopes.OPENID)
            .clientSettings(ClientSettings.builder().requireProofKey(true).build())
            .build();
    Instant issuedAt = Instant.parse("2026-03-08T16:00:00Z");
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("tester")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(new OAuth2AuthorizationCode("code-value", issuedAt, issuedAt.plusSeconds(300)))
            .build();

    byte[] serialized = serializer.serialize(authorization);
    Object deserialized = serializer.deserialize(serialized);

    OAuth2Authorization restored = assertInstanceOf(OAuth2Authorization.class, deserialized);
    assertNotNull(restored.getAuthorizationGrantType());
    assertEquals(
        AuthorizationGrantType.AUTHORIZATION_CODE.getValue(),
        restored.getAuthorizationGrantType().getValue());
    assertEquals(
        "code-value", restored.getToken(OAuth2AuthorizationCode.class).getToken().getTokenValue());
  }
}
