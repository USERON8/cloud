package com.cloud.order.service;

import java.util.List;
import org.springframework.security.core.Authentication;

public interface OrderBatchService {

  boolean applyOrderAction(
      Long orderId,
      Authentication authentication,
      String action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason);

  int batchApply(
      List<Long> orderIds,
      Authentication authentication,
      String action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason);
}
