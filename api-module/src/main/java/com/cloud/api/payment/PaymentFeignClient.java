package com.cloud.api.payment;

import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.common.domain.vo.payment.PaymentVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;








@FeignClient(name = "payment-service", path = "/internal/payment", contextId = "paymentFeignClient")
public interface PaymentFeignClient {

    





    @GetMapping("/{paymentId}")
    PaymentVO getPaymentById(@PathVariable("paymentId") Long paymentId);

    





    @GetMapping("/order/{orderId}")
    PaymentDTO getPaymentByOrderId(@PathVariable("orderId") Long orderId);

    





    @PostMapping("/create")
    Long createPayment(@RequestBody PaymentDTO paymentDTO);

    






    @PutMapping("/{paymentId}")
    Boolean updatePayment(@PathVariable("paymentId") Long paymentId, @RequestBody PaymentDTO paymentDTO);

    





    @DeleteMapping("/{paymentId}")
    Boolean deletePayment(@PathVariable("paymentId") Long paymentId);

    





    @PostMapping("/{paymentId}/success")
    Boolean paymentSuccess(@PathVariable("paymentId") Long paymentId);

    






    @PostMapping("/{paymentId}/fail")
    Boolean paymentFail(@PathVariable("paymentId") Long paymentId,
                        @RequestParam(required = false) String failReason);

    







    @PostMapping("/{paymentId}/refund")
    Boolean refundPayment(@PathVariable("paymentId") Long paymentId,
                          @RequestParam BigDecimal refundAmount,
                          @RequestParam(required = false) String refundReason);

    







    @PostMapping("/risk-check")
    Boolean riskCheck(@RequestParam Long userId,
                      @RequestParam BigDecimal amount,
                      @RequestParam String paymentMethod);

    

    





    @GetMapping("/batch")
    List<PaymentVO> getPaymentsByIds(@RequestParam List<Long> paymentIds);

    





    @PostMapping("/batch")
    Integer createPaymentsBatch(@RequestBody List<PaymentDTO> paymentList);

    





    @PostMapping("/batch/success")
    Integer paymentSuccessBatch(@RequestBody List<Long> paymentIds);

    






    @PostMapping("/batch/fail")
    Integer paymentFailBatch(@RequestBody List<Long> paymentIds,
                             @RequestParam(required = false) String failReason);
}
