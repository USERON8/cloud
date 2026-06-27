package com.cloud.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GatewaySecretConfigTest {

  @Test
  void baseConfigDoesNotContainCommittedGatewaySigningSecret() throws Exception {
    String application = readResource("application.yml");

    assertThat(application).doesNotContain("cloud-gateway-signature-dev");
    assertThat(application).contains("secret: ${GATEWAY_SIGNATURE_SECRET}");
  }

  @Test
  void devConfigContainsLocalOnlyGatewaySigningSecretFallback() throws Exception {
    String applicationDev = readResource("application-dev.yml");

    assertThat(applicationDev).contains("cloud-gateway-signature-dev");
  }

  private String readResource(String path) throws Exception {
    return new String(
        new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
