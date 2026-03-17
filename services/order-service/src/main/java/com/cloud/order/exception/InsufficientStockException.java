package com.cloud.order.exception;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;

public class InsufficientStockException extends BizException {

  private Long productId;

  public InsufficientStockException(String message) {
    super(ResultCode.STOCK_INSUFFICIENT, message);
  }

  public InsufficientStockException(Long productId, Integer required, Integer available) {
    super(
        ResultCode.STOCK_INSUFFICIENT,
        String.format("库存不足，商品ID: %d，需要: %d，可用: %d", productId, required, available));
    this.productId = productId;
  }

  public InsufficientStockException(String productName, Integer required, Integer available) {
    super(
        ResultCode.STOCK_INSUFFICIENT,
        String.format("库存不足，商品: %s，需要: %d，可用: %d", productName, required, available));
  }

  public InsufficientStockException(String message, Throwable cause) {
    super(ResultCode.STOCK_INSUFFICIENT, message, cause);
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }
}
