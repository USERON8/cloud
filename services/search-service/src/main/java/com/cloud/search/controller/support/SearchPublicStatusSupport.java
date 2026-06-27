package com.cloud.search.controller.support;

import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.search.dto.ProductSearchRequest;
import com.cloud.search.dto.ShopSearchRequest;

public final class SearchPublicStatusSupport {

  private SearchPublicStatusSupport() {}

  public static void normalize(ProductSearchRequest request) {
    if (request == null) {
      return;
    }
    request.setStatus(
        normalizeStatus(request.getStatus(), "public search only supports active status"));
  }

  public static void normalize(ShopSearchRequest request) {
    if (request == null) {
      return;
    }
    request.setStatus(
        normalizeStatus(request.getStatus(), "public shop search only supports active status"));
  }

  private static Integer normalizeStatus(Integer status, String errorMessage) {
    if (status == null) {
      return 1;
    }
    if (!Integer.valueOf(1).equals(status)) {
      throw new BizException(ResultCode.BAD_REQUEST, errorMessage);
    }
    return 1;
  }
}
