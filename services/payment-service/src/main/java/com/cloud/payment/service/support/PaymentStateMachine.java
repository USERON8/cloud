package com.cloud.payment.service.support;

import com.cloud.common.exception.BizException;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PaymentStateMachine {

  public void apply(
      PaymentOrderEntity order,
      PaymentCallbackVerificationResult verificationResult,
      LocalDateTime occurredAt) {
    String currentStatus = order.getStatus();
    String targetStatus = normalizeTargetStatus(verificationResult.normalizedStatus());
    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(currentStatus)) {
      throw new BizException("payment order is already paid");
    }
    if (PaymentOrderStateSupport.ORDER_STATUS_FAILED.equals(currentStatus)) {
      throw new BizException("payment order is already failed");
    }
    if (!PaymentOrderStateSupport.ORDER_STATUS_CREATED.equals(currentStatus)) {
      throw new BizException("payment order status does not allow callback transition");
    }

    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(targetStatus)) {
      order.setStatus(PaymentOrderStateSupport.ORDER_STATUS_PAID);
      if (StringUtils.hasText(verificationResult.providerTxnNo())) {
        order.setProviderTxnNo(verificationResult.providerTxnNo());
      }
      if (order.getPaidAt() == null) {
        order.setPaidAt(occurredAt == null ? LocalDateTime.now() : occurredAt);
      }
      return;
    }

    if (PaymentOrderStateSupport.ORDER_STATUS_FAILED.equals(targetStatus)) {
      order.setStatus(PaymentOrderStateSupport.ORDER_STATUS_FAILED);
      return;
    }

    throw new BizException("unsupported payment target status: " + targetStatus);
  }

  private String normalizeTargetStatus(String normalizedStatus) {
    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equalsIgnoreCase(normalizedStatus)) {
      return PaymentOrderStateSupport.ORDER_STATUS_PAID;
    }
    if (PaymentOrderStateSupport.ORDER_STATUS_FAILED.equalsIgnoreCase(normalizedStatus)) {
      return PaymentOrderStateSupport.ORDER_STATUS_FAILED;
    }
    if ("SUCCESS".equalsIgnoreCase(normalizedStatus)) {
      return PaymentOrderStateSupport.ORDER_STATUS_PAID;
    }
    if ("FAIL".equalsIgnoreCase(normalizedStatus)) {
      return PaymentOrderStateSupport.ORDER_STATUS_FAILED;
    }
    throw new BizException("unsupported payment callback status: " + normalizedStatus);
  }
}
