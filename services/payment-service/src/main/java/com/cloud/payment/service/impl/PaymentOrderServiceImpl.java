package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentCheckoutSessionVO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.enums.ResultCode;
import com.cloud.common.exception.BizException;
import com.cloud.common.exception.SystemException;
import com.cloud.common.messaging.event.PaymentSuccessEvent;
import com.cloud.common.metrics.TradeMetrics;
import com.cloud.common.util.HtmlEscapeUtils;
import com.cloud.payment.config.AlipayConfig;
import com.cloud.payment.converter.PaymentOrderConverter;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.messaging.PaymentMessageProducer;
import com.cloud.payment.module.entity.PaymentCallbackLogEntity;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import com.cloud.payment.service.support.OrderStatusRemoteService;
import com.cloud.payment.service.support.PaymentCallbackContext;
import com.cloud.payment.service.support.PaymentCallbackVerificationResult;
import com.cloud.payment.service.support.PaymentCallbackVerifier;
import com.cloud.payment.service.support.PaymentOrderStateSupport;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import com.cloud.payment.service.support.PaymentStateMachine;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

  private static final Set<String> COUNTED_REFUND_STATUSES = Set.of("REFUNDING", "REFUNDED");
  private static final Set<String> PAYABLE_ORDER_STATUSES =
      Set.of("CREATED", "STOCK_RESERVED", "PAID");
  private static final String CHECKOUT_PATH_PREFIX = "/api/payment-checkouts/";

  private final PaymentOrderMapper paymentOrderMapper;
  private final PaymentRefundMapper paymentRefundMapper;
  private final PaymentCallbackLogMapper paymentCallbackLogMapper;
  private final PaymentOrderConverter paymentOrderConverter;
  private final PaymentCompensationService paymentCompensationService;
  private final AlipayConfig alipayConfig;
  private final PaymentMessageProducer paymentMessageProducer;
  private final OrderStatusRemoteService orderStatusRemoteService;
  private final PaymentCallbackVerifier paymentCallbackVerifier;
  private final PaymentStateMachine paymentStateMachine;
  private final PaymentOrderStateSupport paymentOrderStateSupport;
  private final PaymentSecurityCacheService paymentSecurityCacheService;
  private final List<PaymentProviderGateway> providerGateways;
  private final TradeMetrics tradeMetrics;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createPaymentOrder(PaymentOrderCommandDTO command) {
    ensureOrderReadyForPayment(command);

    if (!paymentSecurityCacheService.allowRateLimit(command.getUserId())) {
      throw new BizException("payment rate limit exceeded");
    }

    String orderKey = buildOrderKey(command);
    Long cachedResult = paymentSecurityCacheService.getCachedResult(orderKey);
    if (cachedResult != null) {
      PaymentOrderEntity cachedOrder = paymentOrderMapper.selectById(cachedResult);
      if (canReusePaymentOrder(cachedOrder)) {
        return cachedResult;
      }
      paymentSecurityCacheService.clearOrderState(orderKey);
    }

    PaymentOrderEntity existingByOrder =
        findPaymentOrderEntityByOrderNo(command.getMainOrderNo(), command.getSubOrderNo());
    if (canReusePaymentOrder(existingByOrder)) {
      paymentSecurityCacheService.markIdempotent(orderKey, command.getIdempotencyKey());
      paymentSecurityCacheService.cacheResult(orderKey, existingByOrder.getId());
      return existingByOrder.getId();
    }
    if (isFailedOrder(existingByOrder)) {
      paymentSecurityCacheService.clearOrderState(orderKey);
    }

    PaymentOrderEntity existing =
        findPaymentOrderEntityByIdempotencyKey(command.getIdempotencyKey());
    if (existing != null) {
      paymentSecurityCacheService.markIdempotent(orderKey, command.getIdempotencyKey());
      paymentSecurityCacheService.cacheResult(orderKey, existing.getId());
      return existing.getId();
    }

    boolean acquired =
        paymentSecurityCacheService.tryAcquireIdempotent(orderKey, command.getIdempotencyKey());
    if (!acquired) {
      PaymentOrderEntity duplicated =
          findPaymentOrderEntityByIdempotencyKey(command.getIdempotencyKey());
      if (duplicated != null) {
        paymentSecurityCacheService.cacheResult(orderKey, duplicated.getId());
        return duplicated.getId();
      }
      throw new BizException("duplicate payment request");
    }

    PaymentOrderEntity entity = paymentOrderConverter.toEntity(command);
    applyProviderFields(entity, command.getChannel(), buildOrderKey(command));
    entity.setStatus("CREATED");
    paymentCompensationService.initializePaymentOrderCompensation(entity);
    paymentOrderMapper.insert(entity);
    paymentSecurityCacheService.markIdempotent(orderKey, command.getIdempotencyKey());
    paymentSecurityCacheService.cacheResult(orderKey, entity.getId());
    return entity.getId();
  }

  private void ensureOrderReadyForPayment(PaymentOrderCommandDTO command) {
    if (command == null) {
      throw new BizException("payment command is required");
    }
    var orderStatus =
        orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo());
    if (orderStatus == null) {
      throw new BizException("order status not found for payment");
    }
    if (orderStatus.getUserId() != null && !orderStatus.getUserId().equals(command.getUserId())) {
      throw new BizException("payment user does not match order owner");
    }
    if (orderStatus.getPayableAmount() != null
        && command.getAmount() != null
        && orderStatus.getPayableAmount().compareTo(command.getAmount()) != 0) {
      throw new BizException("payment amount does not match order payable amount");
    }
    if (!PAYABLE_ORDER_STATUSES.contains(orderStatus.getOrderStatus())) {
      throw new BizException("order is not ready for payment: " + orderStatus.getOrderStatus());
    }
  }

  @Override
  public PaymentOrderVO getPaymentOrderByNo(String paymentNo) {
    PaymentOrderEntity entity = findPaymentOrderEntityByNo(paymentNo);
    return entity == null ? null : toOrderVO(entity);
  }

  @Override
  public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
    PaymentOrderEntity entity = findPaymentOrderEntityByOrderNo(mainOrderNo, subOrderNo);
    return entity == null ? null : toOrderVO(entity);
  }

  @Override
  public PaymentCheckoutSessionVO createCheckoutSession(String paymentNo) {
    PaymentOrderEntity order = findPaymentOrderEntityByNo(paymentNo);
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (paymentOrderStateSupport.isTerminalStatus(order.getStatus())) {
      throw new BizException("payment order is already finalized");
    }
    if (!PaymentOrderStateSupport.ORDER_STATUS_CREATED.equals(order.getStatus())) {
      throw new BizException("payment order is not eligible for checkout: " + order.getStatus());
    }

    String ticket =
        paymentSecurityCacheService.createCheckoutTicket(order.getPaymentNo(), order.getUserId());
    PaymentCheckoutSessionVO session = new PaymentCheckoutSessionVO();
    session.setPaymentNo(order.getPaymentNo());
    session.setCheckoutPath(CHECKOUT_PATH_PREFIX + ticket);
    session.setExpiresInSeconds(paymentSecurityCacheService.getCheckoutTicketTtlSeconds());
    return session;
  }

  @Override
  public String renderCheckoutPage(String ticket) {
    PaymentSecurityCacheService.CheckoutTicket checkoutTicket =
        paymentSecurityCacheService.getCheckoutTicket(ticket);
    if (checkoutTicket == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment checkout session expired");
    }
    PaymentOrderEntity order = findPaymentOrderEntityByNo(checkoutTicket.paymentNo());
    if (order == null) {
      throw new BizException(ResultCode.NOT_FOUND, "payment order not found");
    }
    if (!Objects.equals(order.getUserId(), checkoutTicket.userId())) {
      throw new BizException(ResultCode.FORBIDDEN, "payment checkout session is invalid");
    }
    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(order.getStatus())) {
      return buildStatusPage("Payment completed", "This payment order has already been completed.");
    }
    if (PaymentOrderStateSupport.ORDER_STATUS_FAILED.equals(order.getStatus())) {
      return buildStatusPage("Payment unavailable", "This payment order is no longer available.");
    }
    PaymentProviderGateway gateway = resolveGateway(order.getChannel());
    if (gateway == null) {
      throw new BizException("unsupported payment channel: " + order.getChannel());
    }
    return gateway.buildCheckoutPage(order);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean handlePaymentCallback(
      PaymentCallbackCommandDTO command, PaymentCallbackContext context) {
    try {
      if (findCallbackLogByCallbackNo(command.getCallbackNo()) != null
          || findCallbackLogByIdempotencyKey(command.getIdempotencyKey()) != null) {
        tradeMetrics.incrementPaymentCallback("duplicate");
        return true;
      }

      PaymentOrderEntity order = findPaymentOrderEntityByNo(command.getPaymentNo());
      PaymentCallbackVerificationResult verificationResult =
          paymentCallbackVerifier.verify(order, command, context);
      String previousStatus = order.getStatus();

      PaymentCallbackLogEntity log = new PaymentCallbackLogEntity();
      log.setPaymentNo(command.getPaymentNo());
      log.setProvider(verificationResult.provider());
      log.setCallbackNo(command.getCallbackNo());
      log.setCallbackStatus(verificationResult.normalizedStatus());
      log.setProviderEventType(verificationResult.providerEventType());
      log.setProviderTxnNo(verificationResult.providerTxnNo());
      log.setVerifiedAppId(verificationResult.verifiedAppId());
      log.setVerifiedSellerId(verificationResult.verifiedSellerId());
      log.setPayload(verificationResult.payload());
      log.setRawPayloadHash(resolveRawPayloadHash(verificationResult));
      log.setIdempotencyKey(command.getIdempotencyKey());
      paymentCallbackLogMapper.insert(log);

      if (paymentOrderStateSupport.isTerminalStatus(previousStatus)) {
        tradeMetrics.incrementPaymentCallback("ignored");
        return true;
      }

      paymentStateMachine.apply(order, verificationResult, LocalDateTime.now());
      order.setNextPollAt(null);
      order.setLastPolledAt(LocalDateTime.now());
      order.setLastPollError(null);
      paymentOrderMapper.updateById(order);
      paymentOrderStateSupport.handlePersistedState(order, previousStatus);
      publishPaymentSuccessIfNeeded(order, previousStatus);
      tradeMetrics.incrementPaymentCallback("success");
      return true;
    } catch (Exception ex) {
      tradeMetrics.incrementPaymentCallback("failed");
      throw ex;
    }
  }

  @Override
  public Boolean handleInternalPaymentCallback(PaymentCallbackCommandDTO command) {
    throw new BizException(
        ResultCode.BAD_REQUEST, "internal payment callbacks cannot update payment state");
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createRefund(PaymentRefundCommandDTO command) {
    if (command == null) {
      throw new BizException("refund command is required");
    }
    PaymentRefundEntity existing =
        paymentRefundMapper.selectOne(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (existing != null) {
      return existing.getId();
    }

    PaymentOrderEntity paymentOrder = findPaymentOrderEntityByNo(command.getPaymentNo());
    if (paymentOrder == null) {
      throw new BizException("payment order not found");
    }
    validateRefundRequest(command, paymentOrder);

    PaymentRefundEntity entity = paymentOrderConverter.toEntity(command);
    applyProviderFields(entity, paymentOrder);
    entity.setStatus("REFUNDING");
    entity.setRetryCount(0);
    entity.setNextRetryAt(LocalDateTime.now());
    paymentRefundMapper.insert(entity);

    paymentCompensationService.submitRefund(paymentOrder, entity);
    return entity.getId();
  }

  private void validateRefundRequest(
      PaymentRefundCommandDTO command, PaymentOrderEntity paymentOrder) {
    if (!PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(paymentOrder.getStatus())) {
      throw new BizException(
          "payment order is not eligible for refund: " + paymentOrder.getStatus());
    }
    if (paymentOrder.getAmount() == null
        || paymentOrder.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException("payment order amount is invalid for refund");
    }
    if (command.getRefundAmount() == null
        || command.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BizException("refund amount must be greater than 0");
    }
    if (command.getRefundAmount().compareTo(paymentOrder.getAmount()) > 0) {
      throw new BizException("refund amount cannot exceed payment amount");
    }

    List<PaymentRefundEntity> paymentRefunds =
        paymentRefundMapper.selectList(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getPaymentNo, paymentOrder.getPaymentNo())
                .eq(PaymentRefundEntity::getDeleted, 0));
    validateAfterSaleOwnership(command, paymentOrder, paymentRefunds);

    BigDecimal accumulatedRefundAmount = BigDecimal.ZERO;
    for (PaymentRefundEntity refund : paymentRefunds) {
      if (refund == null || !COUNTED_REFUND_STATUSES.contains(refund.getStatus())) {
        continue;
      }
      BigDecimal refundAmount = refund.getRefundAmount();
      if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      accumulatedRefundAmount = accumulatedRefundAmount.add(refundAmount);
    }
    if (accumulatedRefundAmount.add(command.getRefundAmount()).compareTo(paymentOrder.getAmount())
        > 0) {
      throw new BizException("refund amount exceeds the remaining paid amount");
    }
  }

  private void validateAfterSaleOwnership(
      PaymentRefundCommandDTO command,
      PaymentOrderEntity paymentOrder,
      List<PaymentRefundEntity> paymentRefunds) {
    List<PaymentRefundEntity> sameAfterSaleRefunds =
        paymentRefundMapper.selectList(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getAfterSaleNo, command.getAfterSaleNo())
                .eq(PaymentRefundEntity::getDeleted, 0));
    for (PaymentRefundEntity refund : sameAfterSaleRefunds) {
      if (refund == null) {
        continue;
      }
      if (!paymentOrder.getPaymentNo().equals(refund.getPaymentNo())) {
        throw new BizException("after-sale refund does not belong to the target payment order");
      }
      if (!command.getRefundNo().equals(refund.getRefundNo())) {
        throw new BizException("after-sale refund already exists for the target payment order");
      }
    }

    for (PaymentRefundEntity refund : paymentRefunds) {
      if (refund == null || !command.getAfterSaleNo().equals(refund.getAfterSaleNo())) {
        continue;
      }
      if (!command.getRefundNo().equals(refund.getRefundNo())) {
        throw new BizException("after-sale refund already exists for the target payment order");
      }
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean cancelRefund(String refundNo, String reason) {
    if (refundNo == null || refundNo.isBlank()) {
      throw new BizException("refund no is required");
    }
    PaymentRefundEntity refund =
        paymentRefundMapper.selectOne(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getRefundNo, refundNo)
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (refund == null) {
      return true;
    }
    String status = refund.getStatus();
    if ("REFUNDED".equals(status)) {
      return false;
    }
    if ("CANCELLED".equals(status)) {
      return true;
    }
    refund.setStatus("CANCELLED");
    if (reason != null && !reason.isBlank()) {
      refund.setLastError(reason);
    }
    refund.setNextRetryAt(null);
    refund.setLastRetryAt(LocalDateTime.now());
    paymentRefundMapper.updateById(refund);
    return true;
  }

  @Override
  public PaymentRefundVO getRefundByNo(String refundNo) {
    PaymentRefundEntity entity =
        paymentRefundMapper.selectOne(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getRefundNo, refundNo)
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
    return entity == null ? null : toRefundVO(entity);
  }

  private PaymentOrderVO toOrderVO(PaymentOrderEntity entity) {
    return paymentOrderConverter.toVO(entity);
  }

  private PaymentRefundVO toRefundVO(PaymentRefundEntity entity) {
    return paymentOrderConverter.toVO(entity);
  }

  private String buildOrderKey(PaymentOrderCommandDTO command) {
    return command.getMainOrderNo() + ":" + command.getSubOrderNo();
  }

  private PaymentOrderEntity findPaymentOrderEntityByNo(String paymentNo) {
    return paymentOrderMapper.selectByPaymentNo(paymentNo);
  }

  private PaymentOrderEntity findPaymentOrderEntityByIdempotencyKey(String idempotencyKey) {
    return paymentOrderMapper.selectByIdempotencyKey(idempotencyKey);
  }

  private PaymentOrderEntity findPaymentOrderEntityByOrderNo(
      String mainOrderNo, String subOrderNo) {
    return paymentOrderMapper.selectLatestByMainOrderNoAndSubOrderNo(mainOrderNo, subOrderNo);
  }

  private boolean canReusePaymentOrder(PaymentOrderEntity order) {
    return order != null && !isFailedOrder(order);
  }

  private boolean isFailedOrder(PaymentOrderEntity order) {
    return order != null && PaymentOrderStateSupport.ORDER_STATUS_FAILED.equals(order.getStatus());
  }

  private PaymentProviderGateway resolveGateway(String channel) {
    for (PaymentProviderGateway gateway : providerGateways) {
      if (gateway.supports(channel)) {
        return gateway;
      }
    }
    return null;
  }

  private String buildStatusPage(String title, String message) {
    return """
        <!DOCTYPE html>
        <html lang="en">
          <head>
            <meta charset="UTF-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>%s</title>
            <style>
              body {
                margin: 0;
                min-height: 100vh;
                display: grid;
                place-items: center;
                background: #f4f7fb;
                color: #1f2a37;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
              }
              main {
                max-width: 420px;
                padding: 24px;
                border-radius: 18px;
                background: #ffffff;
                box-shadow: 0 16px 48px rgba(15, 23, 42, 0.08);
                text-align: center;
              }
              h1 {
                margin: 0 0 12px;
                font-size: 24px;
              }
              p {
                margin: 0;
                line-height: 1.6;
                color: #526072;
              }
            </style>
          </head>
          <body>
            <main>
              <h1>%s</h1>
              <p>%s</p>
            </main>
          </body>
        </html>
        """
        .formatted(
            HtmlEscapeUtils.escape(title),
            HtmlEscapeUtils.escape(title),
            HtmlEscapeUtils.escape(message));
  }

  private PaymentCallbackLogEntity findCallbackLogByCallbackNo(String callbackNo) {
    if (!StringUtils.hasText(callbackNo)) {
      return null;
    }
    return paymentCallbackLogMapper.selectOne(
        new LambdaQueryWrapper<PaymentCallbackLogEntity>()
            .eq(PaymentCallbackLogEntity::getCallbackNo, callbackNo)
            .eq(PaymentCallbackLogEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private PaymentCallbackLogEntity findCallbackLogByIdempotencyKey(String idempotencyKey) {
    if (!StringUtils.hasText(idempotencyKey)) {
      return null;
    }
    return paymentCallbackLogMapper.selectOne(
        new LambdaQueryWrapper<PaymentCallbackLogEntity>()
            .eq(PaymentCallbackLogEntity::getIdempotencyKey, idempotencyKey)
            .eq(PaymentCallbackLogEntity::getDeleted, 0)
            .last("LIMIT 1"));
  }

  private void applyProviderFields(PaymentOrderEntity entity, String channel, String bizOrderKey) {
    String provider = normalizeProvider(channel);
    entity.setProvider(provider);
    entity.setProviderAppId(resolveProviderAppId(provider));
    entity.setProviderMerchantId(resolveProviderMerchantId(provider));
    entity.setBizType(resolveBizType(provider));
    entity.setBizOrderKey(bizOrderKey);
  }

  private void applyProviderFields(PaymentRefundEntity refund, PaymentOrderEntity order) {
    String provider = firstNonBlank(order.getProvider(), order.getChannel());
    refund.setProvider(provider);
    refund.setProviderAppId(resolveProviderAppId(provider, order));
    refund.setProviderMerchantId(resolveProviderMerchantId(provider, order));
  }

  private String resolveProviderAppId(String provider) {
    return resolveProviderAppId(provider, null);
  }

  private String resolveProviderAppId(String provider, PaymentOrderEntity order) {
    if ("ALIPAY".equalsIgnoreCase(provider)) {
      return firstNonBlank(
          alipayConfig.getAppId(), order == null ? null : order.getProviderAppId());
    }
    return order == null ? null : order.getProviderAppId();
  }

  private String resolveProviderMerchantId(String provider) {
    return resolveProviderMerchantId(provider, null);
  }

  private String resolveProviderMerchantId(String provider, PaymentOrderEntity order) {
    if ("ALIPAY".equalsIgnoreCase(provider)) {
      return firstNonBlank(
          alipayConfig.getMerchantId(), order == null ? null : order.getProviderMerchantId());
    }
    return order == null ? null : order.getProviderMerchantId();
  }

  private String resolveBizType(String provider) {
    if (provider == null) {
      return null;
    }
    return provider.equalsIgnoreCase("ALIPAY") ? "SUB_ORDER" : provider;
  }

  private String resolveRawPayloadHash(PaymentCallbackVerificationResult verificationResult) {
    if (verificationResult == null) {
      return null;
    }
    if (StringUtils.hasText(verificationResult.rawPayloadHash())) {
      return verificationResult.rawPayloadHash();
    }
    return hashPayload(verificationResult.payload());
  }

  private String normalizeProvider(String channel) {
    if (!StringUtils.hasText(channel)) {
      return null;
    }
    return channel.trim().toUpperCase();
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private void publishPaymentSuccessIfNeeded(PaymentOrderEntity order, String previousStatus) {
    if (PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(previousStatus)) {
      return;
    }
    if (!PaymentOrderStateSupport.ORDER_STATUS_PAID.equals(order.getStatus())) {
      return;
    }
    PaymentSuccessEvent event =
        PaymentSuccessEvent.builder()
            .paymentId(order.getId())
            .orderNo(order.getMainOrderNo())
            .subOrderNo(order.getSubOrderNo())
            .userId(order.getUserId())
            .amount(order.getAmount())
            .paymentMethod(order.getChannel())
            .transactionNo(order.getProviderTxnNo())
            .build();
    if (!paymentMessageProducer.sendPaymentSuccessEvent(event)) {
      throw new SystemException("failed to enqueue payment success event");
    }
  }

  private String hashPayload(String payload) {
    if (!StringUtils.hasText(payload)) {
      return null;
    }
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hashed.length * 2);
      for (byte b : hashed) {
        builder.append(Character.forDigit((b >>> 4) & 0x0F, 16));
        builder.append(Character.forDigit(b & 0x0F, 16));
      }
      return builder.toString();
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Failed to hash payment callback payload", ex);
    }
  }
}
