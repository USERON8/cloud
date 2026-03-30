package com.cloud.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth21AuthorizationServerConfigTest {

  private final PasswordEncoder passwordEncoder = new JwtPasswordConfig().passwordEncoder();

  @Test
  void registeredClientRepositoryShouldUseConfiguredIdsAndEncodedSecrets() {
    OAuth21AuthorizationServerConfig config = new OAuth21AuthorizationServerConfig(passwordEncoder);
    ReflectionTestUtils.setField(config, "webClientId", "web-client");
    ReflectionTestUtils.setField(config, "webClientSecret", "web-secret");
    ReflectionTestUtils.setField(
        config,
        "webRedirectUris",
        "http://127.0.0.1:18080/callback,http://127.0.0.1:3000/callback");
    ReflectionTestUtils.setField(config, "internalClientId", "client-service");
    ReflectionTestUtils.setField(config, "internalClientSecret", "internal-secret");
    ReflectionTestUtils.setField(config, "mobileClientId", "mobile-client");
    ReflectionTestUtils.setField(config, "mobileClientSecret", "mobile-secret");
    ReflectionTestUtils.setField(config, "mobileRedirectUris", "weixin://oauth2/callback");
    ReflectionTestUtils.setField(config, "accessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.setField(config, "serviceAccessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.setField(config, "refreshTokenValidity", Duration.ofDays(7));
    ReflectionTestUtils.setField(config, "authorizationCodeValidity", Duration.ofMinutes(5));
    ReflectionTestUtils.setField(config, "blacklistFailClosed", true);
    ReflectionTestUtils.setField(
        config, "maxFailClosedAccessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.invokeMethod(config, "validateTokenPolicy");

    RegisteredClientRepository repository = config.registeredClientRepository();
    var webClient = repository.findByClientId("web-client");
    var serviceClient = repository.findByClientId("client-service");
    var mobileClient = repository.findByClientId("mobile-client");

    assertThat(webClient).isNotNull();
    assertThat(serviceClient).isNotNull();
    assertThat(mobileClient).isNotNull();
    assertThat(webClient.getClientSecret()).doesNotStartWith("{noop}");
    assertThat(serviceClient.getClientSecret()).doesNotStartWith("{noop}");
    assertThat(mobileClient.getClientSecret()).doesNotStartWith("{noop}");
    assertThat(passwordEncoder.matches("web-secret", webClient.getClientSecret())).isTrue();
    assertThat(passwordEncoder.matches("internal-secret", serviceClient.getClientSecret()))
        .isTrue();
    assertThat(passwordEncoder.matches("mobile-secret", mobileClient.getClientSecret())).isTrue();
    assertThat(webClient.getRedirectUris())
        .containsExactlyInAnyOrder(
            "http://127.0.0.1:18080/callback", "http://127.0.0.1:3000/callback");
    assertThat(mobileClient.getRedirectUris()).containsExactly("weixin://oauth2/callback");
  }

  @Test
  void registeredClientRepositoryShouldKeepPreEncodedSecretsUntouched() {
    OAuth21AuthorizationServerConfig config = new OAuth21AuthorizationServerConfig(passwordEncoder);
    ReflectionTestUtils.setField(config, "webClientId", "web-client");
    ReflectionTestUtils.setField(config, "webClientSecret", "{noop}web-secret");
    ReflectionTestUtils.setField(config, "webRedirectUris", "http://127.0.0.1:18080/callback");
    ReflectionTestUtils.setField(config, "internalClientId", "client-service");
    ReflectionTestUtils.setField(config, "internalClientSecret", "{noop}internal-secret");
    ReflectionTestUtils.setField(config, "mobileClientId", "mobile-client");
    ReflectionTestUtils.setField(config, "mobileClientSecret", "{noop}mobile-secret");
    ReflectionTestUtils.setField(config, "mobileRedirectUris", "weixin://oauth2/callback");
    ReflectionTestUtils.setField(config, "accessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.setField(config, "serviceAccessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.setField(config, "refreshTokenValidity", Duration.ofDays(7));
    ReflectionTestUtils.setField(config, "authorizationCodeValidity", Duration.ofMinutes(5));
    ReflectionTestUtils.setField(config, "blacklistFailClosed", true);
    ReflectionTestUtils.setField(
        config, "maxFailClosedAccessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.invokeMethod(config, "validateTokenPolicy");

    RegisteredClientRepository repository = config.registeredClientRepository();

    assertThat(repository.findByClientId("web-client").getClientSecret())
        .isEqualTo("{noop}web-secret");
    assertThat(repository.findByClientId("client-service").getClientSecret())
        .isEqualTo("{noop}internal-secret");
    assertThat(repository.findByClientId("mobile-client").getClientSecret())
        .isEqualTo("{noop}mobile-secret");
  }

  @Test
  void validateTokenPolicyShouldRejectLongAccessTokenWhenFailClosedEnabled() {
    OAuth21AuthorizationServerConfig config = new OAuth21AuthorizationServerConfig(passwordEncoder);
    ReflectionTestUtils.setField(config, "accessTokenValidity", Duration.ofHours(2));
    ReflectionTestUtils.setField(config, "serviceAccessTokenValidity", Duration.ofMinutes(15));
    ReflectionTestUtils.setField(config, "blacklistFailClosed", true);
    ReflectionTestUtils.setField(
        config, "maxFailClosedAccessTokenValidity", Duration.ofMinutes(15));

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(config, "validateTokenPolicy"))
        .hasMessageContaining("access-token-validity");
  }
}
