package com.cloud.common.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class GatewayIdentitySignatureSupport {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private GatewayIdentitySignatureSupport() {}

  public static String canonicalPayload(
      String userId,
      String username,
      String nickname,
      String status,
      String clientId,
      String scopes,
      String roles,
      String permissions,
      String authorities,
      String traceId,
      String subject,
      String timestamp) {
    List<String> fields = new ArrayList<>();
    fields.add(safe(userId));
    fields.add(safe(username));
    fields.add(safe(nickname));
    fields.add(safe(status));
    fields.add(safe(clientId));
    fields.add(safe(scopes));
    fields.add(safe(roles));
    fields.add(safe(permissions));
    fields.add(safe(authorities));
    fields.add(safe(traceId));
    fields.add(safe(subject));
    fields.add(safe(timestamp));
    return String.join("\n", fields);
  }

  public static String sign(String payload, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return toHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Sign gateway identity failed", ex);
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
    for (int index = 0; index < left.length; index++) {
      result |= left[index] ^ right[index];
    }
    return result == 0;
  }

  private static String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }
}
