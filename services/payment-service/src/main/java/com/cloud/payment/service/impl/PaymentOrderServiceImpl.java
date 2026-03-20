package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.domain.dto.payment.PaymentCallbackCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentOrderCommandDTO;
import com.cloud.common.domain.dto.payment.PaymentRefundCommandDTO;
import com.cloud.common.domain.vo.payment.PaymentOrderVO;
import com.cloud.common.domain.vo.payment.PaymentRefundVO;
import com.cloud.common.exception.BusinessException;
import com.cloud.payment.mapper.PaymentCallbackLogMapper;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.module.entity.PaymentCallbackLogEntity;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.PaymentOrderService;
import com.cloud.payment.service.support.OrderStatusRemoteService;
import com.cloud.payment.service.support.PaymentOrderStateSupport;
import com.cloud.payment.service.support.PaymentSecurityCacheService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl implements PaymentOrderService {

  private final PaymentOrderMapper paymentOrderMapper;
  private final PaymentRefundMapper paymentRefundMapper;
  private final PaymentCallbackLogMapper paymentCallbackLogMapper;
  private final PaymentCompensationService paymentCompensationService;
  private final OrderStatusRemoteService orderStatusRemoteService;
  private final PaymentOrderStateSupport paymentOrderStateSupport;
  private final PaymentSecurityCacheService paymentSecurityCacheService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createPaymentOrder(PaymentOrderCommandDTO command) {
    ensureOrderReadyForPayment(command);

    if (!paymentSecurityCacheService.allowRateLimit(command.getUserId())) {
      throw new BusinessException("payment rate limit exceeded");
    }

    String orderKey = buildOrderKey(command);
    Long cachedResult = paymentSecurityCacheService.getCachedResult(orderKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    PaymentOrderEntity existing =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (existing != null) {
      paymentSecurityCacheService.markIdempotent(orderKey, command.getIdempotencyKey());
      paymentSecurityCacheService.cacheResult(orderKey, existing.getId());
      return existing.getId();
    }

    boolean acquired =
        paymentSecurityCacheService.tryAcquireIdempotent(orderKey, command.getIdempotencyKey());
    if (!acquired) {
      PaymentOrderEntity duplicated =
          paymentOrderMapper.selectOne(
              new LambdaQueryWrapper<PaymentOrderEntity>()
                  .eq(PaymentOrderEntity::getIdempotencyKey, command.getIdempotencyKey())
                  .eq(PaymentOrderEntity::getDeleted, 0)
                  .last("LIMIT 1"));
      if (duplicated != null) {
        paymentSecurityCacheService.cacheResult(orderKey, duplicated.getId());
        return duplicated.getId();
      }
      throw new BusinessException("duplicate payment request");
    }

    PaymentOrderEntity entity = new PaymentOrderEntity();
    entity.setPaymentNo(command.getPaymentNo());
    entity.setMainOrderNo(command.getMainOrderNo());
    entity.setSubOrderNo(command.getSubOrderNo());
    entity.setUserId(command.getUserId());
    entity.setAmount(command.getAmount());
    entity.setChannel(command.getChannel());
    entity.setStatus("CREATED");
    entity.setIdempotencyKey(command.getIdempotencyKey());
    paymentCompensationService.initializePaymentOrderCompensation(entity);
    paymentOrderMapper.insert(entity);
    paymentSecurityCacheService.markIdempotent(orderKey, command.getIdempotencyKey());
    paymentSecurityCacheService.cacheResult(orderKey, entity.getId());
    return entity.getId();
  }

  private void ensureOrderReadyForPayment(PaymentOrderCommandDTO command) {
    if (command == null) {
      throw new BusinessException("payment command is required");
    }
    var orderStatus =
        orderStatusRemoteService.getSubOrderStatus(
            command.getMainOrderNo(), command.getSubOrderNo());
    if (orderStatus == null) {
      throw new BusinessException("order status not found for payment");
    }
    if (orderStatus.getUserId() != null && !orderStatus.getUserId().equals(command.getUserId())) {
      throw new BusinessException("payment user does not match order owner");
    }
    if (orderStatus.getPayableAmount() != null
        && command.getAmount() != null
        && orderStatus.getPayableAmount().compareTo(command.getAmount()) != 0) {
      throw new BusinessException("payment amount does not match order payable amount");
    }
    if (!"STOCK_RESERVED".equals(orderStatus.getOrderStatus())
        && !"PAID".equals(orderStatus.getOrderStatus())) {
      throw new BusinessException(
          "order is not ready for payment: " + orderStatus.getOrderStatus());
    }
  }

  @Override
  public PaymentOrderVO getPaymentOrderByNo(String paymentNo) {
    PaymentOrderEntity entity =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, paymentNo)
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
    return entity == null ? null : toOrderVO(entity);
  }

  @Override
  public PaymentOrderVO getPaymentOrderByOrderNo(String mainOrderNo, String subOrderNo) {
    PaymentOrderEntity entity =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getMainOrderNo, mainOrderNo)
                .eq(PaymentOrderEntity::getSubOrderNo, subOrderNo)
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
    return entity == null ? null : toOrderVO(entity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean handlePaymentCallback(PaymentCallbackCommandDTO command) {
    if (findCallbackLogByCallbackNo(command.getCallbackNo()) != null
        || findCallbackLogByIdempotencyKey(command.getIdempotencyKey()) != null) {
      return true;
    }

    PaymentOrderEntity order =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, command.getPaymentNo())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (order == null) {
      throw new BusinessException("payment order not found");
    }
    validateCallbackAgainstOrder(order, command);
    String previousStatus = order.getStatus();
    String normalizedStatus = normalizeCallbackStatus(command.getCallbackStatus());

    PaymentCallbackLogEntity log = new PaymentCallbackLogEntity();
    log.setPaymentNo(command.getPaymentNo());
    log.setCallbackNo(command.getCallbackNo());
    log.setCallbackStatus(normalizedStatus);
    log.setProviderTxnNo(command.getProviderTxnNo());
    log.setPayload(command.getPayload());
    log.setIdempotencyKey(command.getIdempotencyKey());
    paymentCallbackLogMapper.insert(log);

    if (paymentOrderStateSupport.isTerminalStatus(previousStatus)) {
      return true;
    }

    if ("SUCCESS".equals(normalizedStatus)) {
      paymentOrderStateSupport.markPaid(order, command.getProviderTxnNo(), LocalDateTime.now());
    } else if ("FAIL".equals(normalizedStatus)) {
      paymentOrderStateSupport.markFailed(order);
    }
    order.setNextPollAt(null);
    order.setLastPolledAt(LocalDateTime.now());
    order.setLastPollError(null);
    paymentOrderMapper.updateById(order);
    paymentOrderStateSupport.handlePersistedState(order, previousStatus);
    return true;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Long createRefund(PaymentRefundCommandDTO command) {
    PaymentRefundEntity existing =
        paymentRefundMapper.selectOne(
            new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getIdempotencyKey, command.getIdempotencyKey())
                .eq(PaymentRefundEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (existing != null) {
      return existing.getId();
    }

    PaymentOrderEntity paymentOrder =
        paymentOrderMapper.selectOne(
            new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getPaymentNo, command.getPaymentNo())
                .eq(PaymentOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
    if (paymentOrder == null) {
      throw new BusinessException("payment order not found");
    }

    PaymentRefundEntity entity = new PaymentRefundEntity();
    entity.setRefundNo(command.getRefundNo());
    entity.setPaymentNo(command.getPaymentNo());
    entity.setAfterSaleNo(command.getAfterSaleNo());
    entity.setRefundAmount(command.getRefundAmount());
    entity.setReason(command.getReason());
    entity.setStatus("REFUNDING");
    entity.setIdempotencyKey(command.getIdempotencyKey());
    entity.setRetryCount(0);
    entity.setNextRetryAt(LocalDateTime.now());
    paymentRefundMapper.insert(entity);

    paymentCompensationService.submitRefund(paymentOrder, entity);
    return entity.getId();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public Boolean cancelRefund(String refundNo, String reason) {
    if (refundNo == null || refundNo.isBlank()) {
      throw new BusinessException("refund no is required");
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
    PaymentOrderVO vo = new PaymentOrderVO();
    vo.setId(entity.getId());
    vo.setPaymentNo(entity.getPaymentNo());
    vo.setMainOrderNo(entity.getMainOrderNo());
    vo.setSubOrderNo(entity.getSubOrderNo());
    vo.setUserId(entity.getUserId());
    vo.setAmount(entity.getAmount());
    vo.setChannel(entity.getChannel());
    vo.setStatus(entity.getStatus());
    vo.setProviderTxnNo(entity.getProviderTxnNo());
    vo.setIdempotencyKey(entity.getIdempotencyKey());
    vo.setPaidAt(entity.getPaidAt());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  private PaymentRefundVO toRefundVO(PaymentRefundEntity entity) {
    PaymentRefundVO vo = new PaymentRefundVO();
    vo.setId(entity.getId());
    vo.setRefundNo(entity.getRefundNo());
    vo.setPaymentNo(entity.getPaymentNo());
    vo.setAfterSaleNo(entity.getAfterSaleNo());
    vo.setRefundAmount(entity.getRefundAmount());
    vo.setStatus(entity.getStatus());
    vo.setReason(entity.getReason());
    vo.setIdempotencyKey(entity.getIdempotencyKey());
    vo.setRefundedAt(entity.getRefundedAt());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  private String buildOrderKey(PaymentOrderCommandDTO command) {
    return command.getMainOrderNo() + ":" + command.getSubOrderNo();
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

  private void validateCallbackAgainstOrder(
      PaymentOrderEntity order, PaymentCallbackCommandDTO command) {
    if (command.getAmount() != null
        && order.getAmount() != null
        && order.getAmount().compareTo(command.getAmount()) != 0) {
      throw new BusinessException("payment callback amount does not match order amount");
    }
    if (StringUtils.hasText(order.getProviderTxnNo())
        && StringUtils.hasText(command.getProviderTxnNo())
        && !order.getProviderTxnNo().equals(command.getProviderTxnNo())) {
      throw new BusinessException("payment callback provider transaction does not match order");
    }
  }

  private String normalizeCallbackStatus(String callbackStatus) {
    if (!StringUtils.hasText(callbackStatus)) {
      throw new BusinessException("payment callback status is required");
    }
    String normalized = callbackStatus.trim().toUpperCase();
    if ("SUCCESS".equals(normalized) || "FAIL".equals(normalized)) {
      return normalized;
    }
    throw new BusinessException("unsupported payment callback status: " + callbackStatus);
  }
}
