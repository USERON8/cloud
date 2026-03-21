package com.cloud.user.controller.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class UserAddressControllerContractTest {

  @Test
  void controllerShouldRequireAuthenticationAtClassLevel() {
    PreAuthorize annotation = UserAddressController.class.getAnnotation(PreAuthorize.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo("isAuthenticated()");
  }
}
