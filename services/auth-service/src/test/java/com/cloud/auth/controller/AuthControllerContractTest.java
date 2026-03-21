package com.cloud.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AuthControllerContractTest {

  @Test
  void logoutShouldRequireAuthenticatedUser() throws Exception {
    Method method =
        AuthController.class.getDeclaredMethod(
            "logout", jakarta.servlet.http.HttpServletRequest.class);
    PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo("isAuthenticated()");
  }
}
