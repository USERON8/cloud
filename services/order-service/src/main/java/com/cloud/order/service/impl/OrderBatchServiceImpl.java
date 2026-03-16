package com.cloud.order.service.impl;

import com.cloud.common.exception.BusinessException;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.service.OrderBatchService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderBatchServiceImpl implements OrderBatchService {

  private final OrderService orderService;
  private final OrderQueryService orderQueryService;
  private final OrderShippingService orderShippingService;

  @Override
  public boolean applyOrderAction(
      Long orderId,
      Authentication authentication,
      String action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason) {
    if (orderId == null) {
      return false;
    }
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    applySubOrderAction(subs, action, authentication, shippingCompany, trackingNumber);
    if ("CANCEL".equals(action)) {
      orderQueryService.updateCancelReason(main.getId(), cancelReason);
    }
    return true;
  }

  @Override
  public int batchApply(
      List<Long> orderIds,
      Authentication authentication,
      String action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason) {
    if (orderIds == null || orderIds.isEmpty()) {
      return 0;
    }
    int success = 0;
    for (Long orderId : orderIds) {
      if (orderId == null) {
        continue;
      }
      if (applyOrderAction(
          orderId,
          authentication,
          action,
          shippingCompany,
          trackingNumber,
          cancelReason)) {
        success += 1;
      }
    }
    return success;
  }

  private void applySubOrderAction(
      List<OrderSub> subs,
      String action,
      Authentication authentication,
      String shippingCompany,
      String trackingNumber) {
    if (subs == null || subs.isEmpty()) {
      throw new BusinessException("sub orders not found");
    }
    for (OrderSub sub : subs) {
      if (sub == null || sub.getId() == null) {
        continue;
      }
      if (SecurityPermissionUtils.isMerchant(authentication)
          && !Objects.equals(sub.getMerchantId(), requireCurrentUserId(authentication))) {
        continue;
      }
      if ("SHIP".equals(action)) {
        orderShippingService.ship(sub.getId(), shippingCompany, trackingNumber);
      } else {
        orderService.advanceSubOrderStatus(sub.getId(), action);
      }
    }
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BusinessException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BusinessException("invalid user_id in token");
    }
  }
}
