package com.cloud.payment.v2.service.impl;

import com.cloud.common.exception.BusinessException;
import com.cloud.payment.v2.entity.PaymentOrderV2;
import com.cloud.payment.v2.entity.PaymentRefundV2;
import com.cloud.payment.v2.mapper.PaymentOrderV2Mapper;
import com.cloud.payment.v2.mapper.PaymentRefundV2Mapper;
import com.cloud.payment.v2.service.PaymentV2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentV2ServiceImpl implements PaymentV2Service {

    private final PaymentOrderV2Mapper paymentOrderV2Mapper;
    private final PaymentRefundV2Mapper paymentRefundV2Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderV2 createPaymentOrder(PaymentOrderV2 paymentOrder) {
        if (paymentOrder.getPaymentNo() == null || paymentOrder.getPaymentNo().isBlank()) {
            paymentOrder.setPaymentNo("PAY-" + System.currentTimeMillis());
        }
        if (paymentOrder.getPaymentStatus() == null) {
            paymentOrder.setPaymentStatus("CREATED");
        }
        if (paymentOrder.getPaymentChannel() == null) {
            paymentOrder.setPaymentChannel("ALIPAY");
        }
        paymentOrderV2Mapper.insert(paymentOrder);
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
        if (paymentRefund.getRefundPaymentNo() == null || paymentRefund.getRefundPaymentNo().isBlank()) {
            paymentRefund.setRefundPaymentNo("RFD-" + System.currentTimeMillis());
        }
        if (paymentRefund.getRefundStatus() == null) {
            paymentRefund.setRefundStatus("CREATED");
        }
        if (paymentRefund.getRefundChannel() == null) {
            paymentRefund.setRefundChannel("ALIPAY");
        }
        paymentRefundV2Mapper.insert(paymentRefund);
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
}

