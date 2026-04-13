package com.cloud.payment.service.support;

import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.exception.BizException;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PaymentCallbackVerifier {

  private final AlipayConfig alipayConfig;

  public PaymentCallbackVerificationResult verify(
      PaymentOrderEntity order, PaymentCallbackCommandDTO command, PaymentCallbackContext context) {
    if (order == null) {
      throw new BizException("payment order not found");
    }
    if (command == null) {
      throw new BizException("payment callback command is required");
    }
    if (!StringUtils.hasText(command.getPaymentNo())) {
      throw new BizException("payment callback paymentNo is required");
    }
    if (!StringUtils.hasText(command.getCallbackNo())) {
      throw new BizException("payment callback callbackNo is required");
    }
    if (!StringUtils.hasText(command.getIdempotencyKey())) {
      throw new BizException("payment callback idempotencyKey is required");
    }

    String normalizedStatus = normalizeCallbackStatus(command.getCallbackStatus());
    String provider = resolveProvider(order, context);
    String verifiedAppId = resolveVerifiedAppId(order, context, provider);
    String verifiedSellerId = resolveVerifiedSellerId(order, context, provider);
    String providerTxnNo = resolveProviderTxnNo(order, command);
    BigDecimal amount = resolveAmount(command);

    validateProvider(order, provider);
    validateVerifiedAppId(order, verifiedAppId);
    validateVerifiedSellerId(order, verifiedSellerId);
    validateAmount(order, amount);
    validateProviderTransactionNo(order, providerTxnNo);

    return new PaymentCallbackVerificationResult(
        normalizedStatus,
        provider,
        context == null ? command.getCallbackStatus() : context.providerEventType(),
        verifiedAppId,
        verifiedSellerId,
        providerTxnNo,
        amount,
        command.getPayload(),
        context == null ? null : context.rawPayloadHash());
  }

  private String normalizeCallbackStatus(String callbackStatus) {
    if (!StringUtils.hasText(callbackStatus)) {
      throw new BizException("payment callback status is required");
    }
    String normalized = callbackStatus.trim().toUpperCase();
    if ("SUCCESS".equals(normalized) || "FAIL".equals(normalized)) {
      return normalized;
    }
    throw new BizException("unsupported payment callback status: " + callbackStatus);
  }

  private String resolveProvider(PaymentOrderEntity order, PaymentCallbackContext context) {
    if (context != null && StringUtils.hasText(context.provider())) {
      return context.provider().trim().toUpperCase();
    }
    if (StringUtils.hasText(order.getProvider())) {
      return order.getProvider().trim().toUpperCase();
    }
    if (StringUtils.hasText(order.getChannel())) {
      return order.getChannel().trim().toUpperCase();
    }
    throw new BizException("payment callback provider is required");
  }

  private String resolveVerifiedAppId(
      PaymentOrderEntity order, PaymentCallbackContext context, String provider) {
    if (context != null && StringUtils.hasText(context.verifiedAppId())) {
      return context.verifiedAppId().trim();
    }
    if (StringUtils.hasText(order.getProviderAppId())) {
      return order.getProviderAppId().trim();
    }
    if ("ALIPAY".equalsIgnoreCase(provider) && StringUtils.hasText(alipayConfig.getAppId())) {
      return alipayConfig.getAppId().trim();
    }
    throw new BizException("payment callback app id is required");
  }

  private String resolveVerifiedSellerId(
      PaymentOrderEntity order, PaymentCallbackContext context, String provider) {
    if (context != null && StringUtils.hasText(context.verifiedSellerId())) {
      return context.verifiedSellerId().trim();
    }
    if (StringUtils.hasText(order.getProviderMerchantId())) {
      return order.getProviderMerchantId().trim();
    }
    if ("ALIPAY".equalsIgnoreCase(provider)
        && StringUtils.hasText(alipayConfig.getMerchantId())) {
      return alipayConfig.getMerchantId().trim();
    }
    throw new BizException("payment callback seller id is required");
  }

  private String resolveProviderTxnNo(PaymentOrderEntity order, PaymentCallbackCommandDTO command) {
    if (StringUtils.hasText(command.getProviderTxnNo())) {
      return command.getProviderTxnNo().trim();
    }
    if (StringUtils.hasText(order.getProviderTxnNo())) {
      return order.getProviderTxnNo().trim();
    }
    throw new BizException("payment callback provider transaction is required");
  }

  private BigDecimal resolveAmount(PaymentCallbackCommandDTO command) {
    if (command.getAmount() == null) {
      throw new BizException("payment callback amount is required");
    }
    return command.getAmount();
  }

  private void validateProvider(PaymentOrderEntity order, String provider) {
    if (StringUtils.hasText(order.getProvider())
        && !order.getProvider().equalsIgnoreCase(provider)) {
      throw new BizException("payment callback provider does not match order");
    }
  }

  private void validateVerifiedAppId(PaymentOrderEntity order, String verifiedAppId) {
    if (StringUtils.hasText(order.getProviderAppId())
        && !order.getProviderAppId().equals(verifiedAppId)) {
      throw new BizException("payment callback app id does not match order");
    }
  }

  private void validateVerifiedSellerId(PaymentOrderEntity order, String verifiedSellerId) {
    if (StringUtils.hasText(order.getProviderMerchantId())
        && !order.getProviderMerchantId().equals(verifiedSellerId)) {
      throw new BizException("payment callback seller id does not match order");
    }
  }

  private void validateAmount(PaymentOrderEntity order, BigDecimal amount) {
    if (order.getAmount() != null && order.getAmount().compareTo(amount) != 0) {
      throw new BizException("payment callback amount does not match order amount");
    }
  }

  private void validateProviderTransactionNo(PaymentOrderEntity order, String providerTxnNo) {
    if (StringUtils.hasText(order.getProviderTxnNo())
        && !order.getProviderTxnNo().equals(providerTxnNo)) {
      throw new BizException("payment callback provider transaction does not match order");
    }
  }
}
