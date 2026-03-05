package com.cloud.payment.rpc;

import com.cloud.api.payment.PaymentFeignClient;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@DubboService(interfaceClass = PaymentFeignClient.class, timeout = 5000, retries = 0)
@RequiredArgsConstructor
public class PaymentDubboService implements PaymentFeignClient {

    private final PaymentService paymentService;
    private final PaymentConverter paymentConverter;

    @Override
    public PaymentVO getPaymentById(Long paymentId) {
        Payment payment = paymentService.getById(paymentId);
        return payment == null ? null : paymentConverter.toVO(payment);
    }

    @Override
    public PaymentDTO getPaymentByOrderId(Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @Override
    public Long createPayment(PaymentDTO paymentDTO) {
        return paymentService.createPayment(paymentDTO);
    }

    @Override
    public Boolean updatePayment(Long paymentId, PaymentDTO paymentDTO) {
        paymentDTO.setId(paymentId);
        return paymentService.updatePayment(paymentDTO);
    }

    @Override
    public Boolean deletePayment(Long paymentId) {
        return paymentService.deletePayment(paymentId);
    }

    @Override
    public Boolean paymentSuccess(Long paymentId) {
        return paymentService.processPaymentSuccess(paymentId);
    }

    @Override
    public Boolean paymentFail(Long paymentId, String failReason) {
        return paymentService.processPaymentFailed(paymentId, failReason);
    }

    @Override
    public Boolean refundPayment(Long paymentId, BigDecimal refundAmount, String refundReason) {
        return paymentService.processRefund(paymentId, refundAmount, refundReason);
    }

    @Override
    public Boolean riskCheck(Long userId, BigDecimal amount, String paymentMethod) {
        return paymentService.riskCheck(userId, amount, paymentMethod);
    }

    @Override
    public List<PaymentVO> getPaymentsByIds(List<Long> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Payment> payments = paymentService.listByIds(paymentIds);
        return paymentConverter.toVOList(payments);
    }

    @Override
    public Integer createPaymentsBatch(List<PaymentDTO> paymentList) {
        if (paymentList == null || paymentList.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (PaymentDTO dto : paymentList) {
            try {
                Long id = paymentService.createPayment(dto);
                if (id != null) {
                    success++;
                }
            } catch (Exception e) {
                log.warn("Batch create payment failed: orderId={}", dto.getOrderId(), e);
            }
        }
        return success;
    }

    @Override
    public Integer paymentSuccessBatch(List<Long> paymentIds) {
        return processBatch(paymentIds, paymentService::processPaymentSuccess);
    }

    @Override
    public Integer paymentFailBatch(List<Long> paymentIds, String failReason) {
        return processBatch(paymentIds, id -> paymentService.processPaymentFailed(id, failReason));
    }

    private Integer processBatch(List<Long> ids, java.util.function.Function<Long, Boolean> processor) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int success = 0;
        for (Long id : ids) {
            try {
                if (Boolean.TRUE.equals(processor.apply(id))) {
                    success++;
                }
            } catch (Exception e) {
                log.warn("Batch process payment failed: paymentId={}", id, e);
            }
        }
        return success;
    }
}
