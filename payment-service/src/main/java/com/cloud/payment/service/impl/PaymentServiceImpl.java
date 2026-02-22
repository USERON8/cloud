package com.cloud.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.common.annotation.DistributedLock;
import com.cloud.common.domain.dto.payment.PaymentDTO;
import com.cloud.payment.converter.PaymentConverter;
import com.cloud.payment.mapper.PaymentMapper;
import com.cloud.payment.module.entity.Payment;
import com.cloud.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;






@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment>
        implements PaymentService {

    private final PaymentConverter paymentConverter;

    @Override
    public boolean isPaymentRecordExists(Long orderId) {
        
        try {
            long count = count(new LambdaQueryWrapper<Payment>()
                    .eq(Payment::getOrderId, orderId));
            boolean exists = count > 0;
            
            return exists;
        } catch (Exception e) {
            log.error("妫€鏌ユ敮浠樿褰曟槸鍚﹀瓨鍦ㄥけ璐?- 璁㈠崟ID: {}", orderId, e);
            return false;
        }
    }


    


    public void sendPaymentRefundLog(Long paymentId, Long refundId, Long orderId,
                                     Long userId, java.math.BigDecimal refundAmount,
                                     String refundReason, String operator) {
        try {

        } catch (Exception e) {
            log.warn("鍙戦€佹敮浠橀€€娆炬棩蹇楀け璐?- 鏀粯ID: {}, 閫€娆綢D: {}, 璁㈠崟ID: {}",
                    paymentId, refundId, orderId, e);
        }
    }

    


    private String getPaymentMethodName(Integer channel) {
        if (channel == null) return "UNKNOWN";
        return switch (channel) {
            case 1 -> "ALIPAY";
            case 2 -> "WECHAT";
            case 3 -> "BANK_CARD";
            case 4 -> "BALANCE";
            default -> "OTHER";
        };
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cloud.common.domain.dto.payment.PaymentDTO> getPaymentsPage(
            Integer page, Integer size, Long userId, Integer status, Integer channel) {
        

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Payment> paymentPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) wrapper.eq(Payment::getUserId, userId);
        if (status != null) wrapper.eq(Payment::getStatus, status);
        if (channel != null) wrapper.eq(Payment::getChannel, channel);
        wrapper.orderByDesc(Payment::getCreatedAt);
        page(paymentPage, wrapper);
        Page<PaymentDTO> result = new Page<>(paymentPage.getCurrent(), paymentPage.getSize(), paymentPage.getTotal());
        result.setRecords(paymentConverter.toDTOList(paymentPage.getRecords()));
        return result;
    }

    @Override
    public com.cloud.common.domain.dto.payment.PaymentDTO getPaymentById(Long id) {
        Payment payment = getById(id);
        return payment != null ? paymentConverter.toDTO(payment) : null;
    }

    @Override
    @DistributedLock(
            key = "'payment:create:order:' + #paymentDTO.orderId",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire payment create lock failed"
    )
    public Long createPayment(com.cloud.common.domain.dto.payment.PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setOrderId(paymentDTO.getOrderId());
        payment.setUserId(paymentDTO.getUserId());
        payment.setAmount(paymentDTO.getAmount());
        payment.setChannel(paymentDTO.getChannel());
        payment.setTransactionId(paymentDTO.getTransactionId());
        payment.setTraceId(paymentDTO.getTraceId());
        payment.setStatus(0);
        save(payment);
        return payment.getId();
    }

    @Override
    @DistributedLock(
            key = "'payment:update:' + #paymentDTO.id",
            waitTime = 5,
            leaseTime = 15,
            failMessage = "Acquire payment update lock failed"
    )
    public Boolean updatePayment(com.cloud.common.domain.dto.payment.PaymentDTO paymentDTO) {
        Payment payment = getById(paymentDTO.getId());
        if (payment == null) return false;
        if (paymentDTO.getOrderId() != null) payment.setOrderId(paymentDTO.getOrderId());
        if (paymentDTO.getUserId() != null) payment.setUserId(paymentDTO.getUserId());
        if (paymentDTO.getAmount() != null) payment.setAmount(paymentDTO.getAmount());
        if (paymentDTO.getStatus() != null) payment.setStatus(paymentDTO.getStatus());
        if (paymentDTO.getChannel() != null) payment.setChannel(paymentDTO.getChannel());
        if (paymentDTO.getTransactionId() != null) payment.setTransactionId(paymentDTO.getTransactionId());
        if (paymentDTO.getTraceId() != null) payment.setTraceId(paymentDTO.getTraceId());
        return updateById(payment);
    }

    @Override
    @DistributedLock(
            key = "'payment:delete:' + #id",
            waitTime = 3,
            leaseTime = 10,
            failMessage = "Acquire payment delete lock failed"
    )
    public Boolean deletePayment(Long id) {
        return removeById(id);
    }

    @Override
    @DistributedLock(
            key = "'payment:status:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire payment status lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean processPaymentSuccess(Long id) {
        
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(2);
        return updateById(payment);
    }

    @Override
    @DistributedLock(
            key = "'payment:status:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire payment status lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean processPaymentFailed(Long id, String failReason) {
        
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(3);
        return updateById(payment);
    }

    @Override
    @DistributedLock(
            key = "'payment:status:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire payment status lock failed"
    )
    @Transactional(rollbackFor = Exception.class)
    public Boolean processRefund(Long id, java.math.BigDecimal refundAmount, String refundReason) {
        
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(4);
        return updateById(payment);
    }

    @Override
    public com.cloud.common.domain.dto.payment.PaymentDTO getPaymentByOrderId(Long orderId) {
        Payment payment = getOne(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
        return payment != null ? paymentConverter.toDTO(payment) : null;
    }

    @Override
    public Boolean riskCheck(Long userId, java.math.BigDecimal amount, String paymentMethod) {
        
        return true;
    }

    @Override
    @DistributedLock(
            key = "'payment:status:' + #id",
            waitTime = 5,
            leaseTime = 20,
            failMessage = "Acquire payment status lock failed"
    )
    public Boolean updatePaymentStatus(Long id, Integer status, String remark) {
        Payment payment = getById(id);
        if (payment == null) return false;
        payment.setStatus(status);
        return updateById(payment);
    }

    @Override
    public Integer getPaymentStatus(Long id) {
        Payment payment = getById(id);
        return payment != null ? payment.getStatus() : null;
    }

    @Override
    public Boolean validatePaymentAmount(Long id, java.math.BigDecimal amount) {
        Payment payment = getById(id);
        return payment != null && payment.getAmount().compareTo(amount) == 0;
    }

    @Override
    public java.util.Map<String, Object> getUserPaymentStats(Long userId) {
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        long count = count(new LambdaQueryWrapper<Payment>().eq(Payment::getUserId, userId));
        stats.put("totalCount", count);
        stats.put("successCount", count(new LambdaQueryWrapper<Payment>().eq(Payment::getUserId, userId).eq(Payment::getStatus, 2)));
        return stats;
    }
}
