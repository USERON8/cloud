package com.cloud.order.enums;

import com.cloud.common.exception.BizException;
import java.util.Arrays;

public enum OrderAction {
  RESERVE("RESERVE", "STOCK_RESERVED"),
  PAY("PAY", "PAID"),
  SHIP("SHIP", "SHIPPED"),
  DONE("DONE", "DONE"),
  RECEIVE("RECEIVE", "DONE"),
  CANCEL("CANCEL", "CANCELLED"),
  CLOSE("CLOSE", "CLOSED");

  private final String code;
  private final String targetStatus;

  OrderAction(String code, String targetStatus) {
    this.code = code;
    this.targetStatus = targetStatus;
  }

  public String code() {
    return code;
  }

  public String targetStatus() {
    return targetStatus;
  }

  public static OrderAction fromValue(String rawAction) {
    if (rawAction == null || rawAction.isBlank()) {
      throw new BizException("order action is required");
    }
    String normalized = rawAction.trim().toUpperCase();
    return Arrays.stream(values())
        .filter(action -> action.code.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new BizException("unsupported order action: " + normalized));
  }
}
