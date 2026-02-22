package com.cloud.payment.controller;

import com.cloud.api.payment.PaymentFeignClient;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;




@Slf4j
@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class PaymentFeignController implements PaymentFeignClient {

    private final PaymentService paymentService;
    private final PaymentConverter paymentConverter;

    @Override
    @GetMapping("/{paymentId}")
    public PaymentVO getPaymentById(@PathVariable("paymentId") Long paymentId) {
        Payment payment = paymentService.getById(paymentId);
        return payment == null ? null : paymentConverter.toVO(payment);
    }

    @Override
    @GetMapping("/order/{orderId}")
    public PaymentDTO getPaymentByOrderId(@PathVariable("orderId") Long orderId) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @Override
    @PostMapping("/create")
    public Long createPayment(@RequestBody PaymentDTO paymentDTO) {
        return paymentService.createPayment(paymentDTO);
    }

    @Override
    @PutMapping("/{paymentId}")
    public Boolean updatePayment(@PathVariable("paymentId") Long paymentId, @RequestBody PaymentDTO paymentDTO) {
        paymentDTO.setId(paymentId);
        return paymentService.updatePayment(paymentDTO);
    }

    @Override
    @DeleteMapping("/{paymentId}")
    public Boolean deletePayment(@PathVariable("paymentId") Long paymentId) {
        return paymentService.deletePayment(paymentId);
    }

    @Override
    @PostMapping("/{paymentId}/success")
    public Boolean paymentSuccess(@PathVariable("paymentId") Long paymentId) {
        return paymentService.processPaymentSuccess(paymentId);
    }

    @Override
    @PostMapping("/{paymentId}/fail")
    public Boolean paymentFail(@PathVariable("paymentId") Long paymentId,
                               @RequestParam(required = false) String failReason) {
        return paymentService.processPaymentFailed(paymentId, failReason);
    }

    @Override
    @PostMapping("/{paymentId}/refund")
    public Boolean refundPayment(@PathVariable("paymentId") Long paymentId,
                                 @RequestParam BigDecimal refundAmount,
                                 @RequestParam(required = false) String refundReason) {
        return paymentService.processRefund(paymentId, refundAmount, refundReason);
    }

    @Override
    @PostMapping("/risk-check")
    public Boolean riskCheck(@RequestParam Long userId,
                             @RequestParam BigDecimal amount,
                             @RequestParam String paymentMethod) {
        return paymentService.riskCheck(userId, amount, paymentMethod);
    }

    @Override
    @GetMapping("/batch")
    public List<PaymentVO> getPaymentsByIds(@RequestParam List<Long> paymentIds) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Payment> payments = paymentService.listByIds(paymentIds);
        return paymentConverter.toVOList(payments);
    }

    @Override
    @PostMapping("/batch")
    public Integer createPaymentsBatch(@RequestBody List<PaymentDTO> paymentList) {
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
    @PostMapping("/batch/success")
    public Integer paymentSuccessBatch(@RequestBody List<Long> paymentIds) {
        return processBatch(paymentIds, id -> paymentService.processPaymentSuccess(id));
    }

    @Override
    @PostMapping("/batch/fail")
    public Integer paymentFailBatch(@RequestBody List<Long> paymentIds,
                                    @RequestParam(required = false) String failReason) {
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
