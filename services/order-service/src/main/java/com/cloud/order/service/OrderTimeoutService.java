package com.cloud.order.service;

import java.util.List;

public interface OrderTimeoutService {

  int checkAndHandleTimeoutOrders();

  List<Long> getTimeoutSubOrderIds(Integer timeoutMinutes);

  boolean cancelTimeoutOrder(Long subOrderId);

  int batchCancelTimeoutOrders(List<Long> subOrderIds);

  Integer getTimeoutConfig();

  boolean updateTimeoutConfig(Integer timeoutMinutes);
}
