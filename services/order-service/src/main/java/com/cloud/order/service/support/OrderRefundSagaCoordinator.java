package com.cloud.order.service.support;

import com.cloud.common.exception.BizException;
import com.cloud.order.entity.AfterSale;
import org.springframework.stereotype.Component;

@Component
public class OrderRefundSagaCoordinator {

  public void startRefundSaga(AfterSale afterSale, String remark) {
    throw new BizException("refund saga is disabled");
  }

  public static String buildRefundNo(String afterSaleNo) {
    if (afterSaleNo == null || afterSaleNo.isBlank()) {
      throw new BizException("after sale number is required");
    }
    return "RF" + afterSaleNo;
  }
}
