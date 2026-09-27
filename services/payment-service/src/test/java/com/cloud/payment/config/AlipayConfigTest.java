package com.cloud.payment.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AlipayConfigTest {

  @Test
  void allowsMissingCredentialsWhenProviderIsDisabled() {
    AlipayConfig config = new AlipayConfig();

    assertThat(config.isCredentialConfigurationValid()).isTrue();
  }

  @Test
  void requiresAllCredentialsWhenProviderIsEnabled() {
    AlipayConfig config = new AlipayConfig();
    config.setEnabled(true);

    assertThat(config.isCredentialConfigurationValid()).isFalse();

    config.setAppId("app-id");
    config.setMerchantPrivateKey("private-key");
    config.setAlipayPublicKey("public-key");

    assertThat(config.isCredentialConfigurationValid()).isTrue();
  }
}
