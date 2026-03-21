package com.cloud.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class StockLedgerControllerContractTest {

  @Test
  void stockEndpointsShouldDeclareExpectedAuthorization() throws Exception {
    assertThat(annotationValue("getLedger")).isEqualTo("hasRole('ADMIN')");
    assertThat(annotationValue("reserve")).isEqualTo("hasAuthority('SCOPE_internal')");
    assertThat(annotationValue("confirm")).isEqualTo("hasAuthority('SCOPE_internal')");
    assertThat(annotationValue("release")).isEqualTo("hasAuthority('SCOPE_internal')");
    assertThat(annotationValue("rollback")).isEqualTo("hasAuthority('SCOPE_internal')");
  }

  private String annotationValue(String methodName) throws Exception {
    for (Method method : StockLedgerController.class.getDeclaredMethods()) {
      if (!method.getName().equals(methodName)) {
        continue;
      }
      PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
      assertThat(annotation).as("Missing @PreAuthorize on %s", methodName).isNotNull();
      return annotation.value();
    }
    throw new IllegalStateException("method not found: " + methodName);
  }
}
