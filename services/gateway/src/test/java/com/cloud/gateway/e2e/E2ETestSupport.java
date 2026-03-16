package com.cloud.gateway.e2e;

import io.restassured.RestAssured;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;

final class E2ETestSupport {

  private E2ETestSupport() {}

  static void initRestAssured() {
    RestAssured.baseURI = baseUrl();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  static void assumeHasAccessToken() {
    Assumptions.assumeTrue(hasText(accessToken()), "E2E access token is required");
  }

  static void assumeHasInternalToken() {
    Assumptions.assumeTrue(hasText(internalToken()), "E2E internal token is required");
  }

  static String baseUrl() {
    return get("E2E_BASE_URL", "http://127.0.0.1:18080");
  }

  static String accessToken() {
    return get("E2E_ACCESS_TOKEN", "");
  }

  static String internalToken() {
    return get("E2E_INTERNAL_TOKEN", "");
  }

  static long userId() {
    return getLong("E2E_USER_ID", 20001L);
  }

  static long merchantId() {
    return getLong("E2E_MERCHANT_ID", 30001L);
  }

  static long categoryId() {
    return getLong("E2E_CATEGORY_ID", 300L);
  }

  static long spuId() {
    return getLong("E2E_SPU_ID", 50001L);
  }

  static long skuId() {
    return getLong("E2E_SKU_ID", 51001L);
  }

  static String receiverName() {
    return get("E2E_RECEIVER_NAME", "test-user");
  }

  static String receiverPhone() {
    return get("E2E_RECEIVER_PHONE", "13800000000");
  }

  static String receiverAddress() {
    return get("E2E_RECEIVER_ADDRESS", "test-address");
  }

  static String newIdempotencyKey() {
    return "idem-" + UUID.randomUUID();
  }

  static String newPaymentNo() {
    return "PAY" + System.currentTimeMillis();
  }

  static String newCallbackNo() {
    return "CB" + System.currentTimeMillis();
  }

  private static String get(String key, String fallback) {
    String value = System.getenv(key);
    if (!hasText(value)) {
      value = System.getProperty(key, fallback);
    }
    return hasText(value) ? value : fallback;
  }

  private static long getLong(String key, long fallback) {
    String value = System.getenv(key);
    if (!hasText(value)) {
      value = System.getProperty(key);
    }
    if (!hasText(value)) {
      return fallback;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return fallback;
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
