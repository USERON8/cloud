package com.cloud.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2ClientConfigTest {

  @Test
  void clientRegistrationRepositoryShouldAllowEmptyOptionalRegistrations() {
    OAuth2ClientConfig config = new OAuth2ClientConfig();
    ReflectionTestUtils.setField(config, "authTokenUri", "http://127.0.0.1:8081/oauth2/token");
    ReflectionTestUtils.setField(config, "githubClientId", "");
    ReflectionTestUtils.setField(config, "githubClientSecret", "");
    ReflectionTestUtils.setField(config, "githubRedirectUri", "http://127.0.0.1/callback");
    ReflectionTestUtils.setField(config, "serviceClientId", "client-service");
    ReflectionTestUtils.setField(config, "serviceClientSecret", "");

    ClientRegistrationRepository repository = config.clientRegistrationRepository();

    assertThat(repository.findByRegistrationId("github")).isNull();
    assertThat(repository.findByRegistrationId("client-service")).isNull();
  }

  @Test
  void clientRegistrationRepositoryShouldRegisterConfiguredClientsOnly() {
    OAuth2ClientConfig config = new OAuth2ClientConfig();
    ReflectionTestUtils.setField(config, "authTokenUri", "http://127.0.0.1:8081/oauth2/token");
    ReflectionTestUtils.setField(config, "githubClientId", "github-id");
    ReflectionTestUtils.setField(config, "githubClientSecret", "github-secret");
    ReflectionTestUtils.setField(config, "githubRedirectUri", "http://127.0.0.1/callback");
    ReflectionTestUtils.setField(config, "serviceClientId", "client-service");
    ReflectionTestUtils.setField(config, "serviceClientSecret", "service-secret");

    ClientRegistrationRepository repository = config.clientRegistrationRepository();

    assertThat(repository.findByRegistrationId("github")).isNotNull();
    assertThat(repository.findByRegistrationId("client-service")).isNotNull();
  }
}
