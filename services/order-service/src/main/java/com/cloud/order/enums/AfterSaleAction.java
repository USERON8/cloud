package com.cloud.order.enums;

import com.cloud.common.exception.BizException;
import java.util.Arrays;

public enum AfterSaleAction {
  AUDIT("AUDIT", "AUDITING"),
  APPROVE("APPROVE", "APPROVED"),
  REJECT("REJECT", "REJECTED"),
  WAIT_RETURN("WAIT_RETURN", "WAIT_RETURN"),
  RETURN("RETURN", "RETURNED"),
  RECEIVE("RECEIVE", "RECEIVED"),
  PROCESS("PROCESS", "REFUNDING"),
  REFUND("REFUND", "REFUNDED"),
  CANCEL("CANCEL", "CANCELLED"),
  CLOSE("CLOSE", "CLOSED");

  private final String code;
  private final String targetStatus;

  AfterSaleAction(String code, String targetStatus) {
    this.code = code;
    this.targetStatus = targetStatus;
  }

  public String code() {
    return code;
  }

  public String targetStatus() {
    return targetStatus;
  }

  public static AfterSaleAction fromValue(String rawAction) {
    if (rawAction == null || rawAction.isBlank()) {
      throw new BizException("after-sale action is required");
    }
    String normalized = rawAction.trim().toUpperCase();
    return Arrays.stream(values())
        .filter(action -> action.code.equals(normalized))
        .findFirst()
        .orElseThrow(() -> new BizException("unsupported after-sale action: " + normalized));
  }
}
