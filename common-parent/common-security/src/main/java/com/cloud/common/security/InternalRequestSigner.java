package com.cloud.common.security;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class InternalRequestSigner {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private InternalRequestSigner() {}

  public static String sign(
      String method,
      String path,
      String timestamp,
      String subject,
      String userId,
      String username,
      String clientId,
      String roles,
      String permissions,
      String scopes,
      String secret) {
    String payload =
        safe(method)
            + "\n"
            + safe(path)
            + "\n"
            + safe(timestamp)
            + "\n"
            + safe(subject)
            + "\n"
            + safe(userId)
            + "\n"
            + safe(username)
            + "\n"
            + safe(clientId)
            + "\n"
            + safe(roles)
            + "\n"
            + safe(permissions)
            + "\n"
            + safe(scopes);
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return toHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("failed to sign internal request", ex);
    }
  }

  public static boolean constantTimeEquals(String actual, String expected) {
    if (actual == null || expected == null) {
      return false;
    }
    byte[] left = actual.getBytes(StandardCharsets.UTF_8);
    byte[] right = expected.getBytes(StandardCharsets.UTF_8);
    if (left.length != right.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < left.length; i++) {
      result |= left[i] ^ right[i];
    }
    return result == 0;
  }

  private static String safe(String value) {
    return Objects.toString(value, "");
  }

  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }
}
