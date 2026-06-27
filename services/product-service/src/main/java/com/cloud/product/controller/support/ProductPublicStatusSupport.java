package com.cloud.product.controller.support;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;

public final class ProductPublicStatusSupport {

  private ProductPublicStatusSupport() {}

  public static Integer normalizePublicStatus(Integer status) {
    if (status == null) {
      return 1;
    }
    if (!Integer.valueOf(1).equals(status)) {
      throw new BizException(
          ResultCode.BAD_REQUEST, "public product queries only support active status");
    }
    return status;
  }
}
