package com.cloud.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.payment.module.entity.Payment;

import java.math.BigDecimal;
import java.util.Map;






public interface PaymentService extends IService<Payment> {

    





    boolean isPaymentRecordExists(Long orderId);

    









    Page<PaymentDTO> getPaymentsPage(Integer page, Integer size, Long userId, Integer status, Integer channel);

    





    PaymentDTO getPaymentById(Long id);

    





    Long createPayment(PaymentDTO paymentDTO);

    





    Boolean updatePayment(PaymentDTO paymentDTO);

    





    Boolean deletePayment(Long id);

    





    Boolean processPaymentSuccess(Long id);

    






    Boolean processPaymentFailed(Long id, String failReason);

    







    Boolean processRefund(Long id, BigDecimal refundAmount, String refundReason);

    





    PaymentDTO getPaymentByOrderId(Long orderId);

    







    Boolean riskCheck(Long userId, BigDecimal amount, String paymentMethod);

    







    Boolean updatePaymentStatus(Long id, Integer status, String remark);

    





    Integer getPaymentStatus(Long id);

    






    Boolean validatePaymentAmount(Long id, BigDecimal amount);

    





    Map<String, Object> getUserPaymentStats(Long userId);
}
