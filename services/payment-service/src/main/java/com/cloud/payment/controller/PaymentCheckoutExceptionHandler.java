package com.cloud.payment.controller;

import com.cloud.common.annotation.RawResponse;
import com.cloud.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RawResponse
@RestControllerAdvice(assignableTypes = PaymentCheckoutController.class)
public class PaymentCheckoutExceptionHandler {

  private static final String TITLE_PAYMENT_UNAVAILABLE = "Payment unavailable";
  private static final String MESSAGE_CHECKOUT_INIT_FAILED =
      "Failed to initialize the payment page.";

  @ExceptionHandler(BizException.class)
  public ResponseEntity<String> handleBizException(BizException ex) {
    log.warn("Render payment checkout page failed: {}", ex.getMessage());
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(buildHtmlPage(TITLE_PAYMENT_UNAVAILABLE, ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleException(Exception ex) {
    log.warn("Render payment checkout page failed", ex);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(buildHtmlPage(TITLE_PAYMENT_UNAVAILABLE, MESSAGE_CHECKOUT_INIT_FAILED));
  }

  private String buildHtmlPage(String title, String message) {
    return """
        <!DOCTYPE html>
        <html lang="en">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%s</title>
            <style>
              body {
                margin: 0;
                min-height: 100vh;
                display: grid;
                place-items: center;
                background: #f4f7fb;
                color: #1f2a37;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
              }
              main {
                max-width: 420px;
                padding: 24px;
                border-radius: 18px;
                background: #ffffff;
                box-shadow: 0 16px 48px rgba(15, 23, 42, 0.08);
                text-align: center;
              }
              h1 {
                margin: 0 0 12px;
                font-size: 24px;
              }
              p {
                margin: 0;
                line-height: 1.6;
                color: #526072;
              }
            </style>
          </head>
          <body>
            <main>
              <h1>%s</h1>
              <p>%s</p>
            </main>
          </body>
        </html>
        """
        .formatted(escapeHtml(title), escapeHtml(title), escapeHtml(message));
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
