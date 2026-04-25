package com.cloud.order.service.impl;

import com.cloud.api.user.UserDubboApi;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.security.SecurityPermissionUtils;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.enums.OrderAction;
import com.cloud.order.service.OrderBatchService;
import com.cloud.order.service.OrderQueryService;
import com.cloud.order.service.OrderService;
import com.cloud.order.service.OrderShippingService;
import com.cloud.order.service.support.PaymentOrderRemoteService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderBatchServiceImpl implements OrderBatchService {

  private static final Set<String> CANCELLABLE_SUB_STATUSES = Set.of("CREATED", "STOCK_RESERVED");
  private static final String PAYMENT_STATUS_PAID = "PAID";

  private final OrderService orderService;
  private final OrderQueryService orderQueryService;
  private final OrderShippingService orderShippingService;
  private final PaymentOrderRemoteService paymentOrderRemoteService;

  @org.apache.dubbo.config.annotation.DubboReference(check = false, timeout = 5000, retries = 0)
  private UserDubboApi userDubboApi;

  @Override
  public boolean applyOrderAction(
      Long orderId,
      Authentication authentication,
      OrderAction action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason) {
    validateSupportedAction(action);
    if (orderId == null) {
      return false;
    }
    OrderMain main = orderQueryService.requireAccessibleMainOrder(orderId, authentication);
    List<OrderSub> subs = orderService.listSubOrders(main.getId());
    applySubOrderAction(main, subs, action, authentication, shippingCompany, trackingNumber);
    if (action == OrderAction.CANCEL) {
      orderQueryService.updateCancelReason(main.getId(), cancelReason);
    }
    return true;
  }

  @Override
  public int batchApply(
      List<Long> orderIds,
      Authentication authentication,
      OrderAction action,
      String shippingCompany,
      String trackingNumber,
      String cancelReason) {
    validateSupportedAction(action);
    if (orderIds == null || orderIds.isEmpty()) {
      return 0;
    }
    int success = 0;
    for (Long orderId : orderIds) {
      if (orderId == null) {
        continue;
      }
      if (applyOrderAction(
          orderId, authentication, action, shippingCompany, trackingNumber, cancelReason)) {
        success += 1;
      }
    }
    return success;
  }

  private void applySubOrderAction(
      OrderMain mainOrder,
      List<OrderSub> subs,
      OrderAction action,
      Authentication authentication,
      String shippingCompany,
      String trackingNumber) {
    if (subs == null || subs.isEmpty()) {
      throw new BizException("sub orders not found");
    }
    if (action == OrderAction.SHIP) {
      requireShipOperator(authentication);
    } else if (action == OrderAction.DONE) {
      requireCompleteOperator(authentication);
    }
    Long currentMerchantId =
        SecurityPermissionUtils.isMerchant(authentication)
            ? requireCurrentMerchantId(authentication)
            : null;
    List<OrderSub> targetSubs = resolveTargetSubs(subs, currentMerchantId);
    if (action == OrderAction.CANCEL) {
      validateCancelableSubOrders(mainOrder, targetSubs);
    }
    for (OrderSub sub : targetSubs) {
      if (sub == null || sub.getId() == null) {
        continue;
      }
      if (action == OrderAction.SHIP) {
        orderShippingService.ship(sub.getId(), shippingCompany, trackingNumber);
      } else {
        orderService.advanceSubOrderStatus(sub.getId(), action);
      }
    }
  }

  private void validateSupportedAction(OrderAction action) {
    if (action == OrderAction.PAY) {
      throw new BizException(
          ResultCode.BAD_REQUEST,
          "direct pay actions are disabled; wait for verified payment confirmation");
    }
  }

  private List<OrderSub> resolveTargetSubs(List<OrderSub> subs, Long currentMerchantId) {
    return subs.stream()
        .filter(sub -> sub != null && sub.getId() != null)
        .filter(
            sub ->
                currentMerchantId == null || Objects.equals(sub.getMerchantId(), currentMerchantId))
        .toList();
  }

  private void validateCancelableSubOrders(OrderMain mainOrder, List<OrderSub> targetSubs) {
    if (targetSubs == null || targetSubs.isEmpty()) {
      throw new BizException("sub orders not found");
    }
    for (OrderSub sub : targetSubs) {
      if (sub == null) {
        continue;
      }
      if (!CANCELLABLE_SUB_STATUSES.contains(sub.getOrderStatus())) {
        throw new BizException(
            ResultCode.BAD_REQUEST,
            "cancel is only allowed before payment: " + sub.getSubOrderNo());
      }
      ensureLatestPaymentNotPaid(mainOrder, sub);
    }
  }

  private void ensureLatestPaymentNotPaid(OrderMain mainOrder, OrderSub subOrder) {
    if (mainOrder == null
        || mainOrder.getMainOrderNo() == null
        || mainOrder.getMainOrderNo().isBlank()
        || subOrder == null
        || subOrder.getSubOrderNo() == null
        || subOrder.getSubOrderNo().isBlank()) {
      return;
    }
    PaymentOrderVO paymentOrder =
        paymentOrderRemoteService.getPaymentOrderByOrderNo(
            mainOrder.getMainOrderNo(), subOrder.getSubOrderNo());
    if (paymentOrder != null && PAYMENT_STATUS_PAID.equals(paymentOrder.getStatus())) {
      throw new BizException(
          ResultCode.BAD_REQUEST,
          "cancel is not allowed after payment confirmation: " + subOrder.getSubOrderNo());
    }
  }

  private void requireShipOperator(Authentication authentication) {
    if (SecurityPermissionUtils.isAdmin(authentication)
        || SecurityPermissionUtils.isMerchant(authentication)) {
      return;
    }
    throw new BizException(ResultCode.FORBIDDEN, "shipping requires merchant or admin privileges");
  }

  private void requireCompleteOperator(Authentication authentication) {
    if (!SecurityPermissionUtils.isMerchant(authentication)) {
      return;
    }
    throw new BizException(
        ResultCode.FORBIDDEN, "order completion requires the order owner or admin privileges");
  }

  private Long requireCurrentMerchantId(Authentication authentication) {
    Long currentUserId = requireCurrentUserId(authentication);
    Long currentMerchantId = userDubboApi.findMerchantIdByOwnerUserId(currentUserId);
    if (currentMerchantId == null) {
      throw new BizException("current merchant not found");
    }
    return currentMerchantId;
  }

  private Long requireCurrentUserId(Authentication authentication) {
    String userId = SecurityPermissionUtils.getCurrentUserId(authentication);
    if (userId == null || userId.isBlank()) {
      throw new BizException("current user not found in token");
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException ex) {
      throw new BizException("invalid user_id in token");
    }
  }
}
