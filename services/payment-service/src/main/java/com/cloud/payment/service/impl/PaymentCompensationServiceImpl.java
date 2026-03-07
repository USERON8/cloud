package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.payment.config.PaymentCompensationProperties;
import com.cloud.payment.mapper.PaymentOrderMapper;
import com.cloud.payment.mapper.PaymentRefundMapper;
import com.cloud.payment.module.entity.PaymentOrderEntity;
import com.cloud.payment.module.entity.PaymentRefundEntity;
import com.cloud.payment.service.PaymentCompensationService;
import com.cloud.payment.service.provider.PaymentProviderGateway;
import com.cloud.payment.service.provider.model.PaymentOrderQueryResult;
import com.cloud.payment.service.provider.model.PaymentRefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompensationServiceImpl implements PaymentCompensationService {

    private static final String ORDER_STATUS_CREATED = "CREATED";
    private static final String ORDER_STATUS_PAID = "PAID";
    private static final String ORDER_STATUS_FAILED = "FAILED";
    private static final String REFUND_STATUS_REFUNDING = "REFUNDING";
    private static final String REFUND_STATUS_REFUNDED = "REFUNDED";
    private static final String REFUND_STATUS_REFUND_FAILED = "REFUND_FAILED";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentRefundMapper paymentRefundMapper;
    private final PaymentCompensationProperties properties;
    private final List<PaymentProviderGateway> providerGateways;

    @Override
    public void initializePaymentOrderCompensation(PaymentOrderEntity order) {
        order.setPollCount(0);
        order.setLastPolledAt(null);
        order.setLastPollError(null);
        order.setNextPollAt(LocalDateTime.now().plusSeconds(properties.getOrderQuery().getInitialDelaySeconds()));
    }

    @Override
    public void submitRefund(PaymentOrderEntity order, PaymentRefundEntity refund) {
        applyRefundAttempt(order, refund, 1, true);
    }

    @Override
    public int reconcilePendingOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentOrderEntity> orders = paymentOrderMapper.selectList(new LambdaQueryWrapper<PaymentOrderEntity>()
                .eq(PaymentOrderEntity::getStatus, ORDER_STATUS_CREATED)
                .eq(PaymentOrderEntity::getDeleted, 0)
                .isNotNull(PaymentOrderEntity::getNextPollAt)
                .le(PaymentOrderEntity::getNextPollAt, now)
                .orderByAsc(PaymentOrderEntity::getNextPollAt)
                .last("LIMIT " + properties.getOrderQuery().getBatchSize()));

        int handledCount = 0;
        for (PaymentOrderEntity order : orders) {
            handledCount++;
            applyOrderQueryResult(order, queryOrder(order), now);
        }
        return handledCount;
    }

    @Override
    public int retryPendingRefunds() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentRefundEntity> refunds = paymentRefundMapper.selectList(new LambdaQueryWrapper<PaymentRefundEntity>()
                .eq(PaymentRefundEntity::getStatus, REFUND_STATUS_REFUNDING)
                .eq(PaymentRefundEntity::getDeleted, 0)
                .and(wrapper -> wrapper.le(PaymentRefundEntity::getNextRetryAt, now)
                        .or()
                        .isNull(PaymentRefundEntity::getNextRetryAt))
                .orderByAsc(PaymentRefundEntity::getNextRetryAt)
                .last("LIMIT " + properties.getRefundRetry().getBatchSize()));

        int handledCount = 0;
        for (PaymentRefundEntity refund : refunds) {
            PaymentOrderEntity order = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrderEntity>()
                    .eq(PaymentOrderEntity::getPaymentNo, refund.getPaymentNo())
                    .eq(PaymentOrderEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (order == null) {
                log.warn("Skip refund retry because payment order is missing, refundNo={}, paymentNo={}", refund.getRefundNo(), refund.getPaymentNo());
                continue;
            }
            handledCount++;
            int nextAttempt = defaultNumber(refund.getRetryCount()) + 1;
            applyRefundAttempt(order, refund, nextAttempt, false);
        }
        return handledCount;
    }

    private void applyOrderQueryResult(PaymentOrderEntity order, PaymentOrderQueryResult result, LocalDateTime now) {
        int nextAttempt = defaultNumber(order.getPollCount()) + 1;
        order.setPollCount(nextAttempt);
        order.setLastPolledAt(now);
        if (StringUtils.hasText(result.providerTxnNo())) {
            order.setProviderTxnNo(result.providerTxnNo());
        }

        switch (result.status()) {
            case PAID -> {
                order.setStatus(ORDER_STATUS_PAID);
                order.setPaidAt(result.paidAt() != null ? result.paidAt() : now);
                order.setNextPollAt(null);
                order.setLastPollError(null);
            }
            case FAILED -> {
                order.setStatus(ORDER_STATUS_FAILED);
                order.setNextPollAt(null);
                order.setLastPollError(truncate(result.message()));
            }
            case PENDING, ERROR -> {
                boolean exhausted = nextAttempt >= properties.getOrderQuery().getMaxAttempts();
                order.setNextPollAt(exhausted ? null : now.plusSeconds(properties.getOrderQuery().getIntervalSeconds()));
                order.setLastPollError(exhausted
                        ? truncate("poll attempts exhausted: " + defaultString(result.message(), "PENDING"))
                        : truncate(result.message()));
            }
        }
        paymentOrderMapper.updateById(order);
    }

    private void applyRefundAttempt(PaymentOrderEntity order, PaymentRefundEntity refund, int attemptNumber, boolean firstAttempt) {
        LocalDateTime now = LocalDateTime.now();
        PaymentRefundResult result = executeRefund(order, refund);

        refund.setRetryCount(attemptNumber);
        refund.setLastRetryAt(now);

        switch (result.status()) {
            case REFUNDED -> {
                refund.setStatus(REFUND_STATUS_REFUNDED);
                refund.setRefundedAt(result.refundedAt() != null ? result.refundedAt() : now);
                refund.setNextRetryAt(null);
                refund.setLastError(null);
            }
            case PENDING, ERROR, FAILED -> {
                boolean exhausted = attemptNumber >= properties.getRefundRetry().getMaxAttempts();
                refund.setStatus(exhausted ? REFUND_STATUS_REFUND_FAILED : REFUND_STATUS_REFUNDING);
                refund.setNextRetryAt(exhausted ? null : now.plusSeconds(properties.getRefundRetry().getIntervalSeconds()));
                refund.setLastError(truncate(result.message()));
            }
        }
        paymentRefundMapper.updateById(refund);

        if (log.isInfoEnabled()) {
            log.info(
                    "Processed refund compensation, refundNo={}, paymentNo={}, status={}, attempt={}, firstAttempt={}",
                    refund.getRefundNo(), refund.getPaymentNo(), refund.getStatus(), attemptNumber, firstAttempt
            );
        }
    }

    private PaymentOrderQueryResult queryOrder(PaymentOrderEntity order) {
        PaymentProviderGateway gateway = resolveGateway(order.getChannel());
        if (gateway == null) {
            return PaymentOrderQueryResult.error("unsupported payment channel: " + order.getChannel());
        }
        return gateway.queryPaymentOrder(order);
    }

    private PaymentRefundResult executeRefund(PaymentOrderEntity order, PaymentRefundEntity refund) {
        PaymentProviderGateway gateway = resolveGateway(order.getChannel());
        if (gateway == null) {
            return PaymentRefundResult.failed("unsupported payment channel: " + order.getChannel());
        }
        return gateway.executeRefund(order, refund);
    }

    private PaymentProviderGateway resolveGateway(String channel) {
        for (PaymentProviderGateway gateway : providerGateways) {
            if (gateway.supports(channel)) {
                return gateway;
            }
        }
        return null;
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
