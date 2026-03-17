package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;

public class OrderBusinessException extends BizException {

  private final Long orderId;
  private final String errorCode;

  public OrderBusinessException(String message) {
    super(ResultCode.BUSINESS_ERROR, message);
    this.orderId = null;
    this.errorCode = null;
  }

  public OrderBusinessException(String message, Throwable cause) {
    super(ResultCode.BUSINESS_ERROR, message, cause);
    this.orderId = null;
    this.errorCode = null;
  }

  public OrderBusinessException(Long orderId, String message) {
    super(ResultCode.BUSINESS_ERROR, message);
    this.orderId = orderId;
    this.errorCode = null;
  }

  public OrderBusinessException(Long orderId, String errorCode, String message) {
    super(resolveCode(errorCode), message);
    this.orderId = orderId;
    this.errorCode = errorCode;
  }

  public OrderBusinessException(Long orderId, String message, Throwable cause) {
    super(ResultCode.BUSINESS_ERROR, message, cause);
    this.orderId = orderId;
    this.errorCode = null;
  }

  public Long getOrderId() {
    return orderId;
  }

  public String getErrorCode() {
    return errorCode;
  }

  private static int resolveCode(String errorCode) {
    if (errorCode == null || errorCode.isBlank()) {
      return ResultCode.BUSINESS_ERROR.getCode();
    }
    try {
      return Integer.parseInt(errorCode);
    } catch (NumberFormatException ex) {
      return ResultCode.BUSINESS_ERROR.getCode();
    }
  }
}
