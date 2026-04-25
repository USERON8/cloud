package com.cloud.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class AlipayCallbackExceptionHandlerTest {

  private final AlipayCallbackExceptionHandler handler = new AlipayCallbackExceptionHandler();

  @Test
  void handleExceptionReturnsFailurePlainTextBody() {
    ResponseEntity<String> response = handler.handleException(new IllegalArgumentException("bad"));

    assertEquals(200, response.getStatusCode().value());
    assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    assertEquals("failure", response.getBody());
  }
}
