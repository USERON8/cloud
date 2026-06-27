package com.cloud.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class OAuthSecretConfigTest {

  @Test
  void baseConfigDoesNotContainCommittedOAuthSecrets() throws Exception {
    String application = readResource("application.yml");

    assertThat(application)
        .doesNotContain("cloud-shop-secret")
        .doesNotContain("cloud-mini-secret")
        .doesNotContain("cloud-client-service-secret-dev");
    assertThat(application)
        .contains("secret: ${APP_OAUTH2_WEB_CLIENT_SECRET}")
        .contains("secret: ${APP_OAUTH2_MOBILE_CLIENT_SECRET}")
        .contains("secret: ${APP_OAUTH2_INTERNAL_CLIENT_SECRET:${CLIENT_SERVICE_SECRET}}");
  }

  @Test
  void devConfigContainsLocalOnlyOAuthSecretFallbacks() throws Exception {
    String applicationDev = readResource("application-dev.yml");

    assertThat(applicationDev)
        .contains("cloud-shop-secret")
        .contains("cloud-mini-secret")
        .contains("cloud-client-service-secret-dev");
  }

  private String readResource(String path) throws Exception {
    return new String(
        new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
