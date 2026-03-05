package com.cloud.api.payment;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentFeignClient {

    PaymentVO getPaymentById(Long paymentId);

    PaymentDTO getPaymentByOrderId(Long orderId);

    Long createPayment(PaymentDTO paymentDTO);

    Boolean updatePayment(Long paymentId, PaymentDTO paymentDTO);

    Boolean deletePayment(Long paymentId);

    Boolean paymentSuccess(Long paymentId);

    Boolean paymentFail(Long paymentId, String failReason);

    Boolean refundPayment(Long paymentId, BigDecimal refundAmount, String refundReason);

    Boolean riskCheck(Long userId, BigDecimal amount, String paymentMethod);

    List<PaymentVO> getPaymentsByIds(List<Long> paymentIds);

    Integer createPaymentsBatch(List<PaymentDTO> paymentList);

    Integer paymentSuccessBatch(List<Long> paymentIds);

    Integer paymentFailBatch(List<Long> paymentIds, String failReason);
}
