package com.cloud.order.service.impl;

import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderTimeoutService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimeoutServiceImpl implements OrderTimeoutService {

  private static final Set<String> CANCELLABLE_TIMEOUT_STATUSES =
      Set.of("CREATED", "STOCK_RESERVED");
  private static final String PAYMENT_STATUS_PAID = "PAID";

  private final OrderSubMapper orderSubMapper;
  private final OrderMainMapper orderMainMapper;
  private final OrderService orderService;
  private final PaymentOrderRemoteService paymentOrderRemoteService;

  @Value("${order.timeout.minutes:30}")
  private Integer timeoutMinutes;

  @Value("${order.timeout.batch-size:200}")
  private Integer timeoutBatchSize;

  @Override
  public int checkAndHandleTimeoutOrders() {
    List<Long> timeoutSubOrderIds = getTimeoutSubOrderIds(timeoutMinutes);
    if (timeoutSubOrderIds.isEmpty()) {
      return 0;
    }
    return batchCancelTimeoutOrders(timeoutSubOrderIds);
  }

  @Override
  public List<Long> getTimeoutSubOrderIds(Integer timeoutMinutes) {
    int effectiveTimeout =
        (timeoutMinutes == null || timeoutMinutes <= 0) ? this.timeoutMinutes : timeoutMinutes;
    int effectiveBatchSize =
        (timeoutBatchSize == null || timeoutBatchSize <= 0) ? 200 : timeoutBatchSize;

    LocalDateTime timeoutPoint = LocalDateTime.now().minusMinutes(effectiveTimeout);
    return orderSubMapper.listTimeoutSubOrderIds(
        List.of("CREATED", "STOCK_RESERVED"), timeoutPoint, effectiveBatchSize);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public boolean cancelTimeoutOrder(Long subOrderId) {
    if (subOrderId == null) {
      return false;
    }
    OrderSub currentSubOrder = orderSubMapper.selectById(subOrderId);
    if (currentSubOrder == null || Integer.valueOf(1).equals(currentSubOrder.getDeleted())) {
      return false;
    }
    if (!CANCELLABLE_TIMEOUT_STATUSES.contains(currentSubOrder.getOrderStatus())) {
      return false;
    }
    OrderMain mainOrder = orderMainMapper.selectById(currentSubOrder.getMainOrderId());
    if (hasCompletedRemotePayment(mainOrder, currentSubOrder)) {
      log.info(
          "Skip timeout cancel because payment is already confirmed: subOrderId={}, subOrderNo={}",
          subOrderId,
          currentSubOrder.getSubOrderNo());
      return false;
    }
    OrderSub updated = orderService.advanceSubOrderStatus(subOrderId, OrderAction.CANCEL);
    if (updated == null) {
      return false;
    }
    refreshMainOrderStatusIfAllSubsClosed(updated.getMainOrderId());
    return true;
  }

  private boolean hasCompletedRemotePayment(OrderMain mainOrder, OrderSub subOrder) {
    if (mainOrder == null
        || Integer.valueOf(1).equals(mainOrder.getDeleted())
        || mainOrder.getMainOrderNo() == null
        || mainOrder.getMainOrderNo().isBlank()
        || subOrder == null
        || subOrder.getSubOrderNo() == null
        || subOrder.getSubOrderNo().isBlank()) {
      return false;
    }
    PaymentOrderVO paymentOrder =
        paymentOrderRemoteService.getPaymentOrderByOrderNo(
            mainOrder.getMainOrderNo(), subOrder.getSubOrderNo());
    return paymentOrder != null && PAYMENT_STATUS_PAID.equals(paymentOrder.getStatus());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public int batchCancelTimeoutOrders(List<Long> subOrderIds) {
    if (subOrderIds == null || subOrderIds.isEmpty()) {
      return 0;
    }

    int successCount = 0;
    for (Long subOrderId : subOrderIds) {
      try {
        if (cancelTimeoutOrder(subOrderId)) {
          successCount++;
        }
      } catch (Exception e) {
        log.warn("Skip timeout order cancel failure: subOrderId={}", subOrderId);
      }
    }
    return successCount;
  }

  @Override
  public Integer getTimeoutConfig() {
    return timeoutMinutes;
  }

  @Override
  public boolean updateTimeoutConfig(Integer timeoutMinutes) {
    if (timeoutMinutes == null || timeoutMinutes <= 0) {
      throw new BizException("timeoutMinutes must be greater than 0");
    }
    this.timeoutMinutes = timeoutMinutes;
    return true;
  }

  private void refreshMainOrderStatusIfAllSubsClosed(Long mainOrderId) {
    if (mainOrderId == null) {
      return;
    }
    long remainingCount =
        orderSubMapper.countActiveByMainOrderIdAndStatuses(
            mainOrderId, List.of("CREATED", "STOCK_RESERVED", "PAID", "SHIPPED"));
    if (remainingCount > 0) {
      return;
    }
    OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
    if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
      return;
    }
    mainOrder.setOrderStatus("CANCELLED");
    mainOrder.setCancelledAt(LocalDateTime.now());
    orderMainMapper.updateById(mainOrder);
  }
}
