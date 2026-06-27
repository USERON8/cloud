package com.cloud.payment.service.support;

import org.springframework.util.StringUtils;

public final class PaymentTextSupport {

  private PaymentTextSupport() {}

  public static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
