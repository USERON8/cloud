package com.cloud.order.service;

import com.cloud.order.enums.OrderAction;
import java.util.List;
import org.springframework.security.core.Authentication;

public interface OrderBatchService {

  boolean applyOrderAction(
      Long orderId,
      Authentication authentication,
      OrderAction action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason);

  int batchApply(
      List<Long> orderIds,
      Authentication authentication,
      OrderAction action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason);
}
