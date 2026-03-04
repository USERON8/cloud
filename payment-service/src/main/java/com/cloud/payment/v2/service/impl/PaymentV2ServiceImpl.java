package com.cloud.payment.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloud.common.exception.BusinessException;
import com.cloud.payment.v2.entity.PaymentOrderV2;
import com.cloud.payment.v2.entity.PaymentRefundV2;
import com.cloud.payment.v2.mapper.PaymentOrderV2Mapper;
import com.cloud.payment.v2.mapper.PaymentRefundV2Mapper;
import com.cloud.payment.v2.service.PaymentV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentV2ServiceImpl implements PaymentV2Service {

    private final PaymentOrderV2Mapper paymentOrderV2Mapper;
    private final PaymentRefundV2Mapper paymentRefundV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderV2 createPaymentOrder(PaymentOrderV2 paymentOrder) {
        String idempotencyKey = normalizeIdempotencyKey(paymentOrder.getIdempotencyKey());
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("idempotency key is required");
        }
        paymentOrder.setIdempotencyKey(idempotencyKey);

        PaymentOrderV2 existing = paymentOrderV2Mapper.selectOne(
                new LambdaQueryWrapper<PaymentOrderV2>()
                        .eq(PaymentOrderV2::getIdempotencyKey, idempotencyKey)
                        .eq(PaymentOrderV2::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing;
        }

        if (paymentOrder.getPaymentNo() == null || paymentOrder.getPaymentNo().isBlank()) {
            paymentOrder.setPaymentNo("PAY-" + System.currentTimeMillis());
        }
        if (paymentOrder.getPaymentStatus() == null) {
            paymentOrder.setPaymentStatus("CREATED");
        }
        if (paymentOrder.getPaymentChannel() == null) {
            paymentOrder.setPaymentChannel("ALIPAY");
        }
        try {
            paymentOrderV2Mapper.insert(paymentOrder);
        } catch (DuplicateKeyException duplicateKeyException) {
            PaymentOrderV2 duplicated = paymentOrderV2Mapper.selectOne(
                    new LambdaQueryWrapper<PaymentOrderV2>()
                            .eq(PaymentOrderV2::getIdempotencyKey, idempotencyKey)
                            .eq(PaymentOrderV2::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (duplicated != null) {
                return duplicated;
            }
            throw duplicateKeyException;
        }
        return paymentOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderV2 markPaid(Long paymentId, String transactionNo) {
        PaymentOrderV2 paymentOrder = paymentOrderV2Mapper.selectById(paymentId);
        if (paymentOrder == null || paymentOrder.getDeleted() == 1) {
            throw new BusinessException("payment order not found");
        }
        paymentOrder.setPaymentStatus("PAID");
        paymentOrder.setPaidAmount(paymentOrder.getTotalAmount());
        paymentOrder.setPaidAt(LocalDateTime.now());
        paymentOrder.setTransactionNo(transactionNo);
        paymentOrderV2Mapper.updateById(paymentOrder);
        return paymentOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundV2 createRefund(PaymentRefundV2 paymentRefund) {
        String normalizedRefundNo = normalizeRefundNo(paymentRefund.getRefundPaymentNo());
        if (!StringUtils.hasText(normalizedRefundNo)) {
            normalizedRefundNo = "RFD-" + System.currentTimeMillis();
        }
        paymentRefund.setRefundPaymentNo(normalizedRefundNo);

        PaymentRefundV2 existing = paymentRefundV2Mapper.selectOne(
                new LambdaQueryWrapper<PaymentRefundV2>()
                        .eq(PaymentRefundV2::getRefundPaymentNo, normalizedRefundNo)
                        .eq(PaymentRefundV2::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing;
        }
        if (paymentRefund.getRefundStatus() == null) {
            paymentRefund.setRefundStatus("CREATED");
        }
        if (paymentRefund.getRefundChannel() == null) {
            paymentRefund.setRefundChannel("ALIPAY");
        }
        try {
            paymentRefundV2Mapper.insert(paymentRefund);
        } catch (DuplicateKeyException duplicateKeyException) {
            PaymentRefundV2 duplicated = paymentRefundV2Mapper.selectOne(
                    new LambdaQueryWrapper<PaymentRefundV2>()
                            .eq(PaymentRefundV2::getRefundPaymentNo, normalizedRefundNo)
                            .eq(PaymentRefundV2::getDeleted, 0)
                            .last("LIMIT 1")
            );
            if (duplicated != null) {
                return duplicated;
            }
            throw duplicateKeyException;
        }
        return paymentRefund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundV2 markRefunded(Long refundId, String refundTransactionNo) {
        PaymentRefundV2 paymentRefund = paymentRefundV2Mapper.selectById(refundId);
        if (paymentRefund == null || paymentRefund.getDeleted() == 1) {
            throw new BusinessException("payment refund not found");
        }
        paymentRefund.setRefundStatus("REFUNDED");
        paymentRefund.setRefundTransactionNo(refundTransactionNo);
        paymentRefund.setRefundedAt(LocalDateTime.now());
        paymentRefundV2Mapper.updateById(paymentRefund);
        return paymentRefund;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        String trimmed = idempotencyKey.trim();
        return trimmed.length() <= 128 ? trimmed : trimmed.substring(0, 128);
    }

    private String normalizeRefundNo(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        String trimmed = candidate.trim();
        if (trimmed.length() <= 64) {
            return trimmed;
        }
        String digest = UUID.nameUUIDFromBytes(trimmed.getBytes(StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
        return "RFD-" + digest;
    }
}
