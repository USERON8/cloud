package com.cloud.order.service.support;

import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.exception.BizException;
import com.cloud.order.entity.AfterSale;
import com.cloud.order.entity.OrderMain;
import com.cloud.order.entity.OrderSub;
import com.cloud.order.mapper.AfterSaleMapper;
import com.cloud.order.mapper.OrderMainMapper;
import com.cloud.order.mapper.OrderSubMapper;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderRefundSagaCoordinator {

  private static final String STATUS_REFUNDING = "REFUNDING";
  private static final String STATUS_REFUNDED = "REFUNDED";
  private static final String PAYMENT_STATUS_PAID = "PAID";
  private static final String REFUND_IDEMPOTENCY_PREFIX = "after-sale-refund:";

  private final AfterSaleMapper afterSaleMapper;
  private final OrderMainMapper orderMainMapper;
  private final OrderSubMapper orderSubMapper;
  private final PaymentOrderRemoteService paymentOrderRemoteService;
  private final OrderAggregateCacheService orderAggregateCacheService;

  @Transactional(rollbackFor = Exception.class)
  public void startRefundSaga(AfterSale afterSale, String remark) {
    AfterSale current = requireRefundableAfterSale(afterSale);
    if (STATUS_REFUNDED.equals(current.getStatus())) {
      return;
    }
    if (STATUS_REFUNDING.equals(current.getStatus())) {
      return;
    }

    OrderMain mainOrder = requireMainOrder(current.getMainOrderId());
    OrderSub subOrder = requireSubOrder(current.getSubOrderId(), mainOrder.getId());
    PaymentOrderVO paymentOrder =
        paymentOrderRemoteService.getPaymentOrderByOrderNo(
            mainOrder.getMainOrderNo(), subOrder.getSubOrderNo());
    if (paymentOrder == null || !PAYMENT_STATUS_PAID.equals(paymentOrder.getStatus())) {
      throw new BizException("paid payment order is required before refund");
    }

    BigDecimal refundAmount = resolveRefundAmount(current, subOrder, paymentOrder);
    current.setStatus(STATUS_REFUNDING);
    current.setApprovedAmount(refundAmount);
    if (current.getRefundChannel() == null || current.getRefundChannel().isBlank()) {
      current.setRefundChannel(paymentOrder.getChannel());
    }
    if ((current.getDescription() == null || current.getDescription().isBlank())
        && remark != null
        && !remark.isBlank()) {
      current.setDescription(remark.trim());
    }
    afterSaleMapper.updateById(current);

    subOrder.setAfterSaleStatus(STATUS_REFUNDING);
    orderSubMapper.updateById(subOrder);
    orderAggregateCacheService.evict(mainOrder.getId());

    PaymentRefundCommandDTO command = new PaymentRefundCommandDTO();
    command.setRefundNo(buildRefundNo(current.getAfterSaleNo()));
    command.setPaymentNo(paymentOrder.getPaymentNo());
    command.setAfterSaleNo(current.getAfterSaleNo());
    command.setRefundAmount(refundAmount);
    command.setReason(resolveRefundReason(current, remark));
    command.setIdempotencyKey(REFUND_IDEMPOTENCY_PREFIX + current.getAfterSaleNo());

    Long refundId = paymentOrderRemoteService.createRefund(command);
    if (refundId == null) {
      throw new BizException("refund request failed");
    }
  }

  public static String buildRefundNo(String afterSaleNo) {
    if (afterSaleNo == null || afterSaleNo.isBlank()) {
      throw new BizException("after sale number is required");
    }
    return "RF" + afterSaleNo;
  }

  private AfterSale requireRefundableAfterSale(AfterSale source) {
    if (source == null || source.getId() == null) {
      throw new BizException("after sale is required");
    }
    AfterSale current = afterSaleMapper.selectById(source.getId());
    if (current == null || Integer.valueOf(1).equals(current.getDeleted())) {
      throw new BizException("after sale not found");
    }
    if (current.getAfterSaleNo() == null || current.getAfterSaleNo().isBlank()) {
      throw new BizException("after sale number is required");
    }
    if (current.getSubOrderId() == null || current.getMainOrderId() == null) {
      throw new BizException("after sale order reference is required");
    }
    return current;
  }

  private OrderMain requireMainOrder(Long mainOrderId) {
    OrderMain mainOrder = orderMainMapper.selectById(mainOrderId);
    if (mainOrder == null || Integer.valueOf(1).equals(mainOrder.getDeleted())) {
      throw new BizException("main order not found for refund");
    }
    if (mainOrder.getMainOrderNo() == null || mainOrder.getMainOrderNo().isBlank()) {
      throw new BizException("main order number is required for refund");
    }
    return mainOrder;
  }

  private OrderSub requireSubOrder(Long subOrderId, Long mainOrderId) {
    OrderSub subOrder = orderSubMapper.selectById(subOrderId);
    if (subOrder == null || Integer.valueOf(1).equals(subOrder.getDeleted())) {
      throw new BizException("sub order not found for refund");
    }
    if (!mainOrderId.equals(subOrder.getMainOrderId())) {
      throw new BizException("after sale sub order does not belong to main order");
    }
    if (subOrder.getSubOrderNo() == null || subOrder.getSubOrderNo().isBlank()) {
      throw new BizException("sub order number is required for refund");
    }
    return subOrder;
  }

  private BigDecimal resolveRefundAmount(
      AfterSale afterSale, OrderSub subOrder, PaymentOrderVO paymentOrder) {
    BigDecimal amount = firstPositive(afterSale.getApprovedAmount(), afterSale.getApplyAmount());
    if (amount == null) {
      throw new BizException("refund amount is required");
    }
    BigDecimal payableAmount = firstPositive(subOrder.getPayableAmount(), paymentOrder.getAmount());
    if (payableAmount != null && amount.compareTo(payableAmount) > 0) {
      throw new BizException("refund amount cannot exceed sub order payable amount");
    }
    if (paymentOrder.getAmount() != null && amount.compareTo(paymentOrder.getAmount()) > 0) {
      throw new BizException("refund amount cannot exceed paid amount");
    }
    return amount;
  }

  private BigDecimal firstPositive(BigDecimal first, BigDecimal second) {
    if (first != null && first.compareTo(BigDecimal.ZERO) > 0) {
      return first;
    }
    if (second != null && second.compareTo(BigDecimal.ZERO) > 0) {
      return second;
    }
    return null;
  }

  private String resolveRefundReason(AfterSale afterSale, String remark) {
    if (remark != null && !remark.isBlank()) {
      return remark.trim();
    }
    if (afterSale.getReason() != null && !afterSale.getReason().isBlank()) {
      return afterSale.getReason().trim();
    }
    return "after-sale refund";
  }
}
