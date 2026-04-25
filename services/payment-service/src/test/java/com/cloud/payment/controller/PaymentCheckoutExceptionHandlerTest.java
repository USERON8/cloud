package com.cloud.payment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloud.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class PaymentCheckoutExceptionHandlerTest {

  private final PaymentCheckoutExceptionHandler handler = new PaymentCheckoutExceptionHandler();

  @Test
  void handleBizExceptionReturnsHtmlPageWithEscapedMessage() {
    ResponseEntity<String> response =
        handler.handleBizException(new BizException("invalid <ticket> & session"));

    assertEquals(200, response.getStatusCode().value());
    assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
    assertTrue(response.getBody().contains("Payment unavailable"));
    assertTrue(response.getBody().contains("invalid &lt;ticket&gt; &amp; session"));
  }

  @Test
  void handleExceptionReturnsGenericHtmlPage() {
    ResponseEntity<String> response = handler.handleException(new RuntimeException("boom"));

    assertEquals(200, response.getStatusCode().value());
    assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
    assertTrue(response.getBody().contains("Payment unavailable"));
    assertTrue(response.getBody().contains("Failed to initialize the payment page."));
  }
}
